package com.hapidzfadli.hflix.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "video_formats")
public class VideoFormat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false)
    private String resolution;

    @Column(nullable = false)
    private String codec;

    @Column(nullable = false)
    private Integer bitrate;

    @Column(name= "s3_path", nullable = false)
    private String s3Path;

    @Column(name = "file_size", nullable = false)
    private String fileSize;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PROCESSING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        PROCESSING,
        READY,
        ERROR
    }


}
