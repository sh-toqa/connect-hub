package org.connecthub.backend.Integration;

import org.connecthub.backend.enums.ContentType;
import org.connecthub.backend.enums.FriendshipStatus;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.model.Content;
import org.connecthub.backend.model.Friendship;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.ContentRepository;
import org.connecthub.backend.repository.FriendshipRepository;
import org.connecthub.backend.repository.UserRepository;
import org.connecthub.backend.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.junit.jupiter.api.Nested;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ContentController
 * Full stack — real H2 database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("ContentController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContentControllerIntegrationTest {

    @Autowired private MockMvc           mockMvc;
    @Autowired private ObjectMapper      objectMapper;
    @Autowired private JwtUtil           jwtUtil;
    @Autowired private UserRepository    userRepository;
    @Autowired private ContentRepository contentRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private PasswordEncoder   passwordEncoder;

    private User   alice;
    private User   bob;
    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() {
        friendshipRepository.deleteAll();
        contentRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(User.builder()
                .email("alice@example.com").username("alice")
                .hashedPassword(passwordEncoder.encode("Password1!"))
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .status(UserStatus.ONLINE).build());

        bob = userRepository.save(User.builder()
                .email("bob@example.com").username("bob")
                .hashedPassword(passwordEncoder.encode("Password1!"))
                .dateOfBirth(LocalDate.of(1998, 6, 15))
                .status(UserStatus.ONLINE).build());

        aliceToken = jwtUtil.generateToken(alice.getEmail());
        bobToken   = jwtUtil.generateToken(bob.getEmail());

        // Alice and Bob are friends
        friendshipRepository.save(Friendship.builder()
                .requester(alice).receiver(bob)
                .status(FriendshipStatus.ACCEPTED).build());
    }

    private String auth(String token) { return "Bearer " + token; }

    @Nested
    @DisplayName("POST /content/posts")
    class CreatePostTests {

        @Test
        @DisplayName("TC-CC-01 | Create post with text — 201 Created, persisted in DB")
        void createPost_textOnly_returns201AndPersists() throws Exception {
            mockMvc.perform(post("/content/posts")
                            .param("contentText", "Hello from Alice!")
                            .header("Authorization", auth(aliceToken)))
						.andExpect(status().isCreated())
						.andExpect(jsonPath("$.contentText").value("Hello from Alice!"))
						.andExpect(jsonPath("$.contentType").value("POST"))
						.andExpect(jsonPath("$.contentId").exists())
					.andExpect(jsonPath("$.author.username").value("alice"))
					.andExpect(jsonPath("$.timestamp").exists());

        	assertThat(contentRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-CC-02 | Create post with image — 201 Created, image path saved")
        void createPost_withImage_returns201WithImagePath() throws Exception {
                byte[] imageBytes = new byte[512];
                org.springframework.mock.web.MockMultipartFile image =
                        new org.springframework.mock.web.MockMultipartFile(
                                "image", "photo.jpg", "image/jpeg", imageBytes);

                mockMvc.perform(multipart("/content/posts")
                                .file(image)
                                .param("contentText", "Post with image")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.imagePath").isNotEmpty())
                        .andExpect(jsonPath("$.contentText").value("Post with image"));
        }

        @Test
        @DisplayName("TC-CC-03 | Create post without token — 401 Unauthorized")
        void createPost_noToken_returns401() throws Exception {
                mockMvc.perform(post("/content/posts")
                                .param("contentText", "No auth post"))
                        .andExpect(status().isUnauthorized());

                assertThat(contentRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-CC-04 | Create post with no text and no image — 201 (content optional)")
        void createPost_emptyText_returns201() throws Exception {
                mockMvc.perform(post("/content/posts")
                                .param("contentText", "")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isCreated());
        }
    }

    @Nested
    @DisplayName("POST /content/stories")
    class CreateStoryTests {
        @Test
        @DisplayName("TC-CC-05 | Create story — 201 Created, contentType is STORY")
        void createStory_valid_returns201WithStoryType() throws Exception {
                mockMvc.perform(post("/content/stories")
                                .param("contentText", "My story!")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.contentType").value("STORY"))
                        .andExpect(jsonPath("$.contentText").value("My story!"))
                        .andExpect(jsonPath("$.author.username").value("alice"));
        }

        @Test
        @DisplayName("TC-CC-06 | Create story without token — 401 Unauthorized")
        void createStory_noToken_returns401() throws Exception {
                mockMvc.perform(post("/content/stories")
                                .param("contentText", "Unauthorized story"))
                        .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /content/{contentId}")
    class DeleteContentTests {
		@Test
		@DisplayName("TC-CC-07 | Delete own post — 204 No Content, removed from DB")
		void deleteContent_ownPost_returns204AndRemoved() throws Exception {
			Content post = contentRepository.save(Content.builder()
					.author(alice).contentText("To delete")
					.contentType(ContentType.POST).build());

			mockMvc.perform(delete("/content/" + post.getContentId())
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isNoContent());

			assertThat(contentRepository.findById(post.getContentId())).isEmpty();
		}

		@Test
		@DisplayName("TC-CC-08 | Delete another user's post — 404 Not Found")
		void deleteContent_notOwner_returns404() throws Exception {
			// Bob's post
			Content bobPost = contentRepository.save(Content.builder()
					.author(bob).contentText("Bob's post")
					.contentType(ContentType.POST).build());

			// Alice tries to delete Bob's post
			mockMvc.perform(delete("/content/" + bobPost.getContentId())
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isNotFound());

			// Post still exists
			assertThat(contentRepository.findById(bobPost.getContentId())).isPresent();
		}

		@Test
		@DisplayName("TC-CC-09 | Delete non-existent content — 404 Not Found")
		void deleteContent_notFound_returns404() throws Exception {
			mockMvc.perform(delete("/content/00000000-0000-0000-0000-000000000000")
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isNotFound());
		}

		@Test
		@DisplayName("TC-CC-10 | Delete content without token — 401 Unauthorized")
		void deleteContent_noToken_returns401() throws Exception {
			Content post = contentRepository.save(Content.builder()
					.author(alice).contentText("Some post")
					.contentType(ContentType.POST).build());

			mockMvc.perform(delete("/content/" + post.getContentId()))
					.andExpect(status().isUnauthorized());

			assertThat(contentRepository.findById(post.getContentId())).isPresent();
		}
	}

	@Nested
	@DisplayName("GET /content/feed/posts")
	class GetFeedPostsTests {
		@Test
		@DisplayName("TC-CC-11 | Get feed posts — 200, returns friend posts paginated")
		void getFeedPosts_withFriendPosts_returnsPaginatedList() throws Exception {
			// Bob (Alice's friend) creates posts
			contentRepository.save(Content.builder()
					.author(bob).contentText("Bob post 1")
					.contentType(ContentType.POST).build());
			contentRepository.save(Content.builder()
					.author(bob).contentText("Bob post 2")
					.contentType(ContentType.POST).build());

			mockMvc.perform(get("/content/feed/posts")
							.param("page", "0").param("size", "10")
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content").isArray())
					.andExpect(jsonPath("$.content.length()").value(2))
					.andExpect(jsonPath("$.content[0].author.username").value("bob"));
		}

		@Test
		@DisplayName("TC-CC-12 | Get feed posts with no friends — 200, returns empty page")
		void getFeedPosts_noFriends_returnsEmptyPage() throws Exception {
			// Carol has no friends
			User carol = userRepository.save(User.builder()
					.email("carol@example.com").username("carol")
					.hashedPassword(passwordEncoder.encode("Password1!"))
					.dateOfBirth(LocalDate.of(1999, 3, 3))
					.status(UserStatus.ONLINE).build());
			String carolToken = jwtUtil.generateToken(carol.getEmail());

			mockMvc.perform(get("/content/feed/posts")
							.header("Authorization", auth(carolToken)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content").isEmpty());
		}

		@Test
		@DisplayName("TC-CC-13 | Feed does not include own posts — only friends' posts")
		void getFeedPosts_doesNotIncludeOwnPosts() throws Exception {
			// Alice creates a post — should NOT appear in her own feed
			contentRepository.save(Content.builder()
					.author(alice).contentText("Alice's own post")
					.contentType(ContentType.POST).build());

			mockMvc.perform(get("/content/feed/posts")
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content").isEmpty());
		}

		@Test
		@DisplayName("TC-CC-14 | Feed pagination works — page and size params respected")
		void getFeedPosts_pagination_respectsPageAndSize() throws Exception {
			for (int i = 1; i <= 5; i++) {
				contentRepository.save(Content.builder()
						.author(bob).contentText("Bob post " + i)
						.contentType(ContentType.POST).build());
			}

			mockMvc.perform(get("/content/feed/posts")
							.param("page", "0").param("size", "3")
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content.length()").value(3))
					.andExpect(jsonPath("$.totalElements").value(5))
					.andExpect(jsonPath("$.last").value(false));
		}

		@Test
		@DisplayName("TC-CC-15 | Get feed posts without token — 401 Unauthorized")
		void getFeedPosts_noToken_returns401() throws Exception {
			mockMvc.perform(get("/content/feed/posts"))
					.andExpect(status().isUnauthorized());
		}
	}

	@Nested
	@DisplayName("GET /content/feed/stories")
	class GetFeedStoriesTests {
		@Test
		@DisplayName("TC-CC-16 | Get feed stories — 200, returns active friend stories")
		void getFeedStories_withActiveStories_returnsList() throws Exception {
			contentRepository.save(Content.builder()
					.author(bob).contentText("Bob's story!")
					.contentType(ContentType.STORY).build());

			mockMvc.perform(get("/content/feed/stories")
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$").isArray())
					.andExpect(jsonPath("$[0].contentType").value("STORY"))
					.andExpect(jsonPath("$[0].author.username").value("bob"));
		}

		@Test
		@DisplayName("TC-CC-17 | Feed stories excludes expired stories (older than 24h)")
		void getFeedStories_expiredStories_notReturned() throws Exception {
			// Save a story and manually backdate it to 25 hours ago
			Content expired = contentRepository.save(Content.builder()
					.author(bob).contentText("Expired story")
					.contentType(ContentType.STORY).build());
			expired.setTimestamp(LocalDateTime.now().minusHours(25));
			contentRepository.save(expired);

			// Active story
			contentRepository.save(Content.builder()
					.author(bob).contentText("Active story")
					.contentType(ContentType.STORY).build());

			mockMvc.perform(get("/content/feed/stories")
							.header("Authorization", auth(aliceToken)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.length()").value(1))
					.andExpect(jsonPath("$[0].contentText").value("Active story"));
		}

		@Test
		@DisplayName("TC-CC-18 | Get feed stories without token — 401 Unauthorized")
		void getFeedStories_noToken_returns401() throws Exception {
			mockMvc.perform(get("/content/feed/stories"))
					.andExpect(status().isUnauthorized());
		}
	}
}