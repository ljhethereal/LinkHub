package org.example.linkhub.controller;

import jakarta.validation.Valid;
import org.example.linkhub.dto.CommentCreateRequest;
import org.example.linkhub.dto.CommentResponse;
import org.example.linkhub.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;

    public CommentController(CommentService commentService){
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(@PathVariable Long postId, @Valid @RequestBody CommentCreateRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(postId, request));
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable Long postId){
        return commentService.getCommentsByPost(postId);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId, @PathVariable Long commentId){
        commentService.deleteComment(postId, commentId);
        return ResponseEntity.noContent().build();
    }
}
