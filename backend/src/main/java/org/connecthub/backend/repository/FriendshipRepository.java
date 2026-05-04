package org.connecthub.backend.repository;

import org.connecthub.backend.enums.FriendshipStatus;
import org.connecthub.backend.model.Friendship;
import org.connecthub.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    // Find the friendship relationship between two users
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.userId = :userA AND f.receiver.userId = :userB)
           OR (f.requester.userId = :userB AND f.receiver.userId = :userA)
        """)
    Optional<Friendship> findBetweenUsers(
            @Param("userA") UUID userA,
            @Param("userB") UUID userB);

    // Pending requests received by a user
    List<Friendship> findByReceiverAndStatus(User receiver, FriendshipStatus status);

    // All accepted friendships for a user
    @Query("""
        SELECT f FROM Friendship f
        WHERE (f.requester.userId = :userId OR f.receiver.userId = :userId)
        AND f.status = 'ACCEPTED'
        """)
    List<Friendship> findAcceptedFriendships(@Param("userId") UUID userId);

    // IDs of users the given user has blocked
    @Query("""
        SELECT f.receiver.userId FROM Friendship f
        WHERE f.requester.userId = :userId AND f.status = 'BLOCKED'
        """)
    List<UUID> findBlockedUserIds(@Param("userId") UUID userId);

    // Check if userA has blocked userB
    @Query("""
        SELECT COUNT(f) > 0 FROM Friendship f
        WHERE f.requester.userId = :blockerId
        AND f.receiver.userId = :targetId
        AND f.status = 'BLOCKED'
        """)
    boolean isBlocked(@Param("blockerId") UUID blockerId, @Param("targetId") UUID targetId);
}
