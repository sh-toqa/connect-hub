package org.connecthub.backend.dto.response;

import org.connecthub.backend.enums.UserStatus;

import java.util.UUID;

/**
 * Safe outbound user representation.
 * Never includes password or internal paths unless explicitly needed.
 */

public record UserDto(
        UUID userId,
        String username,
        String email,
        String bio,
        String profilePhotoPath,
        String coverPhotoPath,
        UserStatus status
) {}