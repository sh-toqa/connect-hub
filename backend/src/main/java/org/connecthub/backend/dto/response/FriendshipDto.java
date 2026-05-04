package org.connecthub.backend.dto.response;

import  org.connecthub.backend.enums.FriendshipStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record FriendshipDto(
        UUID friendshipId,
        UserDto requester,
        UserDto receiver,
        FriendshipStatus status,
        LocalDateTime createdAt
) {}
