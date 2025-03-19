package com.hapidzfadli.hflix.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * VideoUploadResponseDTO
 *
 * Purpose: Provides necessary information to the client after upload initialization.
 * This DTO contains all the details a client needs to begin the chunked upload process,
 * including the upload endpoint, recommended chunk size, and tracking identifiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadResponseDTO {
    private Long videoId;
    private String uploadUrl;
    private int maxChunkSize; // in bytes
    private boolean resumable;
}