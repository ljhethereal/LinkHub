package org.example.linkhub.event;

public record PostChangedEvent(Long postId, String action) {
    public static final String UPDATED = "UPDATED";
    public static final String DELETED = "DELETED";
}
