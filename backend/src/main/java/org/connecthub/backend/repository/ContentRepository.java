package org.connecthub.backend.repository;

import org.connecthub.backend.enums.ContentType;
import org.connecthub.backend.model.Content;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {

    // Find content by author and type, newest first, paginated - for profile page
    Page<Content> findByAuthor_UserIdAndContentTypeOrderByTimestampDesc(
            UUID authorId, ContentType contentType, Pageable pageable);
    // Newsfeed: posts from a list of friend IDs, newest first, paginated.
    // Excludes content from blocked users (handled in service layer via friendIds list).
    
    @Query("""
        SELECT c FROM Content c
        WHERE c.author.userId IN :friendIds
        AND c.contentType = :contentType
        ORDER BY c.timestamp DESC
        """)
    Page<Content> findFriendContent(
            @Param("friendIds") List<UUID> friendIds,
            @Param("contentType") ContentType contentType,
            Pageable pageable);

    // Delete expired stories (older than 24 hours) - scheduled task will call this method daily
    @Modifying
    @Query("DELETE FROM Content c WHERE c.contentType = 'STORY' AND c.timestamp < :expiryTime")
    int deleteExpiredStories(@Param("expiryTime") LocalDateTime expiryTime);
}