package org.connecthub.backend.service;

import org.connecthub.backend.dto.request.UpdatePasswordRequest;
import org.connecthub.backend.dto.request.UpdateProfileRequest;
import org.connecthub.backend.dto.response.ContentDto;
import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.enums.ContentType;
import org.connecthub.backend.enums.FriendshipStatus;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.exception.InvalidPasswordException;
import org.connecthub.backend.exception.ResourceNotFoundException;
import org.connecthub.backend.mapper.UserMapper;
import org.connecthub.backend.model.Content;
import org.connecthub.backend.model.Friendship;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.ContentRepository;
import org.connecthub.backend.repository.FriendshipRepository;
import org.connecthub.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProfileService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProfileService Unit Tests")
class ProfileServiceTest {

    @Mock private UserRepository       userRepository;
    @Mock private ContentRepository    contentRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private StorageService       storageService;
    @Mock private PasswordEncoder      passwordEncoder;

    private final UserMapper userMapper = new UserMapper();

    @InjectMocks
    private ProfileService profileService;

    private UUID  userId;
    private User  alice;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                userRepository, contentRepository, friendshipRepository,
                storageService, passwordEncoder, userMapper);

        userId = UUID.randomUUID();
        alice = User.builder()
                .userId(userId)
                .email("alice@example.com")
                .username("alice")
                .hashedPassword("$2a$12$hashedpassword")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .status(UserStatus.ONLINE)
                .bio("Hello!")
                .profilePhotoPath("/uploads/profiles/old.jpg")
                .coverPhotoPath("/uploads/covers/old-cover.jpg")
                .build();
    }

    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("Happy path — returns UserDto for existing user")
        void getProfile_existingUser_returnsDto() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));

            UserDto result = profileService.getProfile(userId);

            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.email()).isEqualTo("alice@example.com");
            assertThat(result.username()).isEqualTo("alice");
            // Password must NEVER appear in the DTO
            assertThat(result).extracting(Object::toString).asString()
                    .doesNotContain("hashedpassword");
        }

        @Test
        @DisplayName("User not found — throws ResourceNotFoundException")
        void getProfile_unknownUser_throwsNotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getProfile(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("Happy path — bio is updated and persisted")
        void updateProfile_newBio_savedAndReturned() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest req = new UpdateProfileRequest("New bio text", null, null);
            UserDto result = profileService.updateProfile(userId, req);

            assertThat(result.bio()).isEqualTo("New bio text");
            verify(userRepository).save(alice);
        }

        @Test
        @DisplayName("Null bio — existing bio is preserved")
        void updateProfile_nullBio_existingBioPreserved() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UpdateProfileRequest req = new UpdateProfileRequest(null, null, null);
            profileService.updateProfile(userId, req);

            // Bio should remain unchanged
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getBio()).isEqualTo("Hello!");
        }

        @Test
        @DisplayName("User not found — throws ResourceNotFoundException")
        void updateProfile_unknownUser_throwsNotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.updateProfile(
                    UUID.randomUUID(),
                    new UpdateProfileRequest("bio", null, null)))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateProfilePhoto()")
    class UpdateProfilePhoto {

        @Test
        @DisplayName("Happy path — old photo deleted, new path saved")
        void updateProfilePhoto_validFile_oldDeletedNewSaved() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));
            when(storageService.store(any(), eq("profiles")))
                    .thenReturn("/uploads/profiles/new-uuid.jpg");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", new byte[100]);

            UserDto result = profileService.updateProfilePhoto(userId, file);

            // Old photo must be deleted first
            verify(storageService).delete("/uploads/profiles/old.jpg");
            // New path must be stored
            verify(storageService).store(file, "profiles");
            assertThat(result.profilePhotoPath()).isEqualTo("/uploads/profiles/new-uuid.jpg");
        }

        @Test
        @DisplayName("User not found — throws ResourceNotFoundException")
        void updateProfilePhoto_unknownUser_throwsNotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", "image/jpeg", new byte[10]);

            assertThatThrownBy(() -> profileService.updateProfilePhoto(UUID.randomUUID(), file))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(storageService, never()).store(any(), any());
        }
    }

    @Nested
    @DisplayName("updateCoverPhoto()")
    class UpdateCoverPhoto {

        @Test
        @DisplayName("Happy path — old cover deleted, new path saved")
        void updateCoverPhoto_validFile_oldDeletedNewSaved() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));
            when(storageService.store(any(), eq("covers")))
                    .thenReturn("/uploads/covers/new-cover.jpg");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "cover.jpg", "image/jpeg", new byte[200]);

            UserDto result = profileService.updateCoverPhoto(userId, file);

            verify(storageService).delete("/uploads/covers/old-cover.jpg");
            verify(storageService).store(file, "covers");
            assertThat(result.coverPhotoPath()).isEqualTo("/uploads/covers/new-cover.jpg");
        }
    }

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePassword {

        @Test
        @DisplayName("Happy path — password re-hashed and saved")
        void updatePassword_correctCurrentPassword_hashesAndSaves() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));
            when(passwordEncoder.matches("OldPass1!", "$2a$12$hashedpassword")).thenReturn(true);
            when(passwordEncoder.encode("NewPass2!")).thenReturn("$2a$12$newhash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            profileService.updatePassword(userId,
                    new UpdatePasswordRequest("OldPass1!", "NewPass2!"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            // New hash must be stored — never the plain text
            assertThat(captor.getValue().getHashedPassword()).isEqualTo("$2a$12$newhash");
            assertThat(captor.getValue().getHashedPassword()).doesNotContain("NewPass2!");
        }

        @Test
        @DisplayName("Wrong current password — throws InvalidPasswordException")
        void updatePassword_wrongCurrentPassword_throwsException() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));
            when(passwordEncoder.matches("WrongPass!", "$2a$12$hashedpassword")).thenReturn(false);

            assertThatThrownBy(() -> profileService.updatePassword(userId,
                    new UpdatePasswordRequest("WrongPass!", "NewPass2!")))
                    .isInstanceOf(InvalidPasswordException.class);

            // Nothing should be saved when the current password is wrong
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("User not found — throws ResourceNotFoundException")
        void updatePassword_unknownUser_throwsNotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.updatePassword(
                    UUID.randomUUID(),
                    new UpdatePasswordRequest("any", "newpass")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getOwnPosts()")
    class GetOwnPosts {

        @Test
        @DisplayName("Happy path — returns paginated ContentDto list")
        void getOwnPosts_existingUser_returnsMappedPage() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));

            Content post = Content.builder()
                    .contentId(UUID.randomUUID())
                    .author(alice)
                    .contentText("Hello world!")
                    .contentType(ContentType.POST)
                    .timestamp(LocalDateTime.now())
                    .build();

            Page<Content> page = new PageImpl<>(List.of(post));
            when(contentRepository.findByAuthor_UserIdAndContentTypeOrderByTimestampDesc(
                    eq(userId), eq(ContentType.POST), any(Pageable.class)))
                    .thenReturn(page);

            Page<ContentDto> result = profileService.getOwnPosts(userId, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).contentText()).isEqualTo("Hello world!");
            assertThat(result.getContent().get(0).author().userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("Empty posts — returns empty page, no exception")
        void getOwnPosts_noPosts_returnsEmptyPage() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));
            when(contentRepository.findByAuthor_UserIdAndContentTypeOrderByTimestampDesc(
                    any(), any(), any())).thenReturn(Page.empty());

            Page<ContentDto> result = profileService.getOwnPosts(userId, 0, 10);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("User not found — throws ResourceNotFoundException")
        void getOwnPosts_unknownUser_throwsNotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> profileService.getOwnPosts(UUID.randomUUID(), 0, 10))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getFriends()")
    class GetFriends {

        @Test
        @DisplayName("Happy path — returns friend on receiver side")
        void getFriends_aliceIsRequester_returnsBobAsReceiver() {
            User bob = User.builder()
                    .userId(UUID.randomUUID())
                    .username("bob")
                    .email("bob@example.com")
                    .status(UserStatus.ONLINE)
                    .build();

            Friendship friendship = Friendship.builder()
                    .friendshipId(UUID.randomUUID())
                    .requester(alice)
                    .receiver(bob)
                    .status(FriendshipStatus.ACCEPTED)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));
            when(friendshipRepository.findAcceptedFriendships(userId))
                    .thenReturn(List.of(friendship));

            List<UserDto> friends = profileService.getFriends(userId);

            assertThat(friends).hasSize(1);
            assertThat(friends.get(0).username()).isEqualTo("bob");
        }

        @Test
        @DisplayName("Alice is receiver — Bob (requester) still returned")
        void getFriends_aliceIsReceiver_returnsRequesterAsFriend() {
            User bob = User.builder()
                    .userId(UUID.randomUUID())
                    .username("bob")
                    .email("bob@example.com")
                    .status(UserStatus.OFFLINE)
                    .build();

            // Alice is the RECEIVER this time
            Friendship friendship = Friendship.builder()
                    .friendshipId(UUID.randomUUID())
                    .requester(bob)
                    .receiver(alice)
                    .status(FriendshipStatus.ACCEPTED)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));
            when(friendshipRepository.findAcceptedFriendships(userId))
                    .thenReturn(List.of(friendship));

            List<UserDto> friends = profileService.getFriends(userId);

            assertThat(friends).hasSize(1);
            assertThat(friends.get(0).username()).isEqualTo("bob");
        }

        @Test
        @DisplayName("No friends — returns empty list, no exception")
        void getFriends_noFriends_returnsEmptyList() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(alice));
            when(friendshipRepository.findAcceptedFriendships(userId)).thenReturn(List.of());

            List<UserDto> friends = profileService.getFriends(userId);

            assertThat(friends).isEmpty();
        }
    }
}