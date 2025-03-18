package com.hapidzfadli.hflix.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "videos")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name="original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "s3_path", nullable = false)
    private String s3Path;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column
    private Integer duration;

    @Column(name= "thumbnail_path")
    private String thumbnailPath;

    @Enumerated(EnumType.STRING)
    private Status status = Status.UPLOADING;

    @Enumerated(EnumType.STRING)
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "encoding_status", columnDefinition = "JSON")
    private String encodingStatus;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "download_count")
    private Long downloadCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
private LocalDateTime updatedAt;

    public enum Status {
        UPLOADING,
        PROCESSING,
        READY,
        ERROR,
    }

    public enum Visibility {
        PUBLIC,
        PRIVATE,
        UNLISTED,
    }
}
