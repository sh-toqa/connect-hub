package org.connecthub.backend.mapper;

import org.connecthub.backend.dto.response.UserDto;
import org.connecthub.backend.model.User;
import org.connecthub.backend.dto.response.ContentDto;
import org.connecthub.backend.dto.response.FriendshipDto;
import org.connecthub.backend.model.Content;
import org.connecthub.backend.model.Friendship;
import org.springframework.stereotype.Component;

/**
 * Mapper class for converting between User, Content, Friendship entities and their corresponding DTOs.
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
    
    public ContentDto toContentDto(Content content) {
        if (content == null) return null;
        return new ContentDto(
                content.getContentId(),
                content.getContentText(),
                content.getImagePath(),
                content.getContentType(),
                content.getTimestamp(),
                toUserDto(content.getAuthor())
        );
    }

    public FriendshipDto toFriendshipDto(Friendship friendship) {
        if (friendship == null) return null;
        return new FriendshipDto(
                friendship.getFriendshipId(),
                toUserDto(friendship.getRequester()),
                toUserDto(friendship.getReceiver()),
                friendship.getStatus(),
                friendship.getCreatedAt()
        );
    }
}