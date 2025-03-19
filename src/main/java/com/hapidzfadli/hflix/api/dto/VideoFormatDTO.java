package com.hapidzfadli.hflix.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VideoFormatDTO
 *
 * Purpose: Represents a specific encoding format of a video.
 * This DTO contains technical details about a video encoding option,
 * used when presenting available streaming options to the client
 * for adaptive bitrate streaming.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoFormatDTO {
    private Long id;
    private String resolution;
    private String codec;
    private Integer bitrate;
}
