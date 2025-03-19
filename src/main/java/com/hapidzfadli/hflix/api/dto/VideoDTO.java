package com.hapidzfadli.hflix.api.dto;

import com.hapidzfadli.hflix.domain.entity.Video;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * VideoDTO
 *
 * Purpose: Transfers video data between the controller and client.
 * This DTO represents the complete video metadata for display in the UI,
 * including all details needed for video listings and detail pages.
 * It excludes sensitive information and internal implementation details.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoDTO {
    private Long id;
    private String title;
    private String description;
    private String originalFilename;
    private Long fileSize;
    private Integer duration;
    private String thumbnailPath;
    private Video.Status status;
    private Video.Visibility visibility;
    private Long viewCount;
    private Long downloadCount;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static VideoDTO fromVideo(Video video) {
        return VideoDTO.builder()
                .id(video.getId())
                .title(video.getTitle())
                .description(video.getDescription())
                .originalFilename(video.getOriginalFilename())
                .fileSize(video.getFileSize())
                .duration(video.getDuration())
                .thumbnailPath(video.getThumbnailPath())
                .status(video.getStatus())
                .visibility(video.getVisibility())
                .viewCount(video.getViewCount())
                .downloadCount(video.getDownloadCount())
                .username(video.getUser().getUsername())
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .build();
    }
}