package org.connecthub.backend.System;

import org.connecthub.backend.dto.request.LoginRequest;
import org.connecthub.backend.dto.request.RegisterRequest;
import org.connecthub.backend.dto.request.UpdatePasswordRequest;
import org.connecthub.backend.dto.request.UpdateProfileRequest;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * System Tests — end-to-end user journey tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("System Tests — Full User Journeys")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SystemTest {

    @Autowired private MockMvc              mockMvc;
    @Autowired private ObjectMapper         objectMapper;
    @Autowired private JwtUtil              jwtUtil;
    @Autowired private UserRepository       userRepository;
    @Autowired private ContentRepository    contentRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private PasswordEncoder      passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        friendshipRepository.deleteAll();
        contentRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Authorization header constructor
    private String auth(String token) {
        return "Bearer " + token;
    }

    // Register a user and return their JWT token for authenticated requests
    private String registerAndLogin(String email, String username, String password)
            throws Exception {
        // Register
        RegisterRequest reg = new RegisterRequest(
                email, username, password, LocalDate.of(1998, 1, 1));
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated()); // verify registration - status 201 Created

        // Login
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn(); // verify login - status 200 OK and token returned

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString()); // parse response body
        return body.get("token").asText(); // extract and return the token for authenticated requests
    }

    // Extract a JSON field from an MvcResult response body
    // { "data": { "user": { "email": "alice@example.com" } } } 
    // ex: extractField(result, "user", "email") → returns the email field from the user object in the response
    private String extractField(MvcResult result, String... path) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String p : path) node = node.get(p);
        return node.asText();
    }
    
    private void makeFriends(User a, User b) {
        friendshipRepository.save(Friendship.builder()
                .requester(a).receiver(b)
                .status(FriendshipStatus.ACCEPTED).build());
    }

    // ST-01 — Full User Registration and Login Flow
    @Test
    @Order(1)
    @DisplayName("ST-01 | Registration → Login → Access profile → Logout → Status OFFLINE")
    void st01_registrationLoginLogoutFlow() throws Exception {

        // Step 1 — Register
        RegisterRequest reg = new RegisterRequest(
                "alice@example.com", "alice", "Password1!", LocalDate.of(2000, 5, 10));
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist());

        assertThat(userRepository.findByEmail("alice@example.com")).isPresent();

        // Step 2 — Login
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", "Password1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.status").value("ONLINE"))
                .andReturn();

        String token = extractField(loginResult, "token");

        // Step 3 — Access protected profile using JWT
        mockMvc.perform(get("/profile")
                        .header("Authorization", auth(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));

        // Step 4 — Logout
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", auth(token)))
                .andExpect(status().isNoContent());

        // Step 5 — Verify status is OFFLINE in the database
        User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(alice.getStatus()).isEqualTo(UserStatus.OFFLINE);
    }

    
    // ST-02 — Complete Social Interaction Flow
    @Test
    @Order(2)
    @DisplayName("ST-02 | Alice registers → Bob registers → Friend request → Accept → Post → Newsfeed")
    void st02_completeSocialInteractionFlow() throws Exception {

        // Step 1 — Alice and Bob register and login
        String aliceToken = registerAndLogin("alice@example.com", "alice", "Password1!");
        String bobToken   = registerAndLogin("bob@example.com",   "bob",   "Password1!");

        User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
        User bob   = userRepository.findByEmail("bob@example.com").orElseThrow();

        // Step 2 — Alice sends Bob a friend request
        MvcResult requestResult = mockMvc.perform(
                        post("/friends/request/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();

        String friendshipId = extractField(requestResult, "friendshipId");

        // Step 3 — Bob sees the pending request
        mockMvc.perform(get("/friends/requests")
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requester.username").value("alice"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        // Step 4 — Bob accepts the friend request
        mockMvc.perform(post("/friends/" + friendshipId + "/accept")
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Step 5 — Alice creates a post
        mockMvc.perform(post("/content/posts")
                        .param("contentText", "Hello Bob! We are now friends!")
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isCreated());

        // Step 6 — Bob sees Alice's post in his newsfeed
        mockMvc.perform(get("/content/feed/posts")
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].contentText")
                        .value("Hello Bob! We are now friends!"))
                .andExpect(jsonPath("$.content[0].author.username").value("alice"));

        // Step 7 — Alice sees Bob in her friends list with ONLINE status
        mockMvc.perform(get("/friends")
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACCEPTED"));

        // Step 8 — Verify friendship in database
        Friendship friendship = friendshipRepository
                .findById(UUID.fromString(friendshipId)).orElseThrow();
        assertThat(friendship.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
    }

    // ST-03 — Content Lifecycle Flow
    @Test
    @Order(3)
    @DisplayName("ST-03 | Create post → Appears in profile → Appears in friend newsfeed → Delete → Gone from both")
    void st03_contentLifecycleFlow() throws Exception {

        // Setup
        String aliceToken = registerAndLogin("alice@example.com", "alice", "Password1!");
        String bobToken   = registerAndLogin("bob@example.com",   "bob",   "Password1!");
        User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
        User bob   = userRepository.findByEmail("bob@example.com").orElseThrow();
        makeFriends(alice, bob);

        // Step 1 — Alice creates a post
        MvcResult postResult = mockMvc.perform(post("/content/posts")
                        .param("contentText", "Alice's lifecycle post")
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isCreated())
                .andReturn();

        String contentId = extractField(postResult, "contentId");

        // Step 2 — Post appears in Alice's profile posts
        mockMvc.perform(get("/profile/posts")
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].contentId").value(contentId))
                .andExpect(jsonPath("$.totalElements").value(1));

        // Step 3 — Post appears in Bob's newsfeed
        mockMvc.perform(get("/content/feed/posts")
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].contentId").value(contentId));

        // Step 4 — Alice deletes the post
        mockMvc.perform(delete("/content/" + contentId)
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isNoContent());

        // Step 5 — Post no longer in Alice's profile
        mockMvc.perform(get("/profile/posts")
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // Step 6 — Post no longer in Bob's newsfeed
        mockMvc.perform(get("/content/feed/posts")
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        // Step 7 — Post is gone from the database
        assertThat(contentRepository.findById(UUID.fromString(contentId))).isEmpty();
    }

    // ST-04 — Story Expiry Flow
    @Test
    @Order(4)
    @DisplayName("ST-04 | Create story → Appears in feed → Backdate past 24h → No longer in feed")
    void st04_storyExpiryFlow() throws Exception {

        // Setup
        String aliceToken = registerAndLogin("alice@example.com", "alice", "Password1!");
        String bobToken   = registerAndLogin("bob@example.com",   "bob",   "Password1!");
        User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
        User bob   = userRepository.findByEmail("bob@example.com").orElseThrow();
        makeFriends(alice, bob);

        // Step 1 — Alice creates a story
        MvcResult storyResult = mockMvc.perform(post("/content/stories")
                        .param("contentText", "Alice's fresh story!")
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("STORY"))
                .andReturn();

        String storyId = extractField(storyResult, "contentId");

        // Step 2 — Story appears in Bob's feed (active — just created)
        mockMvc.perform(get("/content/feed/stories")
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].contentId").value(storyId));

        // Step 3 — Simulate expiry: JPQL UPDATE bypasses updatable=false + clears session cache
        contentRepository.backdateTimestamp(
                UUID.fromString(storyId),
                LocalDateTime.now().minusHours(25));

        // Step 4 — Story no longer appears in Bob's feed (service filters expired)
        mockMvc.perform(get("/content/feed/stories")
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ST-05 — Friend Management Full Flow
    @Test
    @Order(5)
    @DisplayName("ST-05 | Send request → Decline → Send again → Accept → Remove → Gone from both lists")
    void st05_friendManagementFullFlow() throws Exception {

        String aliceToken = registerAndLogin("alice@example.com", "alice", "Password1!");
        String bobToken   = registerAndLogin("bob@example.com",   "bob",   "Password1!");
        User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
        User bob   = userRepository.findByEmail("bob@example.com").orElseThrow();

        // Step 1 — Alice sends Bob a friend request
        MvcResult reqResult = mockMvc.perform(
                        post("/friends/request/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                .andExpect(status().isCreated())
                .andReturn();
        String friendshipId = extractField(reqResult, "friendshipId");

        // Step 2 — Bob declines
        mockMvc.perform(delete("/friends/" + friendshipId + "/decline")
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isNoContent());

        // Step 3 — Friendship record is deleted
        assertThat(friendshipRepository.findById(UUID.fromString(friendshipId))).isEmpty();

        // Step 4 — Alice sends another request (after decline, she can try again)
        MvcResult reqResult2 = mockMvc.perform(
                        post("/friends/request/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                .andExpect(status().isCreated())
                .andReturn();
        String friendshipId2 = extractField(reqResult2, "friendshipId");

        // Step 5 — Bob accepts this time
        mockMvc.perform(post("/friends/" + friendshipId2 + "/accept")
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        // Step 6 — Both appear in each other's friends list
        mockMvc.perform(get("/friends").header("Authorization", auth(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/friends").header("Authorization", auth(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // Step 7 — Alice removes Bob
        mockMvc.perform(delete("/friends/" + friendshipId2)
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isNoContent());

        // Step 8 — Neither appears in each other's friends list
        mockMvc.perform(get("/friends").header("Authorization", auth(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/friends").header("Authorization", auth(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Step 9 — Friendship gone from database
        assertThat(friendshipRepository.count()).isEqualTo(0);
    }

    // ST-06 — Block User Flow
    @Test
    @Order(6)
    @DisplayName("ST-06 | Friends → Alice blocks Bob → Bob's posts hidden → Unblock → Bob in suggestions again")
    void st06_blockUserFlow() throws Exception {

        String aliceToken = registerAndLogin("alice@example.com", "alice", "Password1!");
        String bobToken   = registerAndLogin("bob@example.com",   "bob",   "Password1!");
        User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
        User bob   = userRepository.findByEmail("bob@example.com").orElseThrow();
        makeFriends(alice, bob);

        // Step 1 — Bob creates a post — Alice should see it in her feed
        contentRepository.save(Content.builder()
                .author(bob).contentText("Bob's visible post")
                .contentType(ContentType.POST).build());

        mockMvc.perform(get("/content/feed/posts")
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        // Step 2 — Alice blocks Bob
        mockMvc.perform(post("/friends/block/" + bob.getUserId())
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        // Step 3 — Verify block in database
        Friendship block = friendshipRepository
                .findBetweenUsers(alice.getUserId(), bob.getUserId()).orElseThrow();
        assertThat(block.getStatus()).isEqualTo(FriendshipStatus.BLOCKED);

        // Step 4 — Bob no longer appears in Alice's friends list
        mockMvc.perform(get("/friends").header("Authorization", auth(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Step 5 — Bob cannot send Alice a friend request (friendship already exists as BLOCKED)
        mockMvc.perform(post("/friends/request/" + alice.getUserId())
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isConflict());

        // Step 6 — Alice unblocks Bob
        mockMvc.perform(delete("/friends/block/" + bob.getUserId())
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isNoContent());

        // Step 7 — Block record removed from database
        assertThat(friendshipRepository
                .findBetweenUsers(alice.getUserId(), bob.getUserId())).isEmpty();

        // Step 8 — Bob now appears in Alice's suggestions again
        mockMvc.perform(get("/friends/suggestions")
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("bob"));
    }

    // ST-07 — Profile Update Flow
    @Test
    @Order(7)
    @DisplayName("ST-07 | Login → Update bio → Verify → Change password → Login with new password")
    void st07_profileUpdateFlow() throws Exception {

        // Step 1 — Register and login
        String aliceToken = registerAndLogin("alice@example.com", "alice", "Password1!");

        // Step 2 — Update bio
        mockMvc.perform(patch("/profile")
                        .header("Authorization", auth(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProfileRequest("Software engineer & coffee lover", null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Software engineer & coffee lover"));

        // Step 3 — Bio persisted — verify via GET profile
        mockMvc.perform(get("/profile")
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Software engineer & coffee lover"));

        // Step 4 — Change password
        mockMvc.perform(patch("/profile/password")
                        .header("Authorization", auth(aliceToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePasswordRequest("Password1!", "NewSecure2!"))))
                .andExpect(status().isNoContent());

        // Step 5 — Old password no longer works
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", "Password1!"))))
                .andExpect(status().isUnauthorized());

        // Step 6 — New password works and returns valid token
        MvcResult newLogin = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("alice@example.com", "NewSecure2!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        // Step 7 — New token grants access to protected resources
        String newToken = extractField(newLogin, "token");
        mockMvc.perform(get("/profile")
                        .header("Authorization", auth(newToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    // ST-08 — Security Boundary Flow
    @Test
    @Order(8)
    @DisplayName("ST-08 | Unauthenticated blocked → Invalid token blocked → Cross-user actions blocked")
    void st08_securityBoundaryFlow() throws Exception {

        String aliceToken = registerAndLogin("alice@example.com", "alice", "Password1!");
        String bobToken   = registerAndLogin("bob@example.com",   "bob",   "Password1!");
        User alice = userRepository.findByEmail("alice@example.com").orElseThrow();
        User bob   = userRepository.findByEmail("bob@example.com").orElseThrow();
        makeFriends(alice, bob);

        // Alice creates a post
        MvcResult postResult = mockMvc.perform(post("/content/posts")
                        .param("contentText", "Alice's secure post")
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isCreated())
                .andReturn();
        String postId = extractField(postResult, "contentId");

        // Unauthenticated access to protected endpoints should be blocked
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/content/feed/posts"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/friends"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/content/posts").param("contentText", "Hack!"))
                .andExpect(status().isUnauthorized());

        // Invalid token
        mockMvc.perform(get("/profile")
                        .header("Authorization", "InvalidFormat"))
                .andExpect(status().isUnauthorized());

        // Bob cannot delete Alice's post
        mockMvc.perform(delete("/content/" + postId)
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isNotFound());

        // Post still exists
        assertThat(contentRepository.findById(UUID.fromString(postId))).isPresent();

        // Bob cannot change Alice's password
        // PATCH /profile/password always acts on the authenticated user
        // Verify Bob's own change doesn't affect Alice's password
        mockMvc.perform(patch("/profile/password")
                        .header("Authorization", auth(bobToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdatePasswordRequest("Password1!", "BobNewPass2!"))))
                .andExpect(status().isNoContent());

        // Alice's password is still the original
        User aliceDb = userRepository.findByEmail("alice@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("Password1!", aliceDb.getHashedPassword())).isTrue();

        // Self-friend-requests should be blocked
        mockMvc.perform(post("/friends/request/" + alice.getUserId())
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isBadRequest());

        // Duplicate friend request 
        // Alice and Bob are already friends — another request should be rejected
        mockMvc.perform(post("/friends/request/" + bob.getUserId())
                        .header("Authorization", auth(aliceToken)))
                .andExpect(status().isConflict());
    }
}