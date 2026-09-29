package org.example.linkhub.service;

import org.example.linkhub.dto.CommentCreateRequest;
import org.example.linkhub.dto.CommentResponse;
import org.example.linkhub.entity.Comment;
import org.example.linkhub.entity.Post;
import org.example.linkhub.exception.BusinessException;
import org.example.linkhub.repository.CommentRepository;
import org.example.linkhub.repository.PostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    public CommentService(PostRepository postRepository, CommentRepository commentRepository, UserService userService){
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userService = userService;
    }

    private CommentResponse toResponse(Comment comment){
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setCreateTime(comment.getCreatedAt());
        if(comment.getUser() != null){
            response.setAuthorId(comment.getUser().getId());
            response.setAuthorName(comment.getUser().getUserName());
        }

        return response;
    }

    @Transactional
    public CommentResponse createComment(Long postId, CommentCreateRequest request){
        Post post = postRepository.findById(postId).orElseThrow(() -> new BusinessException("帖子不存在"));
        Comment comment = new Comment();
        comment.setContent(request.getContent());
        comment.setPost(post);
        comment.setUser(userService.getCurrentUser());
        post.getComments().add(comment);
        return toResponse(commentRepository.save(comment));
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(Long postId){
        Post post = postRepository.findById(postId).orElseThrow(() -> new BusinessException("帖子不存在"));
        return post.getComments().stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId){
        Post post = postRepository.findById(postId).orElseThrow(() -> new BusinessException("帖子不存在"));
        boolean removed = post.getComments().removeIf(c -> c.getId().equals(commentId));
        if(!removed){
            throw new BusinessException("评论不存在");
        }
    }
}
