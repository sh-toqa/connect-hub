package org.connecthub.backend.Unit.service;

import org.connecthub.backend.dto.request.CreateContentRequest;
import org.connecthub.backend.dto.response.ContentDto;
import org.connecthub.backend.enums.ContentType;
import org.connecthub.backend.enums.FriendshipStatus;
import org.connecthub.backend.enums.UserStatus;
import org.connecthub.backend.exception.ResourceNotFoundException;
import org.connecthub.backend.mapper.UserMapper;
import org.connecthub.backend.model.Content;
import org.connecthub.backend.model.Friendship;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.ContentRepository;
import org.connecthub.backend.repository.FriendshipRepository;
import org.connecthub.backend.repository.UserRepository;
import org.connecthub.backend.service.ContentService;
import org.connecthub.backend.service.StorageService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.*;
        import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentService Unit Tests")
class ContentServiceTest {

    @Mock private ContentRepository    contentRepository;
    @Mock private UserRepository       userRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private StorageService storageService;

    private final UserMapper userMapper = new UserMapper();

    @InjectMocks
    private ContentService contentService;

    private UUID   authorId;
    private User   alice;
    private User   bob;

    @BeforeEach
    void setUp() {
        contentService = new ContentService(
                contentRepository, userRepository, friendshipRepository,
                storageService, userMapper);

        authorId = UUID.randomUUID();
        alice = User.builder()
                .userId(authorId)
                .username("alice")
                .email("alice@example.com")
                .status(UserStatus.ONLINE)
                .build();

        bob = User.builder()
                .userId(UUID.randomUUID())
                .username("bob")
                .email("bob@example.com")
                .status(UserStatus.ONLINE)
                .build();
    }


    @DisplayName("createPost()")
    class CreatePost {

        @Test
        @DisplayName("Happy path — text-only post saved and returned")
        void createPost_textOnly_savedAndReturned() {
            when(userRepository.findById(authorId)).thenReturn(Optional.of(alice));
            when(contentRepository.save(any(Content.class))).thenAnswer(inv -> {
                Content c = inv.getArgument(0);
                c.setContentId(UUID.randomUUID());
                return c;
            });

            CreateContentRequest req = new CreateContentRequest("Hello world", null, ContentType.POST);
            ContentDto result = contentService.createPost(authorId, req, null);

            assertThat(result.contentText()).isEqualTo("Hello world");
            assertThat(result.contentType()).isEqualTo(ContentType.POST);
            assertThat(result.author().username()).isEqualTo("alice");
            assertThat(result.imagePath()).isNull();
            verify(storageService, never()).store(any(), any()); // no image → no storage call
        }

        @Test
        @DisplayName("Post with image — image stored and path saved")
        void createPost_withImage_imageStoredAndPathSaved() {
            when(userRepository.findById(authorId)).thenReturn(Optional.of(alice));
            when(storageService.store(any(), eq("posts"))).thenReturn("/uploads/posts/img.jpg");
            when(contentRepository.save(any(Content.class))).thenAnswer(inv -> inv.getArgument(0));

            MockMultipartFile image = new MockMultipartFile(
                    "image", "photo.jpg", "image/jpeg", new byte[512]);
            CreateContentRequest req = new CreateContentRequest("With image", null, ContentType.POST);

            ContentDto result = contentService.createPost(authorId, req, image);

            verify(storageService).store(image, "posts");
            assertThat(result.imagePath()).isEqualTo("/uploads/posts/img.jpg");
        }

        @Test
        @DisplayName("Author not found — throws ResourceNotFoundException")
        void createPost_authorNotFound_throwsException() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    contentService.createPost(UUID.randomUUID(),
                            new CreateContentRequest("text", null, ContentType.POST), null))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(contentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Content type is always POST regardless of request field")
        void createPost_contentTypeIsAlwaysPost() {
            when(userRepository.findById(authorId)).thenReturn(Optional.of(alice));

            ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
            when(contentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            contentService.createPost(authorId,
                    new CreateContentRequest("text", null, ContentType.POST), null);

            assertThat(captor.getValue().getContentType()).isEqualTo(ContentType.POST);
        }
    }

    @Nested
    @DisplayName("createStory()")
    class CreateStory {

        @Test
        @DisplayName("Happy path — story saved with STORY content type")
        void createStory_saved_withStoryType() {

            when(userRepository.findById(authorId))
                    .thenReturn(Optional.of(alice));

            when(contentRepository.saveAndFlush(any(Content.class)))
                    .thenAnswer(inv -> {
                        Content c = inv.getArgument(0);
                        c.setContentId(UUID.randomUUID());
                        c.setAuthor(alice);
                        c.setTimestamp(LocalDateTime.now());
                        return c;
                    });

            CreateContentRequest req =
                    new CreateContentRequest("My story", null, ContentType.STORY);

            ContentDto result =
                    contentService.createStory(authorId, req, null);

            assertThat(result).isNotNull();
            assertThat(result.contentType()).isEqualTo(ContentType.STORY);
        }

        @Test
        @DisplayName("Story with image stored under 'stories' subdirectory")
        void createStory_withImage_storedUnderStoriesDir() {
            when(userRepository.findById(authorId)).thenReturn(Optional.of(alice));
            when(storageService.store(any(), eq("stories")))
                    .thenReturn("/uploads/stories/img.jpg");

            MockMultipartFile image = new MockMultipartFile(
                    "image", "story.jpg", "image/jpeg", new byte[256]);

            contentService.createStory(
                    authorId,
                    new CreateContentRequest("Story", null, ContentType.STORY),
                    image
            );

            verify(storageService).store(image, "stories");
        }
}

    @Nested
    @DisplayName("deleteContent()")
    class DeleteContent {

        @Test
        @DisplayName("Happy path — author deletes own post, image cleaned up")
        void deleteContent_ownPost_deletedAndImageCleaned() {
            UUID contentId = UUID.randomUUID();
            Content post = Content.builder()
                    .contentId(contentId)
                    .author(alice)
                    .contentText("Post to delete")
                    .imagePath("/uploads/posts/img.jpg")
                    .contentType(ContentType.POST)
                    .build();

            when(contentRepository.findById(contentId)).thenReturn(Optional.of(post));

            contentService.deleteContent(contentId, authorId);

            verify(storageService).delete("/uploads/posts/img.jpg");
            verify(contentRepository).delete(post);
        }

        @Test
        @DisplayName("Delete post with no image — no storage call made")
        void deleteContent_noImage_noStorageCall() {
            UUID contentId = UUID.randomUUID();
            Content post = Content.builder()
                    .contentId(contentId)
                    .author(alice)
                    .contentText("No image post")
                    .imagePath(null)
                    .contentType(ContentType.POST)
                    .build();

            when(contentRepository.findById(contentId)).thenReturn(Optional.of(post));

            contentService.deleteContent(contentId, authorId);

            verify(storageService).delete(null); // null path - StorageService ignores it
            verify(contentRepository).delete(post);
        }

        @Test
        @DisplayName("Non-author tries to delete — throws ResourceNotFoundException")
        void deleteContent_notAuthor_throwsException() {
            UUID contentId  = UUID.randomUUID();
            UUID intruderId = UUID.randomUUID();
            Content post = Content.builder()
                    .contentId(contentId)
                    .author(alice)  // alice owns it
                    .contentText("Alice's post")
                    .contentType(ContentType.POST)
                    .build();

            when(contentRepository.findById(contentId)).thenReturn(Optional.of(post));

            // intruder (not alice) tries to delete
            assertThatThrownBy(() -> contentService.deleteContent(contentId, intruderId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(contentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Content not found — throws ResourceNotFoundException")
        void deleteContent_notFound_throwsException() {
            when(contentRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    contentService.deleteContent(UUID.randomUUID(), authorId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getFriendPosts()")
    class GetFriendPosts {

        @Test
        @DisplayName("Happy path — returns paginated friend posts")
        void getFriendPosts_withFriends_returnsPosts() {
            Friendship friendship = Friendship.builder()
                    .requester(alice).receiver(bob)
                    .status(FriendshipStatus.ACCEPTED).build();
            when(friendshipRepository.findAcceptedFriendships(authorId))
                    .thenReturn(List.of(friendship));

            Content post = Content.builder()
                    .contentId(UUID.randomUUID())
                    .author(bob)
                    .contentText("Bob's post")
                    .contentType(ContentType.POST)
                    .timestamp(LocalDateTime.now())
                    .build();

            when(contentRepository.findFriendContent(
                    anyList(), eq(ContentType.POST), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(post)));

            Page<ContentDto> result = contentService.getFriendPosts(authorId, 0, 10);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).contentText()).isEqualTo("Bob's post");
        }

        @Test
        @DisplayName("No friends — returns empty page immediately")
        void getFriendPosts_noFriends_returnsEmptyPage() {
            when(friendshipRepository.findAcceptedFriendships(authorId))
                    .thenReturn(List.of());

            Page<ContentDto> result = contentService.getFriendPosts(authorId, 0, 10);

            assertThat(result.getContent()).isEmpty();
            verify(contentRepository, never()).findFriendContent(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getFriendStories()")
    class GetFriendStories {

        @Test
        @DisplayName("Returns only active (non-expired) stories")
        void getFriendStories_filtersExpiredStories() {
            Friendship friendship = Friendship.builder()
                    .requester(alice).receiver(bob)
                    .status(FriendshipStatus.ACCEPTED).build();
            when(friendshipRepository.findAcceptedFriendships(authorId))
                    .thenReturn(List.of(friendship));

            Content activeStory = Content.builder()
                    .contentId(UUID.randomUUID()).author(bob)
                    .contentText("Fresh story").contentType(ContentType.STORY)
                    .timestamp(LocalDateTime.now().minusHours(1))  // 1 hour ago - active
                    .build();
            Content expiredStory = Content.builder()
                    .contentId(UUID.randomUUID()).author(bob)
                    .contentText("Old story").contentType(ContentType.STORY)
                    .timestamp(LocalDateTime.now().minusHours(25)) // 25 hours ago - expired
                    .build();

            when(contentRepository.findFriendContent(
                    anyList(), eq(ContentType.STORY), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(activeStory, expiredStory)));

            List<ContentDto> result = contentService.getFriendStories(authorId);

            // Only the active story should be returned
            assertThat(result).hasSize(1);
            assertThat(result.get(0).contentText()).isEqualTo("Fresh story");
        }

        @Test
        @DisplayName("No friends — returns empty list immediately")
        void getFriendStories_noFriends_returnsEmptyList() {
            when(friendshipRepository.findAcceptedFriendships(authorId))
                    .thenReturn(List.of());

            List<ContentDto> result = contentService.getFriendStories(authorId);

            assertThat(result).isEmpty();
            verify(contentRepository, never()).findFriendContent(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("deleteExpiredStories() scheduler")
    class DeleteExpiredStories {

        @Test
        @DisplayName("Calls repository with correct expiry timestamp")
        void deleteExpiredStories_callsRepositoryWithCutoff() {
            when(contentRepository.deleteExpiredStories(any(LocalDateTime.class))).thenReturn(3);

            contentService.deleteExpiredStories();

            ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(contentRepository).deleteExpiredStories(captor.capture());

            // The cutoff should be approximately 24 hours ago
            LocalDateTime cutoff = captor.getValue();
            assertThat(cutoff).isBefore(LocalDateTime.now().minusHours(23));
            assertThat(cutoff).isAfter(LocalDateTime.now().minusHours(25));
        }

        @Test
        @DisplayName("No expired stories — repository called, no exception")
        void deleteExpiredStories_nothingToDelete_noException() {
            when(contentRepository.deleteExpiredStories(any())).thenReturn(0);

            assertThatCode(() -> contentService.deleteExpiredStories())
                    .doesNotThrowAnyException();
        }
    }
}