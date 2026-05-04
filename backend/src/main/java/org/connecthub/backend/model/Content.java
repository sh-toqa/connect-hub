package org.connecthub.backend.model;

import org.connecthub.backend.enums.ContentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing user-generated content such as posts and stories.
 * Includes fields for content text, optional image, content type (post or story), timestamp, and author.
 */
@Entity
@Table(name = "contents", indexes = {
        @Index(name = "idx_content_author", columnList = "author_id"),
        @Index(name = "idx_content_type_timestamp", columnList = "content_type, timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Content {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT")
    private String contentText;

    @Column(length = 500)
    private String imagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    private ContentType contentType;

    @CreationTimestamp
    @Column(name = "timestamp", updatable = false, nullable = false)
    private LocalDateTime timestamp;
}