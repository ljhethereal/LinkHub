package org.example.linkhub.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponse {
    private String content;
    private Long id;
    private LocalDateTime createTime;
    private String authorName;
    private Long authorId;
}
