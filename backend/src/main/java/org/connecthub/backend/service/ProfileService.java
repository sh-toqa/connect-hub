package org.connecthub.backend.service;

import org.connecthub.backend.dto.request.UpdatePasswordRequest;
import org.connecthub.backend.dto.request.UpdateProfileRequest;
import org.connecthub.backend.dto.response.ContentDto;
import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.enums.ContentType;
import org.connecthub.backend.exception.InvalidPasswordException;
import org.connecthub.backend.exception.ResourceNotFoundException;
import org.connecthub.backend.mapper.UserMapper;
import org.connecthub.backend.model.Friendship;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.ContentRepository;
import org.connecthub.backend.repository.FriendshipRepository;
import org.connecthub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Handles all profile management use cases:
 *  - View own profile
 *  - Update bio, photos, password
 *  - View own posts (paginated)
 *  - View friends list with status
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository      userRepository;
    private final ContentRepository   contentRepository;
    private final FriendshipRepository friendshipRepository;
    private final StorageService      storageService;
    private final PasswordEncoder     passwordEncoder;
    private final UserMapper          userMapper;

    // View own or another user's profile
    public UserDto getProfile(UUID userId) {
        User user = findUserOrThrow(userId);
        return userMapper.toUserDto(user);
    }

    // Update bio and direct profile paths (used when no file upload is involved)
    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);

        if (request.bio() != null) {
            user.setBio(request.bio());
        }
        // Direct path updates (used when no file upload is involved)
        if (request.profilePhotoPath() != null) {
            user.setProfilePhotoPath(request.profilePhotoPath());
        }
        if (request.coverPhotoPath() != null) {
            user.setCoverPhotoPath(request.coverPhotoPath());
        }

        return userMapper.toUserDto(userRepository.save(user));
    }

    // Upload profile photo
    @Transactional
    public UserDto updateProfilePhoto(UUID userId, MultipartFile file) {
        User user = findUserOrThrow(userId);

        // Delete old photo to avoid orphaned files
        storageService.delete(user.getProfilePhotoPath());

        String newPath = storageService.store(file, "profiles");
        user.setProfilePhotoPath(newPath);

        return userMapper.toUserDto(userRepository.save(user));
    }

    // Upload cover photo
    @Transactional
    public UserDto updateCoverPhoto(UUID userId, MultipartFile file) {
        User user = findUserOrThrow(userId);

        storageService.delete(user.getCoverPhotoPath());

        String newPath = storageService.store(file, "covers");
        user.setCoverPhotoPath(newPath);

        return userMapper.toUserDto(userRepository.save(user));
    }

    // Update password with verification of current password
    @Transactional
    public void updatePassword(UUID userId, UpdatePasswordRequest request) {
        User user = findUserOrThrow(userId);

        // Verify the user knows their current password before allowing change
        if (!passwordEncoder.matches(request.currentPassword(), user.getHashedPassword())) {
            throw new InvalidPasswordException();
        }

        user.setHashedPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    // View own posts with pagination
    public Page<ContentDto> getOwnPosts(UUID userId, int page, int size) {
        findUserOrThrow(userId);   // validate user exists
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return contentRepository
                .findByAuthor_UserIdAndContentTypeOrderByTimestampDesc(userId, ContentType.POST, pageable)
                .map(userMapper::toContentDto);
    }

    public Page<ContentDto> getOwnStories(UUID userId, int page, int size) {
        findUserOrThrow(userId);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return contentRepository
                .findByAuthor_UserIdAndContentTypeOrderByTimestampDesc(
                        userId, ContentType.STORY, pageable)
                .map(userMapper::toContentDto);
    }

    // View friends list with status
    public List<UserDto> getFriends(UUID userId) {
        findUserOrThrow(userId);

        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(userId);
        return friendships.stream()
                .map(f -> {
                    // Return the OTHER user in the friendship
                    User friend = f.getRequester().getUserId().equals(userId)
                            ? f.getReceiver()
                            : f.getRequester();
                    return userMapper.toUserDto(friend);
                })
                .toList();
    }

    // Helper method to fetch user or throw 404 if not found
    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}