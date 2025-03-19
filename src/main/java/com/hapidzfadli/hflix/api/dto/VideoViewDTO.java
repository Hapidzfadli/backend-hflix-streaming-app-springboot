package com.hapidzfadli.hflix.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * VideoViewDTO
 *
 * Purpose: Captures analytics data for a video view event.
 * This DTO records detailed information about who watched a video,
 * when they watched it, and how they watched it, enabling
 * comprehensive analytics and personalized recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoViewDTO {
    private Long id;
    private Long videoId;
    private Long userId; // Optional, may be null for anonymous views
    private String ipAddress;
    private String userAgent;
    private LocalDateTime viewedAt;
    private Integer watchDuration;
    private String resolution;
}
