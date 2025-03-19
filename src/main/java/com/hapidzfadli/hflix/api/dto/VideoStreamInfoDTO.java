package com.hapidzfadli.hflix.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * VideoStreamInfoDTO
 *
 * Purpose: Provides all information needed for video playback.
 * This DTO aggregates video metadata with available streaming formats,
 * enabling the client to implement adaptive bitrate streaming by
 * selecting the appropriate resolution based on network conditions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoStreamInfoDTO {
    private Long videoId;
    private String title;
    private Integer duration;
    private String thumbnailUrl;
    private List<VideoFormatDTO> formats;
}
