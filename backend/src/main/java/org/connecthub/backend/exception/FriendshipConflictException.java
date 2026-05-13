package org.connecthub.backend.exception;

public class FriendshipConflictException extends RuntimeException {
    public FriendshipConflictException(String message) {
        super(message);
    }
}
