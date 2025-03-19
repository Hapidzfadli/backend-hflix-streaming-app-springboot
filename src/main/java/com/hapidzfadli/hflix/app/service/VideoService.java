package com.hapidzfadli.hflix.app.service;

import com.hapidzfadli.hflix.api.dto.VideoDTO;
import com.hapidzfadli.hflix.api.dto.VideoStreamInfoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface VideoService {
    VideoDTO getVideo(Long id);
    Page<VideoDTO> findPublicVideos(String keyword, Pageable pageable);
    Page<VideoDTO> findUserVideos(String username, String keyword, Pageable pageable);
    VideoStreamInfoDTO getVideoStreamInfo(Long videoId);
    ResponseEntity<byte[]> streamVideo(Long videoId, String resolution, String rangeHeader);
    void recordVideoView(Long videoId, String username, String ipAddress, String userAgent, String resolution);
}
