package org.connecthub.backend.Integration;

import org.connecthub.backend.enums.FriendshipStatus;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.model.Friendship;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for FriendshipController
 * Full stack — real H2 database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("FriendshipController Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FriendshipControllerIntegrationTest {

    @Autowired private MockMvc              mockMvc;
    @Autowired private ObjectMapper         objectMapper;
    @Autowired private JwtUtil              jwtUtil;
    @Autowired private UserRepository       userRepository;
    @Autowired private ContentRepository    contentRepository;
    @Autowired private FriendshipRepository friendshipRepository;
    @Autowired private PasswordEncoder      passwordEncoder;

    private User   alice;
    private User   bob;
    private User   carol;
    private String aliceToken;
    private String bobToken;
    private String carolToken;

    @BeforeEach
    void setUp() {
        friendshipRepository.deleteAll();
        contentRepository.deleteAll();
        userRepository.deleteAll();

        alice = createUser("alice@example.com", "alice");
        bob   = createUser("bob@example.com",   "bob");
        carol = createUser("carol@example.com", "carol");

        aliceToken = jwtUtil.generateToken(alice.getEmail());
        bobToken   = jwtUtil.generateToken(bob.getEmail());
        carolToken = jwtUtil.generateToken(carol.getEmail());
    }

    private User createUser(String email, String username) {
        return userRepository.save(User.builder()
                .email(email).username(username)
                .hashedPassword(passwordEncoder.encode("Password1!"))
                .dateOfBirth(LocalDate.of(1998, 1, 1))
                .status(UserStatus.ONLINE).build());
    }

    private void makeFriends(User a, User b) {
        friendshipRepository.save(Friendship.builder()
                .requester(a).receiver(b)
                .status(FriendshipStatus.ACCEPTED).build());
    }

    private Friendship makePending(User from, User to) {
        return friendshipRepository.save(Friendship.builder()
                .requester(from).receiver(to)
                .status(FriendshipStatus.PENDING).build());
    }

    private String auth(String token) { return "Bearer " + token; }

    @Nested
    @DisplayName("POST /friends/request/{targetId}")
    class SendFriendRequestTests {

        @Test
        @DisplayName("TC-FC-01 | Send friend request — 201 Created, PENDING status")
        void sendRequest_valid_returns201WithPending() throws Exception {
                mockMvc.perform(post("/friends/request/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andExpect(jsonPath("$.requester.username").value("alice"))
                        .andExpect(jsonPath("$.receiver.username").value("bob"))
                        .andExpect(jsonPath("$.friendshipId").exists());

                assertThat(friendshipRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-FC-02 | Send request to self — 400 Bad Request")
        void sendRequest_toSelf_returns400() throws Exception {
                mockMvc.perform(post("/friends/request/" + alice.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isBadRequest());

                assertThat(friendshipRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-FC-03 | Send request to already-connected user — 409 Conflict")
        void sendRequest_alreadyFriends_returns409() throws Exception {
                makeFriends(alice, bob);

                mockMvc.perform(post("/friends/request/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("TC-FC-04 | Send request to non-existent user — 404 Not Found")
        void sendRequest_unknownUser_returns404() throws Exception {
                mockMvc.perform(post("/friends/request/00000000-0000-0000-0000-000000000000")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("TC-FC-05 | Send request without token — 401 Unauthorized")
        void sendRequest_noToken_returns401() throws Exception {
                mockMvc.perform(post("/friends/request/" + bob.getUserId()))
                        .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /friends/{friendshipId}/accept")
    class AcceptFriendRequestTests {
        @Test
        @DisplayName("TC-FC-06 | Accept request — 200 OK, status changes to ACCEPTED")
        void acceptRequest_valid_returns200WithAccepted() throws Exception {
                Friendship pending = makePending(alice, bob);

                mockMvc.perform(post("/friends/" + pending.getFriendshipId() + "/accept")
                                .header("Authorization", auth(bobToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("ACCEPTED"));

                Friendship updated = friendshipRepository
                        .findById(pending.getFriendshipId()).orElseThrow();
                assertThat(updated.getStatus()).isEqualTo(FriendshipStatus.ACCEPTED);
        }

        @Test
        @DisplayName("TC-FC-07 | Accept request as wrong user (not receiver) — 404 Not Found")
        void acceptRequest_wrongUser_returns404() throws Exception {
                Friendship pending = makePending(alice, bob);

                // Carol tries to accept Alice's request to Bob
                mockMvc.perform(post("/friends/" + pending.getFriendshipId() + "/accept")
                                .header("Authorization", auth(carolToken)))
                        .andExpect(status().isNotFound());

                // Status unchanged
                Friendship unchanged = friendshipRepository
                        .findById(pending.getFriendshipId()).orElseThrow();
                assertThat(unchanged.getStatus()).isEqualTo(FriendshipStatus.PENDING);
        }

        @Test
        @DisplayName("TC-FC-08 | Accept non-existent friendship — 404 Not Found")
        void acceptRequest_notFound_returns404() throws Exception {
                mockMvc.perform(post("/friends/00000000-0000-0000-0000-000000000000/accept")
                                .header("Authorization", auth(bobToken)))
                        .andExpect(status().isNotFound());
        }
	}

    @Nested
    @DisplayName("DELETE /friends/{friendshipId}/decline")
    class DeclineFriendRequestTests {
        @Test
        @DisplayName("TC-FC-09 | Decline request — 204 No Content, friendship deleted")
        void declineRequest_valid_returns204AndDeleted() throws Exception {
            Friendship pending = makePending(alice, bob);

        mockMvc.perform(delete("/friends/" + pending.getFriendshipId() + "/decline")
                        .header("Authorization", auth(bobToken)))
                .andExpect(status().isNoContent());

        assertThat(friendshipRepository.findById(pending.getFriendshipId())).isEmpty();
        }

        @Test
        @DisplayName("TC-FC-10 | Decline as wrong user — 404 Not Found, not deleted")
        void declineRequest_wrongUser_returns404() throws Exception {
                Friendship pending = makePending(alice, bob);

                mockMvc.perform(delete("/friends/" + pending.getFriendshipId() + "/decline")
                                .header("Authorization", auth(carolToken)))
                        .andExpect(status().isNotFound());

                assertThat(friendshipRepository.findById(pending.getFriendshipId())).isPresent();
        }
	}

    @Nested
    @DisplayName("DELETE /friends/{friendshipId}")
    class RemoveFriendTests {
        @Test
        @DisplayName("TC-FC-11 | Remove friend as requester — 204 No Content, deleted")
        void removeFriend_byRequester_returns204() throws Exception {
                makeFriends(alice, bob);
                Friendship f = friendshipRepository
                        .findBetweenUsers(alice.getUserId(), bob.getUserId()).orElseThrow();

                mockMvc.perform(delete("/friends/" + f.getFriendshipId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isNoContent());

                assertThat(friendshipRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-FC-12 | Remove friend as receiver — also allowed, 204 No Content")
        void removeFriend_byReceiver_returns204() throws Exception {
                makeFriends(alice, bob);
                Friendship f = friendshipRepository
                        .findBetweenUsers(alice.getUserId(), bob.getUserId()).orElseThrow();

                mockMvc.perform(delete("/friends/" + f.getFriendshipId())
                                .header("Authorization", auth(bobToken)))
                        .andExpect(status().isNoContent());

                assertThat(friendshipRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-FC-13 | Remove friendship as non-participant — 404 Not Found")
        void removeFriend_nonParticipant_returns404() throws Exception {
                makeFriends(alice, bob);
                Friendship f = friendshipRepository
                        .findBetweenUsers(alice.getUserId(), bob.getUserId()).orElseThrow();

                // Carol is not part of this friendship
                mockMvc.perform(delete("/friends/" + f.getFriendshipId())
                                .header("Authorization", auth(carolToken)))
                        .andExpect(status().isNotFound());

                assertThat(friendshipRepository.count()).isEqualTo(1);
        }
	}

    @Nested
    @DisplayName("POST /friends/block/{targetId}")
    class BlockUserTests {
        @Test
        @DisplayName("TC-FC-14 | Block user — 201 Created, status is BLOCKED")
        void blockUser_valid_returns201WithBlocked() throws Exception {
                mockMvc.perform(post("/friends/block/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("BLOCKED"))
                        .andExpect(jsonPath("$.requester.username").value("alice"));

                Friendship block = friendshipRepository
                        .findBetweenUsers(alice.getUserId(), bob.getUserId()).orElseThrow();
                assertThat(block.getStatus()).isEqualTo(FriendshipStatus.BLOCKED);
        }

        @Test
        @DisplayName("TC-FC-15 | Block existing friend — removes friendship then creates block")
        void blockUser_existingFriend_removesAndBlocks() throws Exception {
                makeFriends(alice, bob);
                assertThat(friendshipRepository.count()).isEqualTo(1);

                mockMvc.perform(post("/friends/block/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.status").value("BLOCKED"));

                // Still only one record — friendship replaced by block
                assertThat(friendshipRepository.count()).isEqualTo(1);
                Friendship block = friendshipRepository
                        .findBetweenUsers(alice.getUserId(), bob.getUserId()).orElseThrow();
                assertThat(block.getStatus()).isEqualTo(FriendshipStatus.BLOCKED);
        }

        @Test
        @DisplayName("TC-FC-16 | Block without token — 401 Unauthorized")
        void blockUser_noToken_returns401() throws Exception {
                mockMvc.perform(post("/friends/block/" + bob.getUserId()))
                        .andExpect(status().isUnauthorized());
        }
	}

    @Nested
    @DisplayName("DELETE /friends/block/{targetId}")
    class UnblockUserTests {
        @Test
        @DisplayName("TC-FC-17 | Unblock user — 204 No Content, block record deleted")
        void unblockUser_valid_returns204AndDeleted() throws Exception {
                friendshipRepository.save(Friendship.builder()
                        .requester(alice).receiver(bob)
                        .status(FriendshipStatus.BLOCKED).build());

                mockMvc.perform(delete("/friends/block/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isNoContent());

                assertThat(friendshipRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-FC-18 | Unblock user not blocked — 204 No Content (idempotent)")
        void unblockUser_notBlocked_returns204() throws Exception {
                // No block exists — should still return 204 without error
                mockMvc.perform(delete("/friends/block/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isNoContent());
        }
	}

    @Nested
    @DisplayName("GET /friends/requests")
    class GetPendingRequestsTests {
        @Test
        @DisplayName("TC-FC-19 | Get pending requests — 200, returns list of pending requests")
        void getPendingRequests_withRequests_returnsList() throws Exception {
                makePending(alice, bob);
                makePending(carol, bob);

                mockMvc.perform(get("/friends/requests")
                                .header("Authorization", auth(bobToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(2))
                        .andExpect(jsonPath("$[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("TC-FC-20 | Get pending requests with none — 200, empty list")
        void getPendingRequests_none_returnsEmptyList() throws Exception {
                mockMvc.perform(get("/friends/requests")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("TC-FC-21 | Get pending requests without token — 401 Unauthorized")
        void getPendingRequests_noToken_returns401() throws Exception {
                mockMvc.perform(get("/friends/requests"))
                        .andExpect(status().isUnauthorized());
        }
    }
    
    @Nested
    @DisplayName("GET /friends")
    class GetFriendsTests {
        @Test
        @DisplayName("TC-FC-22 | Get friends list — 200, returns FriendshipDto list")
        void getFriends_withFriends_returnsList() throws Exception {
                makeFriends(alice, bob);
                makeFriends(alice, carol);

                mockMvc.perform(get("/friends")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(2))
                        .andExpect(jsonPath("$[0].friendshipId").exists())
                        .andExpect(jsonPath("$[0].status").value("ACCEPTED"));
        }

        @Test
        @DisplayName("TC-FC-23 | Get friends list with no friends — 200, empty list")
        void getFriends_noFriends_returnsEmptyList() throws Exception {
                mockMvc.perform(get("/friends")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("TC-FC-24 | Pending requests do not appear in friends list")
        void getFriends_pendingNotIncluded() throws Exception {
                makePending(alice, bob);

                mockMvc.perform(get("/friends")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").isEmpty());
        }
	}

    @Nested
    @DisplayName("GET /friends/suggestions")
    class GetFriendSuggestionsTests {
        @Test
        @DisplayName("TC-FC-25 | Get suggestions — 200, returns users not yet connected")
        void getSuggestions_withUnconnectedUsers_returnsThem() throws Exception {
                // Alice has no connections — bob and carol should appear as suggestions
                mockMvc.perform(get("/friends/suggestions")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("TC-FC-26 | Suggestions excludes existing friends")
        void getSuggestions_excludesAlreadyFriends() throws Exception {
                makeFriends(alice, bob);

                // Bob is already a friend — only Carol should appear
                mockMvc.perform(get("/friends/suggestions")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(1))
                        .andExpect(jsonPath("$[0].username").value("carol"));
        }

        @Test
        @DisplayName("TC-FC-27 | Suggestions excludes pending request users")
        void getSuggestions_excludesPendingRequests() throws Exception {
                makePending(alice, bob);

                // Bob has a pending request from Alice — should not appear in suggestions
                mockMvc.perform(get("/friends/suggestions")
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(1))
                        .andExpect(jsonPath("$[0].username").value("carol"));
        }
	}

    @Nested
    @DisplayName("GET /friends/status/{otherUserId}")
    class GetFriendshipStatusTests {
        @Test
        @DisplayName("TC-FC-28 | Get status — NONE when no relationship exists")
        void getStatus_noRelationship_returnsNone() throws Exception {
                mockMvc.perform(get("/friends/status/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("NONE"));
        }

        @Test
        @DisplayName("TC-FC-29 | Get status — PENDING after request sent")
        void getStatus_pendingRequest_returnsPending() throws Exception {
                makePending(alice, bob);

                mockMvc.perform(get("/friends/status/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("PENDING"));
        }

        @Test
        @DisplayName("TC-FC-30 | Get status — ACCEPTED after friendship established")
        void getStatus_accepted_returnsAccepted() throws Exception {
                makeFriends(alice, bob);

                mockMvc.perform(get("/friends/status/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }

        @Test
        @DisplayName("TC-FC-31 | Get status — BLOCKED after blocking")
        void getStatus_blocked_returnsBlocked() throws Exception {
                friendshipRepository.save(Friendship.builder()
                        .requester(alice).receiver(bob)
                        .status(FriendshipStatus.BLOCKED).build());

                mockMvc.perform(get("/friends/status/" + bob.getUserId())
                                .header("Authorization", auth(aliceToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("BLOCKED"));
        }

        @Test
        @DisplayName("TC-FC-32 | Get status without token — 401 Unauthorized")
        void getStatus_noToken_returns401() throws Exception {
                mockMvc.perform(get("/friends/status/" + bob.getUserId()))
                        .andExpect(status().isUnauthorized());
        }
	}
}