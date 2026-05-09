package org.connecthub.backend.controller;

import org.connecthub.backend.dto.request.UpdatePasswordRequest;
import org.connecthub.backend.dto.request.UpdateProfileRequest;
import org.connecthub.backend.dto.response.ContentDto;
import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * All endpoints under /profile require a valid JWT.
 *
 * GET    /profile               — view own profile
 * PATCH  /profile               — update bio / paths
 * POST   /profile/photo         — upload profile photo
 * POST   /profile/cover         — upload cover photo
 * PATCH  /profile/password      — change password
 * GET    /profile/posts         — paginated own posts
 * GET    /profile/friends       — friends list with status
 * GET    /profile/{userId}      — view another user's public profile
 */

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    // View own profile
    @GetMapping
    public ResponseEntity<UserDto> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        System.out.println("Authenticated user: " + userDetails.getUsername());
        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    // View another user's public profile
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserProfile(@PathVariable UUID userId) {
        return ResponseEntity.ok(profileService.getProfile(userId));
    }

    // Update bio and profile paths
    @PatchMapping()
    public ResponseEntity<UserDto> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(profileService.updateProfile(userId, request));
    }

    // Upload profile photo and cover photo
    @PostMapping("/photo")
    public ResponseEntity<UserDto> uploadProfilePhoto(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(profileService.updateProfilePhoto(userId, file));
    }

    // Upload cover photo
    @PostMapping("/cover")
    public ResponseEntity<UserDto> uploadCoverPhoto(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file) {
        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(profileService.updateCoverPhoto(userId, file));
    }


    // Change password
    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePasswordRequest request) {
        UUID userId = extractUserId(userDetails);
        profileService.updatePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    // Get paginated own posts
    @GetMapping("/posts")
    public ResponseEntity<Page<ContentDto>> getMyPosts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(profileService.getOwnPosts(userId, page, size));
    }

    @GetMapping("/stories")
    public ResponseEntity<Page<ContentDto>> getMyStories(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(profileService.getOwnStories(userId, page, size));
    }

    // Get friends list with status
    @GetMapping("/friends")
    public ResponseEntity<List<UserDto>> getMyFriends(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = extractUserId(userDetails);
        return ResponseEntity.ok(profileService.getFriends(userId));
    }

    // Helper method to extract user ID from UserDetails
    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}