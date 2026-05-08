package org.connecthub.backend.controller;

import org.connecthub.backend.dto.response.ContentDto;
import org.connecthub.backend.enums.ContentType;
import org.connecthub.backend.dto.request.CreateContentRequest;
import org.connecthub.backend.service.ContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    // POST /content/posts — accepts contentText as a plain request param + optional image file
    @PostMapping("/posts")
    public ResponseEntity<ContentDto> createPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "contentText", required = false) String contentText,
            @RequestParam(value = "image",       required = false) MultipartFile image) {

        UUID authorId = extractUserId(userDetails);
        CreateContentRequest request = new CreateContentRequest(contentText, null, ContentType.POST);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contentService.createPost(authorId, request, image));
    }

    @PostMapping("/stories")
    public ResponseEntity<ContentDto> createStory(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "contentText", required = false) String contentText,
            @RequestParam(value = "image",       required = false) MultipartFile image) {

        UUID authorId = extractUserId(userDetails);
        CreateContentRequest request = new CreateContentRequest(contentText, null, ContentType.STORY);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contentService.createStory(authorId, request, image));
    }

    @DeleteMapping("/{contentId}")
    public ResponseEntity<Void> deleteContent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID contentId) {
        contentService.deleteContent(contentId, extractUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/feed/posts")
    public ResponseEntity<Page<ContentDto>> getFeedPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(contentService.getFriendPosts(extractUserId(userDetails), page, size));
    }

    @GetMapping("/feed/stories")
    public ResponseEntity<List<ContentDto>> getFeedStories(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(contentService.getFriendStories(extractUserId(userDetails)));
    }

    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}