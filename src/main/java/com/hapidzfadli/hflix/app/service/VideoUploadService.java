package com.hapidzfadli.hflix.app.service;

import com.hapidzfadli.hflix.api.dto.ChunkUploadDTO;
import com.hapidzfadli.hflix.api.dto.VideoDTO;
import com.hapidzfadli.hflix.api.dto.VideoUploadResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface VideoUploadService {
    VideoUploadResponseDTO initializeUpload(String username, String filename, long fileSize, String title, String description);
    ChunkUploadDTO uploadChunk(String username, Long videoId, int chunkNumber, int totalChunks, MultipartFile chunk);
    VideoDTO completeUpload(String username, Long videoId);
}
