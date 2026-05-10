package org.connecthub.backend.controller;

import org.connecthub.backend.dto.response.FriendshipDto;
import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.service.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * POST   /friends/request/{receiverId}          — send friend request
 * POST   /friends/{friendshipId}/accept         — accept request
 * DELETE /friends/{friendshipId}/decline        — decline request
 * DELETE /friends/{friendshipId}                — remove friend
 * POST   /friends/block/{targetId}              — block user
 * DELETE /friends/block/{targetId}              — unblock user
 * GET    /friends/requests                      — pending received requests
 * GET    /friends                               — accepted friends list
 * GET    /friends/suggestions                   — friend suggestions
 * GET    /friends/status/{otherUserId}          — relationship status
 */
@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request/{receiverId}")
    public ResponseEntity<FriendshipDto> sendRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID receiverId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(friendshipService.sendRequest(extractUserId(userDetails), receiverId));
    }

    @PostMapping("/{friendshipId}/accept")
    public ResponseEntity<FriendshipDto> acceptRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID friendshipId) {
        return ResponseEntity.ok(
                friendshipService.acceptRequest(friendshipId, extractUserId(userDetails)));
    }

    @DeleteMapping("/{friendshipId}/decline")
    public ResponseEntity<Void> declineRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID friendshipId) {
        friendshipService.declineRequest(friendshipId, extractUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<Void> removeFriend(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID friendshipId) {
        friendshipService.removeFriend(friendshipId, extractUserId(userDetails));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/block/{targetId}")
    public ResponseEntity<FriendshipDto> blockUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID targetId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(friendshipService.blockUser(extractUserId(userDetails), targetId));
    }

    @DeleteMapping("/block/{targetId}")
    public ResponseEntity<Void> unblockUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID targetId) {
        friendshipService.unblockUser(extractUserId(userDetails), targetId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendshipDto>> getPendingRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                friendshipService.getPendingRequests(extractUserId(userDetails)));
    }

    @GetMapping
    public ResponseEntity<List<FriendshipDto>> getFriends(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                friendshipService.getFriends(extractUserId(userDetails)));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<List<UserDto>> getSuggestions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                friendshipService.getSuggestions(extractUserId(userDetails)));
    }

    @GetMapping("/status/{otherUserId}")
    public ResponseEntity<Map<String, String>> getStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID otherUserId) {
        String status = friendshipService.getStatus(extractUserId(userDetails), otherUserId);
        return ResponseEntity.ok(Map.of("status", status));
    }

    private UUID extractUserId(UserDetails userDetails) {
        return UUID.fromString(userDetails.getUsername());
    }
}