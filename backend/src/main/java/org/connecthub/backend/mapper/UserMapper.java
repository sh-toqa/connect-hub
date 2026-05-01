package org.connecthub.backend.mapper;

import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.model.User;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between User entities and User DTOs.
 */

@Component
public class UserMapper {

    public UserDto toUserDto(User user) {
        if (user == null) return null;
        return new UserDto(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getBio(),
                user.getProfilePhotoPath(),
                user.getCoverPhotoPath(),
                user.getStatus()
        );
    }
}