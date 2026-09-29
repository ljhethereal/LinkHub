package org.example.linkhub.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PostResponse {
    private Long id;
    private String title;
    private String content;
    private Long views;
    private Long authorId;
    private String authorName;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private List<String> tags;
}
