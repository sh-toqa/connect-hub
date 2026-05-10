package org.connecthub.backend.service;

import lombok.RequiredArgsConstructor;
import org.connecthub.backend.dto.response.FriendshipDto;
import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.enums.FriendshipStatus;
import org.connecthub.backend.exception.ResourceNotFoundException;
import org.connecthub.backend.mapper.UserMapper;
import org.connecthub.backend.model.Friendship;
import org.connecthub.backend.model.User;
import org.connecthub.backend.repository.FriendshipRepository;
import org.connecthub.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public FriendshipDto sendRequest(UUID requesterId, UUID receiverId) {
        if (requesterId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send a friend request to yourself");
        }
        User requester = findUser(requesterId);
        User receiver = findUser(receiverId);

        // If friendship exists
        friendshipRepository.findBetweenUsers(requesterId, receiverId)
                .ifPresent(f -> {throw new IllegalStateException("Friendship already exists");});

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendshipStatus.PENDING)
                .build();
        return userMapper.toFriendshipDto(friendshipRepository.save(friendship));

    }

    @Transactional
    public FriendshipDto acceptRequest(UUID friendshipId, UUID receiverId) {
        Friendship friendship = findFriendship(friendshipId);
        ensureReceiver(friendship, receiverId);
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        return userMapper.toFriendshipDto(friendshipRepository.save(friendship));
    }

    @Transactional
    public void declineRequest(UUID friendshipId, UUID receiverId) {
        Friendship friendship = findFriendship(friendshipId);
        ensureReceiver(friendship, receiverId);
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void removeFriend(UUID friendshipId, UUID receiverId) {
        Friendship friendship = findFriendship(friendshipId);
        ensureParticipant(friendship, receiverId);
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public FriendshipDto blockUser(UUID blockerId, UUID targetId) {
        User blocker = findUser(blockerId);
        User target = findUser(targetId);
        friendshipRepository.findBetweenUsers(blockerId, targetId)
                .ifPresent(f -> {friendshipRepository.delete(f);});
        Friendship blocked = Friendship.builder()
                .requester(blocker)
                .receiver(target)
                .status(FriendshipStatus.BLOCKED)
                .build();
        return userMapper.toFriendshipDto(friendshipRepository.save(blocked));
    }

    @Transactional
    public void unblockUser(UUID blockerId, UUID targetId) {
        friendshipRepository.findBetweenUsers(blockerId, targetId)
                .filter(f -> f.getStatus() == FriendshipStatus.BLOCKED
                        && f.getRequester().getUserId().equals(blockerId))
                .ifPresent(friendshipRepository::delete);
    }

    public List<FriendshipDto> getPendingRequests(UUID userId) {
        User user = findUser(userId);
        return friendshipRepository
                .findByReceiverAndStatus(user, FriendshipStatus.PENDING)
                .stream()
                .map(userMapper::toFriendshipDto)
                .toList();
    }

    public List<FriendshipDto> getFriends(UUID userId) {
        return friendshipRepository.findAcceptedFriendships(userId)
                .stream()
                .map(userMapper::toFriendshipDto)
                .toList();
    }

    public List<UserDto> getSuggestions(UUID userId) {
        return userRepository.findSuggestedFriends(userId)
                .stream()
                .map(userMapper::toUserDto)
                .toList();
    }

    public String getStatus(UUID currentUserId, UUID otherUserId) {
        return friendshipRepository.findBetweenUsers(currentUserId, otherUserId)
                .map(f -> f.getStatus().name())
                .orElse("NONE");
    }

    // Helpers
    private User findUser(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found: "+ userId));
    }

    private Friendship findFriendship(UUID id) {
        return friendshipRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Friendship not found: " + id));
    }

    private void ensureReceiver(Friendship f, UUID userId) {
        if (!f.getReceiver().getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Friendship not found");
        }
    }

    private void ensureParticipant(Friendship f, UUID userId) {
        boolean isParticipant = f.getRequester().getUserId().equals(userId)
                || f.getReceiver().getUserId().equals(userId);
        if (!isParticipant) {
            throw new ResourceNotFoundException("Friendship not found");
        }
    }


}
