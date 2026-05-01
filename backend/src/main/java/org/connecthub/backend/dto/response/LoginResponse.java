package org.connecthub.backend.dto.response;

public record LoginResponse(
        String token,
        UserDto user
) {}
