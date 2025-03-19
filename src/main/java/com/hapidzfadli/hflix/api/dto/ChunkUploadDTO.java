package com.hapidzfadli.hflix.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ChunkUploadDTO
 *
 * Purpose: Communicates chunk upload status back to the client.
 * This DTO helps clients track their upload progress and know which
 * chunk to send next, supporting reliable resumable uploads.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadDTO {
    private Long videoId;
    private int chunkNumber;
    private boolean received;
    private int nextExpectedChunk;
}