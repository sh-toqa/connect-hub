package org.connecthub.backend.service;

import org.connecthub.backend.dto.request.CreateContentRequest;
import org.connecthub.backend.dto.response.ContentDto;
import org.connecthub.backend.enums.ContentType;
import org.connecthub.backend.exception.ResourceNotFoundException;
import org.connecthub.backend.mapper.UserMapper;
import org.connecthub.backend.model.Content;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.ContentRepository;
import org.connecthub.backend.repository.FriendshipRepository;
import org.connecthub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Handles all content creation use cases:
 *  - Create post (permanent)
 *  - Create story (expires after 24 hours)
 *  - Delete own content
 *  - Fetch newsfeed (friend posts + stories)
 *  - Scheduled story expiry cleanup
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository    contentRepository;
    private final UserRepository       userRepository;
    private final FriendshipRepository friendshipRepository;
    private final StorageService       storageService;
    private final UserMapper           userMapper;

    @Transactional
    public ContentDto createPost(UUID authorId, CreateContentRequest request,
                                 MultipartFile image) {
        return createContent(authorId, request.contentText(), image, ContentType.POST);
    }


    @Transactional
    public ContentDto createStory(UUID authorId, CreateContentRequest request,
                                  MultipartFile image) {
        return createContent(authorId, request.contentText(), image, ContentType.STORY);
    }

    @Transactional
    public void deleteContent(UUID contentId, UUID requesterId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found: " + contentId));

        // Only the author can delete their own content
        if (!content.getAuthor().getUserId().equals(requesterId)) {
            throw new ResourceNotFoundException("Content not found: " + contentId);
        }

        // Clean up image file if one was attached
        storageService.delete(content.getImagePath());
        contentRepository.delete(content);
    }

    public Page<ContentDto> getFriendPosts(UUID userId, int page, int size) {
        List<UUID> friendIds = getAcceptedFriendIds(userId);
        if (friendIds.isEmpty()) return Page.empty();

        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return contentRepository
                .findFriendContent(friendIds, ContentType.POST, pageable)
                .map(userMapper::toContentDto);
    }

    public List<ContentDto> getFriendStories(UUID userId) {
        List<UUID> friendIds = getAcceptedFriendIds(userId);
        if (friendIds.isEmpty()) return List.of();

        // Stories expire after 24 hours
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        PageRequest pageable = PageRequest.of(0, 50, Sort.by("timestamp").descending());
        return contentRepository
                .findFriendContent(friendIds, ContentType.STORY, pageable)
                .stream()
                .filter(c -> c.getTimestamp().isAfter(cutoff))
                .map(userMapper::toContentDto)
                .toList();
    }

    // Runs every hour — deletes stories older than 24 hours from the database
    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void deleteExpiredStories() {
        LocalDateTime expiryTime = LocalDateTime.now().minusHours(24);
        int deleted = contentRepository.deleteExpiredStories(expiryTime);
        if (deleted > 0) {
            log.info("Story cleanup: deleted {} expired stories", deleted);
        }
    }

    // Helper method to handle both post and story creation logic
    private ContentDto createContent(UUID authorId, String text,
                                     MultipartFile image, ContentType type) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authorId));

        String imagePath = null;
        if (image != null && !image.isEmpty()) {
            imagePath = storageService.store(image, type == ContentType.POST ? "posts" : "stories");
        }

        Content content = Content.builder()
                .author(author)
                .contentText(text)
                .imagePath(imagePath)
                .contentType(type)
                .build();

        return userMapper.toContentDto(contentRepository.saveAndFlush(content));
    }

    // Helper method to get a list of accepted friend IDs for a user, used to fetch newsfeed content
    private List<UUID> getAcceptedFriendIds(UUID userId) {
        List<UUID> friendIds = new ArrayList<>(
        friendshipRepository.findAcceptedFriendships(userId)
                .stream()
                .map(f -> f.getRequester().getUserId().equals(userId)
                        ? f.getReceiver().getUserId()
                        : f.getRequester().getUserId())
                .toList()
        );
        List<UUID> blockedIds = friendshipRepository.findBlockedUserIds(userId);
        friendIds.removeAll(blockedIds);
        return friendIds;
    }
}