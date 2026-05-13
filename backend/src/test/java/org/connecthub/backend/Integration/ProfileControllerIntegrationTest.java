package org.connecthub.backend.Integration;

import org.connecthub.backend.dto.request.UpdatePasswordRequest;
import org.connecthub.backend.dto.request.UpdateProfileRequest;
import org.connecthub.backend.enums.ContentType;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.model.Content;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.ContentRepository;
import org.connecthub.backend.repository.FriendshipRepository;
import org.connecthub.backend.repository.UserRepository;
import org.connecthub.backend.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ProfileController
 * Full stack — real H2 database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ProfileController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProfileControllerIntegrationTest {

    @Autowired private MockMvc           mockMvc;
    @Autowired private ObjectMapper      objectMapper;
    @Autowired private JwtUtil           jwtUtil;
    @Autowired private UserRepository    userRepository;
    @Autowired private ContentRepository contentRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private PasswordEncoder   passwordEncoder;

    private User   alice;
    private String aliceToken;

    @BeforeEach
    void setUp() {
        friendshipRepository.deleteAll();
        contentRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(User.builder()
                .email("alice@example.com")
                .username("alice")
                .hashedPassword(passwordEncoder.encode("Password1!"))
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .status(UserStatus.ONLINE)
                .bio("Hello!")
                .build());

        aliceToken = jwtUtil.generateToken(alice.getEmail());
    }

    private String auth(String token) {
        return "Bearer " + token;
    }

    @Nested
    @DisplayName("GET /profile and GET /profile/{userId}")
    class GetProfileTests {

        @Test
        @DisplayName("TC-PC-01 | Get own profile — 200, returns profile without password")
        void getMyProfile_authenticated_returns200() throws Exception {
                mockMvc.perform(get("/profile")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.email").value("alice@example.com"))
                        .andExpect(jsonPath("$.username").value("alice"))
                        .andExpect(jsonPath("$.userId").exists())
                        .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("TC-PC-02 | Get own profile without token — 401 Unauthorized")
        void getMyProfile_noToken_returns401() throws Exception {
                mockMvc.perform(get("/profile").with(csrf()))
                        .andExpect(status().isUnauthorized());
        }


        @Test
        @DisplayName("TC-PC-03 | Get another user's profile — 200, returns public profile")
        void getUserProfile_existingUser_returns200() throws Exception {
                User bob = userRepository.save(User.builder()
                        .email("bob@example.com").username("bob")
                        .hashedPassword(passwordEncoder.encode("Password1!"))
                        .dateOfBirth(LocalDate.of(1995, 5, 5))
                        .status(UserStatus.OFFLINE).build());

                mockMvc.perform(get("/profile/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.username").value("bob"))
                        .andExpect(jsonPath("$.password").doesNotExist());
        }

        @Test
        @DisplayName("TC-PC-04 | Get non-existent user profile — 404 Not Found")
        void getUserProfile_nonExistent_returns404() throws Exception {
                mockMvc.perform(get("/profile/00000000-0000-0000-0000-000000000000")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isNotFound());
        }
    }

    
    @Nested
    @DisplayName("PATCH /profile")
    class UpdateProfileTests {
        @Test
        @DisplayName("TC-PC-05 | Update bio — 200, bio persisted in database")
        void updateProfile_validBio_returns200AndPersists() throws Exception {
                UpdateProfileRequest req = new UpdateProfileRequest("New bio!", null, null);

                mockMvc.perform(patch("/profile")
                                .header("Authorization", auth(aliceToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.bio").value("New bio!"));

                User updated = userRepository.findById(alice.getUserId()).orElseThrow();
                assertThat(updated.getBio()).isEqualTo("New bio!");
        }

        @Test
        @DisplayName("TC-PC-06 | Update bio exceeding 300 chars — 400 Bad Request")
        void updateProfile_bioTooLong_returns400() throws Exception {
                UpdateProfileRequest req = new UpdateProfileRequest("x".repeat(301), null, null);

                mockMvc.perform(patch("/profile")
                                .header("Authorization", auth(aliceToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.fieldErrors.bio").exists());
        }

        @Test
        @DisplayName("TC-PC-07 | Update profile without token — 401 Unauthorized")
        void updateProfile_noToken_returns401() throws Exception {
                mockMvc.perform(patch("/profile/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"bio\":\"bio\"}"))
                        .andExpect(status().isUnauthorized());
        }
	}


    @Nested
    @DisplayName("POST /profile/photo")
    public class InnerProfileControllerIntegrationTest {
    
        @Test
        @DisplayName("TC-PC-08 | Upload valid profile photo — 200, path saved in DB")
        void uploadProfilePhoto_validImage_returns200() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", new byte[512]);

                mockMvc.perform(multipart("/profile/photo")
                                .file(file)
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.profilePhotoPath").isNotEmpty());

                User updated = userRepository.findById(alice.getUserId()).orElseThrow();
                assertThat(updated.getProfilePhotoPath()).isNotNull();
        }

        @Test
        @DisplayName("TC-PC-09 | Upload photo without token — 401 Unauthorized")
        void uploadProfilePhoto_noToken_returns401() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "photo.jpg", "image/jpeg", new byte[512]);

                mockMvc.perform(multipart("/profile/photo").file(file))
                        .andExpect(status().isUnauthorized());
        }
    }
    

    @Nested
    @DisplayName("POST /profile/cover")
    class CoverPhotoTests {
        @Test
        @DisplayName("TC-PC-10 | Upload valid cover photo — 200, path saved in DB")
        void uploadCoverPhoto_validImage_returns200() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "cover.jpg", "image/jpeg", new byte[1024]);

                mockMvc.perform(multipart("/profile/cover")
                                .file(file)
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.coverPhotoPath").isNotEmpty());
        }
	}


    @Nested
    @DisplayName("PATCH /profile/password")
    class UpdatePasswordTests {
        @Test
        @DisplayName("TC-PC-11 | Change password with correct current — 204 No Content")
        void updatePassword_correctCurrent_returns204() throws Exception {
                UpdatePasswordRequest req = new UpdatePasswordRequest("Password1!", "NewPass2!");

                mockMvc.perform(patch("/profile/password")
                                .header("Authorization", auth(aliceToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isNoContent());

                // Old password should no longer work
                User updated = userRepository.findById(alice.getUserId()).orElseThrow();
                assertThat(passwordEncoder.matches("Password1!", updated.getHashedPassword())).isFalse();
                assertThat(passwordEncoder.matches("NewPass2!", updated.getHashedPassword())).isTrue();
        }

        @Test
        @DisplayName("TC-PC-12 | Change password with wrong current — 401 Unauthorized")
        void updatePassword_wrongCurrent_returns401() throws Exception {
                UpdatePasswordRequest req = new UpdatePasswordRequest("WrongPass!", "NewPass2!");

                mockMvc.perform(patch("/profile/password")
                                .header("Authorization", auth(aliceToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.message").value("Current password is incorrect"));
        }

        @Test
        @DisplayName("TC-PC-13 | Change password with too-short new password — 400 Bad Request")
        void updatePassword_shortNewPassword_returns400() throws Exception {
                UpdatePasswordRequest req = new UpdatePasswordRequest("Password1!", "short");

                mockMvc.perform(patch("/profile/password")
                                .header("Authorization", auth(aliceToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.fieldErrors.newPassword").exists());
        }
	}

    
    @Nested
    @DisplayName("GET /profile/posts")
    class GetMyPostsTests {
		@Test
		@DisplayName("TC-PC-14 | Get own posts — 200, returns paginated posts")
		void getMyPosts_withPosts_returnsPaginatedList() throws Exception {
			contentRepository.save(Content.builder()
					.author(alice).contentText("First post")
					.contentType(ContentType.POST).build());
			contentRepository.save(Content.builder()
					.author(alice).contentText("Second post")
					.contentType(ContentType.POST).build());

			mockMvc.perform(get("/profile/posts")
							.param("page", "0").param("size", "10")
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content").isArray())
					.andExpect(jsonPath("$.content.length()").value(2))
					.andExpect(jsonPath("$.content[0].contentText").exists())
					.andExpect(jsonPath("$.totalElements").value(2));
		}

		@Test
		@DisplayName("TC-PC-15 | Get own posts with no posts — 200, returns empty page")
		void getMyPosts_noPosts_returnsEmptyPage() throws Exception {
			mockMvc.perform(get("/profile/posts")
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content").isEmpty())
					.andExpect(jsonPath("$.totalElements").value(0));
		}

		@Test
		@DisplayName("TC-PC-16 | Get own posts without token — 401 Unauthorized")
		void getMyPosts_noToken_returns401() throws Exception {
			mockMvc.perform(get("/profile/posts"))
					.andExpect(status().isUnauthorized());
		}
	}

    @Nested
    @DisplayName("GET /profile/friends")
    class GetMyFriendsTests {
		@Test
		@DisplayName("TC-PC-17 | Get friends with no friends — 200, returns empty list")
		void getMyFriends_noFriends_returnsEmptyList() throws Exception {
			mockMvc.perform(get("/profile/friends")
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$").isArray())
					.andExpect(jsonPath("$").isEmpty());
		}

		@Test
		@DisplayName("TC-PC-18 | Get friends without token — 401 Unauthorized")
		void getMyFriends_noToken_returns401() throws Exception {
			mockMvc.perform(get("/profile/friends"))
					.andExpect(status().isUnauthorized());
		}
	}
}