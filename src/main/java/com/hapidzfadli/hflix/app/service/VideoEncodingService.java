package com.hapidzfadli.hflix.app.service;

import com.hapidzfadli.hflix.domain.entity.Video;
import com.hapidzfadli.hflix.domain.entity.VideoFormat;

public interface VideoEncodingService {
    void startEncodingJob(Video video);
    void processEncodingJob(Long videoId, String resolution, String codec);
    void updateEncodingStatus(Long videoId, String resolution, VideoFormat.Status status, int progress);
    void generateThumbnail(Video video);
}
