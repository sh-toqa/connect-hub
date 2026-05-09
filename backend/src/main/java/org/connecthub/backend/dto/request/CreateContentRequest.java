package org.connecthub.backend.dto.request;

import org.connecthub.backend.enums.ContentType;
import jakarta.validation.constraints.NotNull;

public record CreateContentRequest(

        String contentText,

        String imagePath,

        @NotNull(message = "Content type is required (POST or STORY)")
        ContentType contentType

) {}
