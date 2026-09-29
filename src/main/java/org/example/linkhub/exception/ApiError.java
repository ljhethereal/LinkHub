package org.example.linkhub.exception;

import java.time.LocalDateTime;

public record ApiError(LocalDateTime timestamp, int code, String message, String path) {

    public static ApiError of(int code, String message, String path) {
        return new ApiError(LocalDateTime.now(), code, message, path);
    }
}
