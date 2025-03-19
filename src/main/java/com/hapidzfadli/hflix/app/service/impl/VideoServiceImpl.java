package com.hapidzfadli.hflix.app.service.impl;

import com.hapidzfadli.hflix.api.dto.VideoDTO;
import com.hapidzfadli.hflix.api.dto.VideoFormatDTO;
import com.hapidzfadli.hflix.api.dto.VideoStreamInfoDTO;
import com.hapidzfadli.hflix.api.dto.VideoViewDTO;
import com.hapidzfadli.hflix.app.service.MinioService;
import com.hapidzfadli.hflix.app.service.VideoService;
import com.hapidzfadli.hflix.config.MinioConfig;
import com.hapidzfadli.hflix.domain.entity.User;
import com.hapidzfadli.hflix.domain.entity.Video;
import com.hapidzfadli.hflix.domain.entity.VideoFormat;
import com.hapidzfadli.hflix.domain.entity.VideoView;
import com.hapidzfadli.hflix.domain.repository.UserRepository;
import com.hapidzfadli.hflix.domain.repository.VideoFormatRepository;
import com.hapidzfadli.hflix.domain.repository.VideoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;
    private final VideoFormatRepository videoFormatRepository;
    private final UserRepository userRepository;
    private final MinioService minioService;
    private final MinioConfig minioConfig;
    private final KafkaTemplate<String, VideoViewDTO> kafkaTemplate;

    private static final int DEFAULT_CHUNK_SIZE = 1024 * 1024; // 1MB
    private static final String KAFKA_TOPIC_VIDEO_VIEWS = "video-views";

    @Override
    public VideoDTO getVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Video not found"));

        return VideoDTO.fromVideo(video);
    }

    @Override
    public Page<VideoDTO> findPublicVideos(String keyword, Pageable pageable) {
        Page<Video> videos = keyword != null && !keyword.isEmpty()
                ? videoRepository.searchPublicVideos(keyword, pageable)
                : videoRepository.findByVisibilityAndStatus(
                Video.Visibility.PUBLIC, Video.Status.READY, pageable);

        return videos.map(VideoDTO::fromVideo);
    }

    @Override
    public Page<VideoDTO> findUserVideos(String username, String keyword, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Page<Video> videos = keyword != null && !keyword.isEmpty()
                ? videoRepository.searchUserVideos(user, keyword, pageable)
                : videoRepository.findByUser(user, pageable);

        return videos.map(VideoDTO::fromVideo);
    }

    @Override
    public VideoStreamInfoDTO getVideoStreamInfo(Long videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException("Video not found"));

        if (video.getStatus() != Video.Status.READY) {
            throw new IllegalStateException("Video is not ready for streaming");
        }

        List<VideoFormat> formats = videoFormatRepository.findByVideoAndStatus(
                video, VideoFormat.Status.READY);

        List<VideoFormatDTO> formatDTOs = formats.stream()
                .map(format -> VideoFormatDTO.builder()
                        .id(format.getId())
                        .resolution(format.getResolution())
                        .codec(format.getCodec())
                        .bitrate(format.getBitrate())
                        .build())
                .collect(Collectors.toList());

        return VideoStreamInfoDTO.builder()
                .videoId(video.getId())
                .title(video.getTitle())
                .duration(video.getDuration())
                .thumbnailUrl("/api/videos/" + video.getId() + "/thumbnail")
                .formats(formatDTOs)
                .build();
    }

    @Override
    public ResponseEntity<byte[]> streamVideo(Long videoId, String resolution, String rangeHeader) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException("Video not found"));

        if (video.getStatus() != Video.Status.READY) {
            throw new IllegalStateException("Video is not ready for streaming");
        }

        VideoFormat format;
        if (resolution != null && !resolution.isEmpty()) {
            format = videoFormatRepository.findByVideoAndResolution(video, resolution)
                    .orElseGet(() -> {
                        List<VideoFormat> formats = videoFormatRepository.findByVideoAndStatus(
                                video, VideoFormat.Status.READY);
                        return getBestMatchFormat(formats, resolution);
                    });
        } else {
            List<VideoFormat> formats = videoFormatRepository.findByVideoAndStatus(
                    video, VideoFormat.Status.READY);
            format = getHighestResolutionFormat(formats);
        }

        if (format == null) {
            throw new IllegalStateException("No streaming formats available for this video");
        }

        try {
            long start = 0;
            long end = Long.parseLong(format.getFileSize()) - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                String[] ranges = rangeHeader.substring(6).split("-");
                start = Long.parseLong(ranges[0]);

                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                }
            }

            long contentLength = end - start + 1;

            InputStream inputStream = minioService.getObjectRange(
                    minioConfig.getBucketName(),
                    format.getS3Path(),
                    start,
                    contentLength > DEFAULT_CHUNK_SIZE ? start + DEFAULT_CHUNK_SIZE - 1 : end
            );

            byte[] data = IOUtils.toByteArray(inputStream);
            inputStream.close();

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", getContentType(format.getCodec()));
            headers.add("Accept-Ranges", "bytes");
            headers.add("Content-Range", String.format("bytes %d-%d/%d", start, start + data.length - 1, Long.parseLong(format.getFileSize())));
            headers.setContentLength(data.length);

            return new ResponseEntity<>(data, headers, HttpStatus.PARTIAL_CONTENT);

        } catch (Exception e) {
            log.error("Error streaming video: {}", e.getMessage(), e);
            throw new RuntimeException("Error streaming video", e);
        }
    }

    @Override
    public void recordVideoView(Long videoId, String username, String ipAddress, String userAgent, String resolution) {
        VideoViewDTO videoView = new VideoViewDTO();
        videoView.setVideoId(videoId);

        if (StringUtils.hasText(username)) {
            Optional<User> user = userRepository.findByUsername(username);
            user.ifPresent(u -> videoView.setUserId(u.getId()));
        }

        videoView.setIpAddress(ipAddress);
        videoView.setUserAgent(userAgent);
        videoView.setResolution(resolution);
        videoView.setViewedAt(java.time.LocalDateTime.now());

        kafkaTemplate.send(KAFKA_TOPIC_VIDEO_VIEWS, videoView);
    }

    private VideoFormat getBestMatchFormat(List<VideoFormat> formats, String requestedResolution) {
        int requestedHeight = parseResolutionHeight(requestedResolution);

        return formats.stream()
                .sorted((f1, f2) ->
                        Integer.compare(parseResolutionHeight(f2.getResolution()),
                                parseResolutionHeight(f1.getResolution())))
                .filter(f -> parseResolutionHeight(f.getResolution()) <= requestedHeight)
                .findFirst()
                .orElse(getLowestResolutionFormat(formats));
    }

    private VideoFormat getHighestResolutionFormat(List<VideoFormat> formats) {
        return formats.stream()
                .sorted((f1, f2) ->
                        Integer.compare(parseResolutionHeight(f2.getResolution()),
                                parseResolutionHeight(f1.getResolution())))
                .findFirst()
                .orElse(null);
    }

    private VideoFormat getLowestResolutionFormat(List<VideoFormat> formats) {
        return formats.stream()
                .sorted((f1, f2) ->
                        Integer.compare(parseResolutionHeight(f1.getResolution()),
                                parseResolutionHeight(f2.getResolution())))
                .findFirst()
                .orElse(null);
    }

    private int parseResolutionHeight(String resolution) {
        if (resolution.equals("4K")) {
            return 2160;
        }

        return Integer.parseInt(resolution.replaceAll("[^0-9]", ""));
    }

    private String getContentType(String codec) {
        switch (codec.toUpperCase()) {
            case "H.264":
            case "H.265":
                return "video/mp4";
            case "VP9":
                return "video/webm";
            default:
                return "video/mp4";
        }
    }
}
