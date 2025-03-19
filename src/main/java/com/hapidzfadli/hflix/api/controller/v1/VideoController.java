package com.hapidzfadli.hflix.api.controller.v1;

import com.hapidzfadli.hflix.api.dto.*;
import com.hapidzfadli.hflix.app.service.MinioService;
import com.hapidzfadli.hflix.app.service.VideoService;
import com.hapidzfadli.hflix.app.service.VideoUploadService;
import com.hapidzfadli.hflix.config.MinioConfig;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class VideoController {

    private final VideoService videoService;
    private final VideoUploadService videoUploadService;
    private final MinioService minioService;
    private final MinioConfig minioConfig;

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseDTO<VideoDTO>> getVideo(@PathVariable("id") Long videoId) {
        log.info("Getting video with ID: {}", videoId);

        VideoDTO videoDTO = videoService.getVideo(videoId);
        return ResponseEntity.ok(WebResponseDTO.success(videoDTO, "Video retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<WebResponseDTO<Page<VideoDTO>>> getPublicVideos(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction) {

        log.info("Getting public videos with keyword: {}, page: {}, size: {}", keyword, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(direction), sort);
        Page<VideoDTO> videosPage = videoService.findPublicVideos(keyword, pageable);

        return ResponseEntity.ok(WebResponseDTO.success(videosPage, "Videos retrieved successfully"));
    }

    @GetMapping("/my-videos")
    public ResponseEntity<WebResponseDTO<Page<VideoDTO>>> getUserVideos(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", defaultValue = "createdAt") String sort,
            @RequestParam(value = "direction", defaultValue = "DESC") String direction,
            Authentication authentication) {

        String username = authentication.getName();
        log.info("Getting videos for user: {}, keyword: {}, page: {}, size: {}", username, keyword, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.fromString(direction), sort);
        Page<VideoDTO> videosPage = videoService.findUserVideos(username, keyword, pageable);

        return ResponseEntity.ok(WebResponseDTO.success(videosPage, "Videos retrieved successfully"));
    }

    @GetMapping("/{id}/stream-info")
    public ResponseEntity<WebResponseDTO<VideoStreamInfoDTO>> getVideoStreamInfo(@PathVariable("id") Long videoId) {
        log.info("Getting stream info for video ID: {}", videoId);

        VideoStreamInfoDTO streamInfo = videoService.getVideoStreamInfo(videoId);
        return ResponseEntity.ok(WebResponseDTO.success(streamInfo, "Video stream info retrieved successfully"));
    }

    @GetMapping("/stream/{id}")
    public ResponseEntity<byte[]> streamVideo(
            @PathVariable("id") Long videoId,
            @RequestParam(value = "resolution", required = false) String resolution,
            @RequestHeader(value = "Range", required = false) String rangeHeader,
            HttpServletRequest request,
            Authentication authentication) {

        log.info("Streaming video ID: {}, resolution: {}, range: {}", videoId, resolution, rangeHeader);

        String username = authentication != null ? authentication.getName() : null;
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        // Asinkron rekam tontonan agar tidak menunda streaming
        CompletableFuture.runAsync(() -> {
            videoService.recordVideoView(videoId, username, ipAddress, userAgent, resolution);
        });

        return videoService.streamVideo(videoId, resolution, rangeHeader);
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> getThumbnail(@PathVariable("id") Long videoId) {
        try {
            log.info("Getting thumbnail for video ID: {}", videoId);

            VideoDTO video = videoService.getVideo(videoId);
            if (video.getThumbnailPath() == null) {
                return ResponseEntity.notFound().build();
            }

            InputStream inputStream = minioService.getObject(minioConfig.getBucketName(), video.getThumbnailPath());
            InputStreamResource resource = new InputStreamResource(inputStream);

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);

        } catch (Exception e) {
            log.error("Error getting thumbnail: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/upload/init")
    public ResponseEntity<WebResponseDTO<VideoUploadResponseDTO>> initializeUpload(
            @RequestParam("filename") String filename,
            @RequestParam("fileSize") long fileSize,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication) {

        log.info("Initializing upload for file: {}, size: {}", filename, fileSize);

        String username = authentication.getName();
        VideoUploadResponseDTO response = videoUploadService.initializeUpload(username, filename, fileSize, title, description);

        return ResponseEntity.ok(WebResponseDTO.success(response, "Upload initialized successfully"));
    }

    @PostMapping("/upload/chunk")
    public ResponseEntity<WebResponseDTO<ChunkUploadDTO>> uploadChunk(
            @RequestParam("videoId") Long videoId,
            @RequestParam("chunkNumber") int chunkNumber,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("file") MultipartFile chunk,
            Authentication authentication) {

        log.info("Uploading chunk {} of {} for video ID: {}", chunkNumber, totalChunks, videoId);

        String username = authentication.getName();
        ChunkUploadDTO response = videoUploadService.uploadChunk(username, videoId, chunkNumber, totalChunks, chunk);

        return ResponseEntity.ok(WebResponseDTO.success(response, "Chunk uploaded successfully"));
    }

    @PostMapping("/upload/complete")
    public ResponseEntity<WebResponseDTO<VideoDTO>> completeUpload(
            @RequestParam("videoId") Long videoId,
            Authentication authentication) {

        log.info("Completing upload for video ID: {}", videoId);

        String username = authentication.getName();
        VideoDTO videoDTO = videoUploadService.completeUpload(username, videoId);

        return ResponseEntity.ok(WebResponseDTO.success(videoDTO, "Upload completed successfully"));
    }
}