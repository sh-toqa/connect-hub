package org.connecthub.backend.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(

        @Size(max = 300, message = "Bio cannot exceed 300 characters")
        String bio,

        String profilePhotoPath,

        String coverPhotoPath

) {}
