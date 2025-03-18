package com.hapidzfadli.hflix.domain.repository;

import com.hapidzfadli.hflix.domain.entity.Video;
import com.hapidzfadli.hflix.domain.entity.VideoFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoFormatRepository extends JpaRepository<VideoFormat, Long> {
    /**
     * Finds all formats associated with a specific video.
     * Used to display a list of all available encoding formats for a video.
     *
     * @param video The video whose formats are to be found.
     * @return A list of video formats associated with the specific video.
     */
    List<VideoFormat> findByVideo(Video video);

    /**
     * Finds video formats based on the video and format status.
     * Used for monitoring the encoding process, to see which formats are ready or still processing.
     *
     * @param video The video whose formats are to be found.
     * @param status The status of the video format.
     * @return A list of video formats matching the specific video and status.
     */
    List<VideoFormat> findByVideoAndStatus(Video video, VideoFormat.Status status);

    /**
     * Finds video formats based on the video and resolution.
     * Used to get the streaming or download URL for a specific resolution.
     * Returns an Optional because the specific resolution format might not be available.
     *
     * @param video The video whose formats are to be found.
     * @param resolution The resolution of the video format.
     * @return An Optional containing the video format matching the specific video and resolution.
     */
    Optional<VideoFormat> findByVideoAndResolution(Video video, String resolution);

    /**
     * Finds video formats based on the video, resolution, and codec.
     * Used to get a specific format that matches the user's device preferences.
     *
     * @param video The video whose formats are to be found.
     * @param resolution The resolution of the video format.
     * @param codec The codec of the video format.
     * @return An Optional containing the video format matching the specific video, resolution, and codec.
     */
    Optional<VideoFormat> findByVideoAndResolutionAndCodec(Video video, String resolution, String codec);

    /**
     * Counts the number of formats that are completed for a specific video.
     * Used to track encoding progress for displaying progress to the user.
     *
     * @param video The video whose formats are to be counted.
     * @param status The status of the video format.
     * @return The number of video formats that are completed for the specific video.
     */
    long countByVideoAndStatus(Video video, VideoFormat.Status status);
}