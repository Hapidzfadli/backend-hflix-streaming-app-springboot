package com.hapidzfadli.hflix.domain.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * VideoView Entity
 *
 * Purpose: Records detailed analytics about each video view.
 * This entity maintains relationships with Video and User entities,
 * enabling comprehensive analytics while maintaining referential integrity.
 * It tracks when, how, and by whom videos are viewed in the system.
 */
@Entity
@Data
@Table(name = "video_views")
public class VideoView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // Nullable - for anonymous views

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    @Column(name = "watch_duration")
    private Integer watchDuration;  // in seconds

    @Column(length = 20)
    private String resolution;
}