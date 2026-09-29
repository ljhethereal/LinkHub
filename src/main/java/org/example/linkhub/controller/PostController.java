package org.example.linkhub.controller;

import jakarta.validation.Valid;
import org.example.linkhub.dto.PostCreateRequest;
import org.example.linkhub.dto.PostResponse;
import org.example.linkhub.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService){
        this.postService = postService;
    }

    @PostMapping
    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostCreateRequest request){
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(request));
    }

    @GetMapping
    public Page<PostResponse> search(@RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Long categoryId,
                                     @RequestParam(required = false) String tagName,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "10") int size){
        return postService.search(keyword, categoryId, tagName, page, size);
    }

    @GetMapping("/{postId}")
    public PostResponse detail(@PathVariable Long postId){
        return postService.getPostDetails(postId);
    }

    @PutMapping("/{postId}")
    public PostResponse updatePost(@PathVariable Long postId, @Valid @RequestBody PostCreateRequest request){
        return postService.updatePost(postId, request);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId){
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
