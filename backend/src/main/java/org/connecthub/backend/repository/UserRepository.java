package org.connecthub.backend.repository;

import org.connecthub.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
/**
 * Repository interface for User entities.
 * Provides methods for querying users by email, username, and existence checks.
 * Extends JpaRepository to inherit standard CRUD operations.
 */

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    /**
     * Find users who are not the current user, not already friends,
     * and not blocked — used for friend suggestions.
     */
    @Query("""
        SELECT u FROM User u
        WHERE u.userId != :currentUserId
        AND u.userId NOT IN (
            SELECT f.receiver.userId FROM Friendship f
            WHERE f.requester.userId = :currentUserId
        )
        AND u.userId NOT IN (
            SELECT f.requester.userId FROM Friendship f
            WHERE f.receiver.userId = :currentUserId
        )
        ORDER BY u.createdAt DESC
        """)
    List<User> findSuggestedFriends(@Param("currentUserId") UUID currentUserId);
}