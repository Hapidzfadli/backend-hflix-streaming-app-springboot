package com.hapidzfadli.hflix.app.service.impl;

import com.hapidzfadli.hflix.api.dto.ChunkUploadDTO;
import com.hapidzfadli.hflix.api.dto.VideoDTO;
import com.hapidzfadli.hflix.api.dto.VideoUploadResponseDTO;
import com.hapidzfadli.hflix.app.service.MinioService;
import com.hapidzfadli.hflix.app.service.UserService;
import com.hapidzfadli.hflix.app.service.VideoEncodingService;
import com.hapidzfadli.hflix.app.service.VideoUploadService;
import com.hapidzfadli.hflix.config.MinioConfig;
import com.hapidzfadli.hflix.domain.entity.User;
import com.hapidzfadli.hflix.domain.entity.Video;
import com.hapidzfadli.hflix.domain.repository.VideoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoUploadServiceImpl implements VideoUploadService {

    private final VideoRepository videoRepository;
    private final UserService userService;
    private final MinioService minioService;
    private final MinioConfig minioConfig;
    private final VideoEncodingService videoEncodingService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final Map<Long, Path> tempUploadDirs = new HashMap<>();


    @Override
    @Transactional
    public VideoUploadResponseDTO initializeUpload(String username, String filename, long fileSize, String title, String description) {
        if(fileSize > 3L  * 1024 * 1024 * 1024){
            throw new IllegalArgumentException("File size exceeds the limit");
        }

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        Video video = new Video();
        video.setUser(user);
        video.setOriginalFilename(filename);
        video.setTitle(title != null ? title : filename);
        video.setDescription(description);
        video.setFileSize(fileSize);
        video.setStatus(Video.Status.UPLOADING);

        video = videoRepository.save(video);

        try {
            Path tempDir = Files.createTempDirectory("video_upload_" + video.getId() + "_");
            tempUploadDirs.put(video.getId(), tempDir);
            log.info("Created temp directory for video {}: {}", video.getId(), tempDir);

            return VideoUploadResponseDTO.builder()
                    .videoId(video.getId())
                    .uploadUrl("/api/videos/upload/chunk?videoId=" + video.getId())
                    .maxChunkSize(5 * 1024 * 1024) // 5MB chunk size
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary directory for upload", e);
        }
    }

    @Override
    @Transactional
    public ChunkUploadDTO uploadChunk(String username, Long videoId, int chunkNumber, int totalChunks, MultipartFile chunk) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException("Video not found"));

        if (!video.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("You don't have permission to upload to this video");
        }

        if (video.getStatus() != Video.Status.UPLOADING) {
            throw new IllegalStateException("Video is not in UPLOADING state");
        }

        Path tempDir = tempUploadDirs.get(videoId);
        if (tempDir == null) {
            throw new IllegalStateException("Upload session not found");
        }

        try {
            Path chunkFile = tempDir.resolve(String.format("%05d", chunkNumber));
            try (FileOutputStream fos = new FileOutputStream(chunkFile.toFile())) {
                fos.write(chunk.getBytes());
            }

            log.info("Saved chunk {} of {} for video {}", chunkNumber, totalChunks, videoId);

            return ChunkUploadDTO.builder()
                    .videoId(videoId)
                    .chunkNumber(chunkNumber)
                    .received(true)
                    .nextExpectedChunk(chunkNumber + 1)
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to save chunk", e);
        }
    }

    @Override
    @Transactional
    public VideoDTO completeUpload(String username, Long videoId){
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException("Video not found"));

        if (!video.getUser().getUsername().equals(username)) {
            throw new IllegalArgumentException("You don't have permission to complete this upload");
        }

        if (video.getStatus() != Video.Status.UPLOADING) {
            throw new IllegalStateException("Video is not in UPLOADING state");
        }

        Path tempDir = tempUploadDirs.get(videoId);
        if (tempDir == null) {
            throw new IllegalStateException("Upload session not found");
        }


        try {
            Path combinedFile = tempDir.resolve("combined.mp4");
            combineChunks(tempDir, combinedFile);

            String s3Key = minioConfig.getOriginalPath() + "/" + video.getUser().getId() + "/" +
                    UUID.randomUUID() + "/" + video.getOriginalFilename();

            minioService.uploadFile(minioConfig.getBucketName(), s3Key, combinedFile.toFile());

            video.setS3Path(s3Key);
            video.setStatus(Video.Status.PROCESSING);
            video = videoRepository.save(video);

            videoEncodingService.startEncodingJob(video);

            Files.walk(tempDir)
                    .map(Path::toFile)
                    .forEach(File::delete);
            tempDir.toFile().delete();
            tempUploadDirs.remove(videoId);

            return VideoDTO.fromVideo(video);

        } catch (Exception e) {
            throw new RuntimeException("Failed to complete upload", e);
        }
    }

    private void combineChunks(Path tempDir, Path outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile.toFile())) {
            Files.list(tempDir)
                    .filter(path -> !path.getFileName().toString().equals("combined.mp4"))
                    .sorted()
                    .forEach(chunkPath -> {
                        try {
                            Files.copy(chunkPath, fos);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to combine chunks", e);
                        }
                    });
        }
    }
}
