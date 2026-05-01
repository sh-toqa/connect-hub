package org.connecthub.backend.repository;

import org.connecthub.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}