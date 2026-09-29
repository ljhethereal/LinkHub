package org.example.linkhub.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.example.linkhub.dto.PostCreateRequest;
import org.example.linkhub.dto.PostResponse;
import org.example.linkhub.entity.Category;
import org.example.linkhub.entity.Post;
import org.example.linkhub.entity.Tag;
import org.example.linkhub.event.PostChangedEvent;
import org.example.linkhub.exception.BusinessException;
import org.example.linkhub.repository.CategoryRepository;
import org.example.linkhub.repository.PostRepository;
import org.example.linkhub.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class PostService {
    public static final String DETAIL_KEY_PREFIX = "post:details:";
    public static final String DETAIL_LOCK_PREFIX = "post:details:lock:";
    public static final String PENDING_KEY_PREFIX = "post:views:pending:";
    public static final String TOTAL_KEY_PREFIX = "post:views:total:";
    public static final String DIRTY_SET_KEY = "post:views:dirty";
    public static final String NULL_MARKER = "##NULL##";

    private static final long DETAIL_BASE_TTL_SECONDS = 600;
    private static final long DETAIL_TTL_JITTER_SECONDS = 120;
    private static final long NULL_TTL_SECONDS = 60;
    private static final long REBUILD_LOCK_TTL_SECONDS = 10;

    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final UserService userService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    public PostService(PostRepository postRepository,
                       TagRepository tagRepository,
                       CategoryRepository categoryRepository,
                       UserService userService,
                       StringRedisTemplate redisTemplate,
                       ObjectMapper objectMapper,
                       org.springframework.context.ApplicationEventPublisher eventPublisher) {
        this.postRepository = postRepository;
        this.tagRepository = tagRepository;
        this.categoryRepository = categoryRepository;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    private PostResponse toResponse(Post post) {
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setViews(post.getViews());
        response.setCreateTime(post.getCreatedAt());
        response.setUpdateTime(post.getUpdatedAt());

        if (post.getUser() != null) {
            response.setAuthorId(post.getUser().getId());
            response.setAuthorName(post.getUser().getUserName());
        }

        if (post.getCategory() != null) {
            response.setCategoryId(post.getCategory().getId());
            response.setCategoryName(post.getCategory().getName());
        }

        if (post.getTags() != null) {
            response.setTags(post.getTags().stream().map(Tag::getTagName).toList());
        }

        return response;
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId).orElseThrow(() -> new BusinessException("分类不存在"));
    }

    private Set<Tag> resolveTags(List<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        if (tagNames == null || tagNames.isEmpty()) {
            return tags;
        }
        List<String> distinct = tagNames.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
        if (distinct.isEmpty()) {
            return tags;
        }

        tags.addAll(tagRepository.findByTagNameIn(distinct));
        Set<String> existing = new HashSet<>();
        tags.forEach(tag -> existing.add(tag.getTagName()));

        for (String name : distinct) {
            if (!existing.contains(name)) {
                tagRepository.upsertByName(name);
            }
        }
        tags.addAll(tagRepository.findByTagNameIn(distinct));
        return tags;
    }

    @Transactional
    public PostResponse createPost(PostCreateRequest request) {
        Post post = new Post();

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setViews(0L);

        post.setUser(userService.getCurrentUser());

        post.setCategory(resolveCategory(request.getCategoryId()));

        post.setTags(resolveTags(request.getTagNames()));

        return toResponse(postRepository.save(post));
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(404, "帖子不存在"));

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(resolveCategory(request.getCategoryId()));
        post.setTags(resolveTags(request.getTagNames()));

        eventPublisher.publishEvent(new PostChangedEvent(postId, PostChangedEvent.UPDATED));
        return toResponse(post);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostDetails(Long postId) {
        PostResponse response = loadDetail(postId);
        response.setViews(recordView(postId));
        return response;
    }

    private PostResponse loadDetail(Long postId) {
        String detailKey = DETAIL_KEY_PREFIX + postId;

        String cached = redisTemplate.opsForValue().get(detailKey);
        if (NULL_MARKER.equals(cached)) {
            throw new BusinessException(404, "帖子不存在");
        }
        if (cached != null) {
            PostResponse parsed = parseCache(cached, detailKey);
            if (parsed != null) {
                return parsed;
            }
        }

        boolean locked = Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(DETAIL_LOCK_PREFIX + postId, "1", REBUILD_LOCK_TTL_SECONDS, TimeUnit.SECONDS));
        if (!locked) {
            sleepQuietly(120);
            String recheck = redisTemplate.opsForValue().get(detailKey);
            if (recheck != null && !NULL_MARKER.equals(recheck)) {
                PostResponse parsed = parseCache(recheck, detailKey);
                if (parsed != null) {
                    return parsed;
                }
            }
            return loadFromDb(postId);
        }

        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new BusinessException(404, "帖子不存在"));
            PostResponse response = toResponse(post);
            try {
                long ttl = DETAIL_BASE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(DETAIL_TTL_JITTER_SECONDS);
                redisTemplate.opsForValue().set(detailKey, objectMapper.writeValueAsString(response),
                        ttl, TimeUnit.SECONDS);
            } catch (JsonProcessingException e) {
                log.warn("帖子详情序列化失败，跳过缓存: {}", postId, e);
            }
            return response;
        } catch (BusinessException e) {
            if (e.getCode() == 404) {
                redisTemplate.opsForValue().set(detailKey, NULL_MARKER, NULL_TTL_SECONDS, TimeUnit.SECONDS);
            }
            throw e;
        } finally {
            redisTemplate.delete(DETAIL_LOCK_PREFIX + postId);
        }
    }

    @Nullable
    private PostResponse parseCache(String raw, String detailKey) {
        try {
            return objectMapper.readValue(raw, PostResponse.class);
        } catch (JsonProcessingException e) {
            log.warn("缓存内容损坏，删除并回源: {}", detailKey, e);
            redisTemplate.delete(detailKey);
            return null;
        }
    }

    private PostResponse loadFromDb(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(404, "帖子不存在"));
        return toResponse(post);
    }

    private long recordView(Long postId) {
        String pendingKey = PENDING_KEY_PREFIX + postId;
        String totalKey = TOTAL_KEY_PREFIX + postId;

        Long pending = redisTemplate.opsForValue().increment(pendingKey);
        if (pending == null) {
            pending = 0L;
        }
        redisTemplate.opsForSet().add(DIRTY_SET_KEY, String.valueOf(postId));

        Long total = redisTemplate.opsForValue().increment(totalKey);
        if (total != null && total == 1L) {
            long dbViews = postRepository.findViewsById(postId).orElse(0L);
            total = dbViews + pending;
            redisTemplate.opsForValue().set(totalKey, String.valueOf(total));
        }
        return total == null ? pending : total;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional(readOnly = true)
    public Page<PostResponse> search(String keyword, Long categoryId, String tagName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Long normalizedCategoryId = categoryId != null && categoryId > 0 ? categoryId : null;
        String normalizedTagName = StringUtils.hasText(tagName) ? tagName.trim() : null;
        return postRepository.search(normalizedKeyword, normalizedCategoryId, normalizedTagName, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(404, "帖子不存在"));

        eventPublisher.publishEvent(new PostChangedEvent(postId, PostChangedEvent.DELETED));
        postRepository.delete(post);
    }
}
