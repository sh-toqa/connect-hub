package org.connecthub.backend.service;

import org.connecthub.backend.dto.response.FriendshipDto;
import org.connecthub.backend.enums.FriendshipStatus;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.exception.FriendshipConflictException;
import org.connecthub.backend.exception.ResourceNotFoundException;
import org.connecthub.backend.mapper.UserMapper;
import org.connecthub.backend.model.Friendship;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.FriendshipRepository;
import org.connecthub.backend.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendshipService Unit Tests")
class FriendshipServiceTest {

    @Mock private FriendshipRepository friendshipRepository;
    @Mock private UserRepository       userRepository;

    private final UserMapper userMapper = new UserMapper();

    @InjectMocks
    private FriendshipService friendshipService;

    private UUID   aliceId, bobId;
    private User   alice, bob;

    @BeforeEach
    void setUp() {
        friendshipService = new FriendshipService(friendshipRepository, userRepository, userMapper);

        aliceId = UUID.randomUUID();
        bobId   = UUID.randomUUID();

        alice = User.builder().userId(aliceId).username("alice")
                .email("alice@example.com").status(UserStatus.ONLINE).build();
        bob   = User.builder().userId(bobId).username("bob")
                .email("bob@example.com").status(UserStatus.OFFLINE).build();
    }

    @Nested 
    @DisplayName("sendRequest()")
    class SendRequest {

        @Test @DisplayName("Happy path — creates PENDING friendship")
        void sendRequest_valid_createsPendingFriendship() {
            when(userRepository.findById(aliceId)).thenReturn(Optional.of(alice));
            when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));
            when(friendshipRepository.findBetweenUsers(aliceId, bobId)).thenReturn(Optional.empty());
            when(friendshipRepository.save(any())).thenAnswer(inv -> {
                Friendship f = inv.getArgument(0);
                f.setFriendshipId(UUID.randomUUID());
                return f;
            });

            FriendshipDto result = friendshipService.sendRequest(aliceId, bobId);

            assertThat(result.status()).isEqualTo(FriendshipStatus.PENDING);
            assertThat(result.requester().username()).isEqualTo("alice");
            assertThat(result.receiver().username()).isEqualTo("bob");
            verify(friendshipRepository).save(any(Friendship.class));
        }

        @Test 
        @DisplayName("Self-request — throws IllegalArgumentException")
        void sendRequest_toSelf_throwsException() {
            assertThatThrownBy(() -> friendshipService.sendRequest(aliceId, aliceId))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(friendshipRepository, never()).save(any());
        }

        @Test 
        @DisplayName("Already friends — throws FriendshipConflictException")
        void sendRequest_alreadyExists_throwsException() {
            when(userRepository.findById(aliceId)).thenReturn(Optional.of(alice));
            when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));
            Friendship existing = Friendship.builder()
                    .requester(alice).receiver(bob)
                    .status(FriendshipStatus.ACCEPTED).build();
            when(friendshipRepository.findBetweenUsers(aliceId, bobId))
                    .thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> friendshipService.sendRequest(aliceId, bobId))
                    .isInstanceOf(FriendshipConflictException.class);
            verify(friendshipRepository, never()).save(any());
        }

        @Test 
        @DisplayName("User not found — throws ResourceNotFoundException")
        void sendRequest_userNotFound_throwsException() {
            when(userRepository.findById(aliceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendshipService.sendRequest(aliceId, bobId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested 
    @DisplayName("acceptRequest()")
    class AcceptRequest {

        @Test @DisplayName("Happy path — status changes to ACCEPTED")
        void acceptRequest_valid_statusAccepted() {
            UUID fid = UUID.randomUUID();
            Friendship f = Friendship.builder()
                    .friendshipId(fid).requester(alice).receiver(bob)
                    .status(FriendshipStatus.PENDING).build();

            when(friendshipRepository.findById(fid)).thenReturn(Optional.of(f));
            when(friendshipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FriendshipDto result = friendshipService.acceptRequest(fid, bobId);

            assertThat(result.status()).isEqualTo(FriendshipStatus.ACCEPTED);
        }

        @Test 
        @DisplayName("Wrong receiver — throws ResourceNotFoundException")
        void acceptRequest_wrongReceiver_throwsException() {
            UUID fid   = UUID.randomUUID();
            UUID carol = UUID.randomUUID();
            Friendship f = Friendship.builder()
                    .friendshipId(fid).requester(alice).receiver(bob)
                    .status(FriendshipStatus.PENDING).build();

            when(friendshipRepository.findById(fid)).thenReturn(Optional.of(f));

            // carol is not the receiver — should be rejected
            assertThatThrownBy(() -> friendshipService.acceptRequest(fid, carol))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(friendshipRepository, never()).save(any());
        }

        @Test 
        @DisplayName("Friendship not found — throws ResourceNotFoundException")
        void acceptRequest_notFound_throwsException() {
            when(friendshipRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> friendshipService.acceptRequest(UUID.randomUUID(), bobId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested 
    @DisplayName("declineRequest()")
    class DeclineRequest {

        @Test @DisplayName("Happy path — friendship record deleted")
        void declineRequest_valid_deleted() {
            UUID fid = UUID.randomUUID();
            Friendship f = Friendship.builder()
                    .friendshipId(fid).requester(alice).receiver(bob)
                    .status(FriendshipStatus.PENDING).build();

            when(friendshipRepository.findById(fid)).thenReturn(Optional.of(f));

            friendshipService.declineRequest(fid, bobId);

            verify(friendshipRepository).delete(f);
        }

        @Test 
        @DisplayName("Wrong receiver — throws ResourceNotFoundException")
        void declineRequest_wrongReceiver_throwsException() {
            UUID fid   = UUID.randomUUID();
            UUID carol = UUID.randomUUID();
            Friendship f = Friendship.builder()
                    .friendshipId(fid).requester(alice).receiver(bob)
                    .status(FriendshipStatus.PENDING).build();

            when(friendshipRepository.findById(fid)).thenReturn(Optional.of(f));

            assertThatThrownBy(() -> friendshipService.declineRequest(fid, carol))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(friendshipRepository, never()).delete(any());
        }
    }

    @Nested 
    @DisplayName("removeFriend()")
    class RemoveFriend {

        @Test @DisplayName("Requester removes friend — deleted")
        void removeFriend_byRequester_deleted() {
            UUID fid = UUID.randomUUID();
            Friendship f = Friendship.builder()
                    .friendshipId(fid).requester(alice).receiver(bob)
                    .status(FriendshipStatus.ACCEPTED).build();

            when(friendshipRepository.findById(fid)).thenReturn(Optional.of(f));

            friendshipService.removeFriend(fid, aliceId);
            verify(friendshipRepository).delete(f);
        }

        @Test 
        @DisplayName("Receiver removes friend — also allowed")
        void removeFriend_byReceiver_deleted() {
            UUID fid = UUID.randomUUID();
            Friendship f = Friendship.builder()
                    .friendshipId(fid).requester(alice).receiver(bob)
                    .status(FriendshipStatus.ACCEPTED).build();

            when(friendshipRepository.findById(fid)).thenReturn(Optional.of(f));

            friendshipService.removeFriend(fid, bobId);
            verify(friendshipRepository).delete(f);
        }

        @Test 
        @DisplayName("Non-participant — throws ResourceNotFoundException")
        void removeFriend_nonParticipant_throwsException() {
            UUID fid   = UUID.randomUUID();
            UUID carol = UUID.randomUUID();
            Friendship f = Friendship.builder()
                    .friendshipId(fid).requester(alice).receiver(bob)
                    .status(FriendshipStatus.ACCEPTED).build();

            when(friendshipRepository.findById(fid)).thenReturn(Optional.of(f));

            assertThatThrownBy(() -> friendshipService.removeFriend(fid, carol))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(friendshipRepository, never()).delete(any());
        }
    }

    @Nested 
    @DisplayName("blockUser()")
    class BlockUser {

        @Test @DisplayName("No prior friendship — creates BLOCKED record")
        void blockUser_noPrior_createsBlockedRecord() {
            when(userRepository.findById(aliceId)).thenReturn(Optional.of(alice));
            when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));
            when(friendshipRepository.findBetweenUsers(aliceId, bobId)).thenReturn(Optional.empty());
            when(friendshipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FriendshipDto result = friendshipService.blockUser(aliceId, bobId);

            assertThat(result.status()).isEqualTo(FriendshipStatus.BLOCKED);
        }

        @Test 
        @DisplayName("Existing friendship — removed first, then blocked")
        void blockUser_existingFriendship_removedAndBlocked() {
            Friendship existing = Friendship.builder()
                    .friendshipId(UUID.randomUUID()).requester(alice).receiver(bob)
                    .status(FriendshipStatus.ACCEPTED).build();

            when(userRepository.findById(aliceId)).thenReturn(Optional.of(alice));
            when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));
            when(friendshipRepository.findBetweenUsers(aliceId, bobId))
                    .thenReturn(Optional.of(existing));
            when(friendshipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            friendshipService.blockUser(aliceId, bobId);

            verify(friendshipRepository).delete(existing);   // old friendship removed
            verify(friendshipRepository).save(argThat(f ->
                    f.getStatus() == FriendshipStatus.BLOCKED)); // new block created
        }
    }

    @Nested 
    @DisplayName("getPendingRequests()")
    class GetPendingRequests {

        @Test 
        @DisplayName("Returns pending requests for receiver")
        void getPendingRequests_returnsList() {
            Friendship pending = Friendship.builder()
                    .friendshipId(UUID.randomUUID()).requester(alice).receiver(bob)
                    .status(FriendshipStatus.PENDING).build();

            when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));
            when(friendshipRepository.findByReceiverAndStatus(bob, FriendshipStatus.PENDING))
                    .thenReturn(List.of(pending));

            List<FriendshipDto> result = friendshipService.getPendingRequests(bobId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).requester().username()).isEqualTo("alice");
        }

        @Test 
        @DisplayName("No pending requests — returns empty list")
        void getPendingRequests_empty_returnsEmptyList() {
            when(userRepository.findById(bobId)).thenReturn(Optional.of(bob));
            when(friendshipRepository.findByReceiverAndStatus(bob, FriendshipStatus.PENDING))
                    .thenReturn(List.of());

            List<FriendshipDto> result = friendshipService.getPendingRequests(bobId);

            assertThat(result).isEmpty();
        }
    }

    @Nested 
    @DisplayName("getFriends()")
    class GetFriends {

        @Test 
        @DisplayName("Returns FriendshipDto list with both users")
        void getFriends_returnsFriendshipDtoList() {
            Friendship f = Friendship.builder()
                    .friendshipId(UUID.randomUUID())
                    .requester(alice).receiver(bob)
                    .status(FriendshipStatus.ACCEPTED).build();

            when(friendshipRepository.findAcceptedFriendships(aliceId))
                    .thenReturn(List.of(f));

            List<FriendshipDto> result = friendshipService.getFriends(aliceId);

            assertThat(result).hasSize(1);
            // Both sides returned — consumer determines who is "the friend"
            assertThat(result.get(0).requester().username()).isEqualTo("alice");
            assertThat(result.get(0).receiver().username()).isEqualTo("bob");
            assertThat(result.get(0).status()).isEqualTo(FriendshipStatus.ACCEPTED);
            assertThat(result.get(0).friendshipId()).isNotNull();
        }

        @Test 
        @DisplayName("No friends — returns empty list")
        void getFriends_noFriends_returnsEmptyList() {
            when(friendshipRepository.findAcceptedFriendships(aliceId))
                    .thenReturn(List.of());

            List<FriendshipDto> result = friendshipService.getFriends(aliceId);

            assertThat(result).isEmpty();
        }
    }

    @Nested 
    @DisplayName("getStatus()")
    class GetStatus {

        @Test 
        @DisplayName("Returns ACCEPTED for existing friendship")
        void getStatus_accepted_returnsAccepted() {
            Friendship f = Friendship.builder()
                    .requester(alice).receiver(bob)
                    .status(FriendshipStatus.ACCEPTED).build();
            when(friendshipRepository.findBetweenUsers(aliceId, bobId))
                    .thenReturn(Optional.of(f));

            assertThat(friendshipService.getStatus(aliceId, bobId)).isEqualTo("ACCEPTED");
        }

        @Test 
        @DisplayName("Returns NONE when no relationship exists")
        void getStatus_noRelationship_returnsNone() {
            when(friendshipRepository.findBetweenUsers(aliceId, bobId))
                    .thenReturn(Optional.empty());

            assertThat(friendshipService.getStatus(aliceId, bobId)).isEqualTo("NONE");
        }

        @Test 
        @DisplayName("Returns PENDING for pending request")
        void getStatus_pending_returnsPending() {
            Friendship f = Friendship.builder()
                    .requester(alice).receiver(bob)
                    .status(FriendshipStatus.PENDING).build();
            when(friendshipRepository.findBetweenUsers(aliceId, bobId))
                    .thenReturn(Optional.of(f));

            assertThat(friendshipService.getStatus(aliceId, bobId)).isEqualTo("PENDING");
        }
    }
}
