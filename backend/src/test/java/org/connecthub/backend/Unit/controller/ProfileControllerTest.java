package org.connecthub.backend.Unit.controller;

import org.connecthub.backend.controller.ProfileController;
import org.connecthub.backend.dto.request.UpdatePasswordRequest;
import org.connecthub.backend.dto.request.UpdateProfileRequest;
import org.connecthub.backend.dto.response.ContentDto;
import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.enums.ContentType;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.exception.GlobalExceptionHandler;
import org.connecthub.backend.exception.InvalidPasswordException;
import org.connecthub.backend.exception.ResourceNotFoundException;
import org.connecthub.backend.security.JwtUtil;
import org.connecthub.backend.service.ProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for ProfileController endpoints, covering both successful and error scenarios.
 * Includes authentication, validation, and service interaction tests to ensure robust API behavior.
 * Each test is annotated with @DisplayName for clarity and documentation purposes.
 * Tests are organized into nested classes by endpoint for better structure and readability.
 */
@WebMvcTest(ProfileController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@DisplayName("ProfileController HTTP Tests")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProfileService profileService;

    @MockBean private JwtUtil jwtUtil;
    @MockBean private UserDetailsService userDetailsService;

    private UUID    userId;
    private UserDto aliceDto;

    @BeforeEach
    void setUp() {
        userId   = UUID.randomUUID();
        aliceDto = new UserDto(userId, "alice", "alice@example.com",
                "Hello!", "/uploads/profiles/alice.jpg",
                "/uploads/covers/alice-cover.jpg", UserStatus.ONLINE);
    }

    private org.springframework.security.test.web.servlet.request
            .SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor authUser() {
        return user(userId.toString()).roles("USER");
    }


    @Nested
    @DisplayName("GET /")
    class GetMyProfile {

        @Test
        @DisplayName("200 — authenticated user gets own profile")
        void getMyProfile_authenticated_returns200WithDto() throws Exception {
            when(profileService.getProfile(any(UUID.class))).thenReturn(aliceDto);

            mockMvc.perform(get("/profile").with(authUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.email").value("alice@example.com"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("401 — unauthenticated request is rejected")
        void getMyProfile_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/profile"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 — user not found returns structured error")
        void getMyProfile_userNotFound_returns404() throws Exception {
            when(profileService.getProfile(any())).thenThrow(
                    new ResourceNotFoundException("User not found"));

            mockMvc.perform(get("/profile").with(authUser()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("PATCH /")
    class UpdateProfile {

        @Test
        @DisplayName("200 — valid bio update returns updated DTO")
        void updateProfile_validRequest_returns200() throws Exception {
            UserDto updated = new UserDto(userId, "alice", "alice@example.com",
                    "Updated bio", null, null, UserStatus.ONLINE);
            when(profileService.updateProfile(any(), any())).thenReturn(updated);

            UpdateProfileRequest req = new UpdateProfileRequest("Updated bio", null, null);

            mockMvc.perform(patch("/profile")
                            .with(authUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bio").value("Updated bio"));
        }

        @Test
        @DisplayName("400 — bio exceeding 300 chars fails validation")
        void updateProfile_bioTooLong_returns400() throws Exception {
            String longBio = "x".repeat(301);
            UpdateProfileRequest req = new UpdateProfileRequest(longBio, null, null);

            mockMvc.perform(patch("/profile")
                            .with(authUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.bio").exists());
        }
    }

    
    @Nested
    @DisplayName("POST /photo")
    class UploadProfilePhoto {

        @Test
        @DisplayName("200 — valid image upload returns updated profile")
        void uploadProfilePhoto_validFile_returns200() throws Exception {
            UserDto updated = new UserDto(userId, "alice", "alice@example.com",
                    null, "/uploads/profiles/new.jpg", null, UserStatus.ONLINE);
            when(profileService.updateProfilePhoto(any(), any())).thenReturn(updated);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", new byte[1024]);

        
            mockMvc.perform(multipart("/profile/photo")
                            .file(file)
                            .with(authUser())
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profilePhotoPath")
                            .value("/uploads/profiles/new.jpg"));
        }

        @Test
        @DisplayName("401 — unauthenticated upload is rejected")
        void uploadProfilePhoto_unauthenticated_returns401() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", new byte[10]);

            mockMvc.perform(multipart("/profile/photo")
                            .file(file)
                            .with(csrf()))                    // ← was missing
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PATCH /password")
    class UpdatePassword {

        @Test
        @DisplayName("204 — correct password change returns no content")
        void updatePassword_correct_returns204() throws Exception {
            doNothing().when(profileService).updatePassword(any(), any());

            UpdatePasswordRequest req = new UpdatePasswordRequest("OldPass1!", "NewPass2!");

            mockMvc.perform(patch("/profile/password")
                            .with(authUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("401 — wrong current password returns 401")
        void updatePassword_wrongCurrentPassword_returns401() throws Exception {
            doThrow(new InvalidPasswordException())
                    .when(profileService).updatePassword(any(), any());

            UpdatePasswordRequest req = new UpdatePasswordRequest("WrongPass!", "NewPass2!");

            mockMvc.perform(patch("/profile/password")
                            .with(authUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message")
                            .value("Current password is incorrect"));
        }

        @Test
        @DisplayName("400 — missing required fields fails validation")
        void updatePassword_missingFields_returns400() throws Exception {
            String body = "{\"currentPassword\":\"\",\"newPassword\":\"\"}";

            mockMvc.perform(patch("/profile/password")
                            .with(authUser()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").exists());
        }
    }

    @Nested
    @DisplayName("GET /posts")
    class GetMyPosts {

        @Test
        @DisplayName("200 — returns paginated post list")
        void getMyPosts_authenticated_returnsPaginatedPosts() throws Exception {
            ContentDto post = new ContentDto(
                    UUID.randomUUID(), "Post text", null,
                    ContentType.POST, LocalDateTime.now(), aliceDto);
            Page<ContentDto> page = new PageImpl<>(List.of(post));
            when(profileService.getOwnPosts(any(), anyInt(), anyInt())).thenReturn(page);

            mockMvc.perform(get("/profile/posts")
                            .param("page", "0").param("size", "10")
                            .with(authUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].contentText").value("Post text"))
                    .andExpect(jsonPath("$.content[0].author.username").value("alice"));
        }
    }

    @Nested
    @DisplayName("GET /friends")
    class GetMyFriends {

        @Test
        @DisplayName("200 — returns friends list with status")
        void getMyFriends_authenticated_returnsList() throws Exception {
            UserDto bob = new UserDto(UUID.randomUUID(), "bob",
                    "bob@example.com", null, null, null, UserStatus.ONLINE);
            when(profileService.getFriends(any())).thenReturn(List.of(bob));

            mockMvc.perform(get("/profile/friends").with(authUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].username").value("bob"))
                    .andExpect(jsonPath("$[0].status").value("ONLINE"));
        }

        @Test
        @DisplayName("200 — empty friends list returns empty array")
        void getMyFriends_noFriends_returnsEmptyArray() throws Exception {
            when(profileService.getFriends(any())).thenReturn(List.of());

            mockMvc.perform(get("/profile/friends").with(authUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}