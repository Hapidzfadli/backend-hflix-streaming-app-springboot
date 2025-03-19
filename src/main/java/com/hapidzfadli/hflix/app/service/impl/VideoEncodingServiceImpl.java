package com.hapidzfadli.hflix.app.service.impl;

import com.hapidzfadli.hflix.app.service.MinioService;
import com.hapidzfadli.hflix.app.service.VideoEncodingService;
import com.hapidzfadli.hflix.config.MinioConfig;
import com.hapidzfadli.hflix.domain.entity.Video;
import com.hapidzfadli.hflix.domain.entity.VideoFormat;
import com.hapidzfadli.hflix.domain.repository.VideoFormatRepository;
import com.hapidzfadli.hflix.domain.repository.VideoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEncodingServiceImpl implements VideoEncodingService {

    private final VideoRepository videoRepository;
    private final VideoFormatRepository videoFormatRepository;
    private final MinioService minioService;
    private final MinioConfig minioConfig;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final VideoEncodingService self;

    @Value("${ffmpeg.binary.path}")
    private String ffmpegPath;

    @Value("${ffmpeg.probe.path}")
    private String ffprobePath;

    @Value("${video.encoding.resolutions}")
    private String resolutionsConfig;

    @Value("${video.encoding.codecs}")
    private String codecsConfig;

    @Value("${kafka.topic.encoding-queue}")
    private String encodingQueueTopic;

    @Value("${kafka.topic.encoding-status}")
    private String encodingStatusTopic;

    @Override
    @Transactional
    public void startEncodingJob(Video video){
        log.info("Starting encoding job for video ID: {}", video.getId());

        String[] resolutions = resolutionsConfig.split(",");
        String[] codecs = codecsConfig.split(",");

        for (String resolution : resolutions) {
            for (String codec : codecs) {
                VideoFormat format = new VideoFormat();
                format.setVideo(video);
                format.setResolution(resolution.trim());
                format.setCodec(codec.trim());
                format.setBitrate(getBitrateForResolution(resolution.trim()));
                format.setStatus(VideoFormat.Status.PROCESSING);
                format.setS3Path("");
                format.setFileSize("0");

                format = videoFormatRepository.save(format);

                Map<String, Object> encodingJob = new HashMap<>();
                encodingJob.put("videoId", video.getId());
                encodingJob.put("resolution", resolution.trim());
                encodingJob.put("codec", codec.trim());
                encodingJob.put("formatId", format.getId());

                kafkaTemplate.send(encodingQueueTopic, video.getId().toString(), encodingJob);

                log.info("Encoding job submitted for video ID: {}, resolution: {}, codec: {}", video.getId(), resolution, codec);
            }
        }
    }

    @Override
    @Async
    public void processEncodingJob(Long videoId, String resolution, String codec)  {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new EntityNotFoundException("Video not found with ID: " + videoId));

            VideoFormat format = videoFormatRepository.findByVideoAndResolutionAndCodec(
                    video, resolution, codec)
                    .orElseThrow(() -> new EntityNotFoundException("Video format not found for video ID: " + videoId));

            File tempOriginal = File.createTempFile("original_", ".mp4");
            try (InputStream is = minioService.getObject(minioConfig.getBucketName(), video.getS3Path());
                 FileOutputStream fos = new FileOutputStream(tempOriginal)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            File tempEncoded = File.createTempFile("encoded_", getFileExtension(codec));

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(
                    ffmpegPath,
                    "-i", tempOriginal.getAbsolutePath(),
                    "-c:v", getFFmpegCodec(codec),
                    "-b:v", format.getBitrate() + "k",
                    "-preset", "medium",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-movflags", "+faststart",
                    getResolutionParams(resolution),
                    tempEncoded.getAbsolutePath()
            );

            pb.redirectErrorStream(true);

            Process process = pb.start();

            boolean completed = process.waitFor(6, TimeUnit.HOURS);

            if(!completed) {
                process.destroy();
                self.updateEncodingStatus(videoId, resolution, VideoFormat.Status.ERROR, 0);
                log.error("Encoding job for video ID: {}, resolution: {} timed out", videoId, resolution);
                return;
            }

            if(process.exitValue() != 0) {
                self.updateEncodingStatus(videoId, resolution, VideoFormat.Status.ERROR, 0);
                log.error("Error processing encoding job for video ID: {}, resolution: {}", videoId, resolution);
                return;
            }

            String s3Key = minioConfig.getEncodedPath() + "/" + video.getUser().getId() + "/" + UUID.randomUUID() + '/' +
                    getBaseFilename(video.getOriginalFilename()) + "_" + resolution + getFileExtension(codec);

            minioService.uploadFile(minioConfig.getBucketName(), s3Key, tempEncoded);

            format.setS3Path(s3Key);
            format.setFileSize(String.valueOf(tempEncoded.length()));
            format.setStatus(VideoFormat.Status.READY);
            videoFormatRepository.save(format);

            if(video.getStatus() == Video.Status.PROCESSING) {
                long completedFormats = videoFormatRepository.countByVideoAndStatus(video, VideoFormat.Status.READY);
                long totalFormats = videoFormatRepository.countByVideo(video);

                if(completedFormats == totalFormats) {
                    video.setStatus(Video.Status.READY);
                    videoRepository.save(video);
                }
            }

            tempOriginal.delete();
            tempEncoded.delete();

            log.info("Completed encoding job for video ID: {}, resolution: {}, codec: {}",
                    videoId, resolution, codec);

        } catch (Exception e) {
            log.error("Error processing encoding job for video ID: {}, resolution: {}",
                    videoId, resolution, e);

            self.updateEncodingStatus(videoId, resolution, VideoFormat.Status.ERROR, 0);
        }
    }

    @Override
    @Transactional
    public void updateEncodingStatus(Long videoId, String resolution, VideoFormat.Status status, int progress) {
        try {
            Video video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new EntityNotFoundException("Video not found with ID: " + videoId));

            VideoFormat format = videoFormatRepository.findByVideoAndResolution(video, resolution)
                    .orElseThrow(() -> new EntityNotFoundException("Video format not found for video ID: " + videoId));

            format.setStatus(status);
            videoFormatRepository.save(format);

            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("videoId", videoId);
            statusUpdate.put("resolution", resolution);
            statusUpdate.put("status", status);
            statusUpdate.put("progress", progress);

            kafkaTemplate.send(encodingStatusTopic, videoId.toString(), statusUpdate);
        } catch (Exception e) {
            log.error("Error updating encoding status for video ID: {}, resolution: {}", videoId, resolution, e);
        }
    }

    @Override
    @Async
    public void generateThumbnail(Video video) {
        try {
            log.info("Generating thumbnail for video ID: {}", video.getId());


            File tempOriginal = File.createTempFile("thumb_original_", ".mp4");
            try (InputStream is = minioService.getObject(minioConfig.getBucketName(), video.getS3Path());
                 FileOutputStream fos = new FileOutputStream(tempOriginal)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }

            File tempThumb = File.createTempFile("thumbnail_", ".jpg");

            ProcessBuilder probePb = new ProcessBuilder();
            probePb.command(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    tempOriginal.getAbsolutePath()
            );

            Process probeProcess = probePb.start();
            String durationStr = new String(probeProcess.getInputStream().readAllBytes()).trim();
            double duration = Double.parseDouble(durationStr);

            double thumbTime = duration * 0.1;

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(
                    ffmpegPath,
                    "-i", tempOriginal.getAbsolutePath(),
                    "-ss", String.format("%.2f", thumbTime),
                    "-vframes", "1",
                    "-vf", "scale=640:-1",
                    "-q:v", "2",
                    tempThumb.getAbsolutePath()
            );

            Process process = pb.start();
            boolean completed = process.waitFor(5, TimeUnit.MINUTES);

            if (!completed || process.exitValue() != 0) {
                log.error("Thumbnail generation failed for video ID: {}", video.getId());
                return;
            }

            String s3Key = minioConfig.getThumbnailPath() + "/" + video.getUser().getId() + "/" +
                    UUID.randomUUID() + ".jpg";

            minioService.uploadFile(minioConfig.getBucketName(), s3Key, tempThumb);

            video.setThumbnailPath(s3Key);
            video.setDuration((int) Math.ceil(duration));
            videoRepository.save(video);

            tempOriginal.delete();
            tempThumb.delete();

            log.info("Thumbnail generated for video ID: {}", video.getId());

        } catch (Exception e) {
            log.error("Error generating thumbnail for video ID: {}", video.getId(), e);
        }
    }


    private int getBitrateForResolution(String resolution) {
        switch (resolution) {
            case "240p":
                return 400;
            case "360p":
                return 700;
            case "480p":
                return 1000;
            case "720p":
                return 2500;
            case "1080p":
                return 5000;
            case "4K":
                return 15000;
            default:
                return 1000;
        }
    }

    private String getFFmpegCodec(String codec) {
        switch (codec) {
            case "H.264":
                return "libx264";
            case "H.265":
                return "libx265";
            case "VP9":
                return "libvpx-vp9";
            default:
                return "libx264";
        }
    }

    private String getResolutionParams(String resolution) {
        switch (resolution) {
            case "240p":
                return "-vf scale=-2:240";
            case "360p":
                return "-vf scale=-2:360";
            case "480p":
                return "-vf scale=-2:480";
            case "720p":
                return "-vf scale=-2:720";
            case "1080p":
                return "-vf scale=-2:1080";
            case "4K":
                return "-vf scale=-2:2160";
            default:
                return "-vf scale=-2:720";
        }
    }

    private String getFileExtension(String codec) {
        switch (codec) {
            case "H.264":
            case "H.265":
                return ".mp4";
            case "VP9":
                return ".webm";
            default:
                return ".mp4";
        }
    }

    private String getBaseFilename(String filename) {
        int lastDotPos = filename.lastIndexOf(".");
        if (lastDotPos > 0) {
            return filename.substring(0, lastDotPos);
        }
        return filename;
    }
}
