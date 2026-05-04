package org.connecthub.backend.dto.response;

import org.connecthub.backend.enums.ContentType;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContentDto(
        UUID contentId,
        String contentText,
        String imagePath,
        ContentType contentType,
        LocalDateTime timestamp,
        UserDto author
) {}