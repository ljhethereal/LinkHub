package org.example.linkhub.service;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.linkhub.dto.CategoryCreateRequest;
import org.example.linkhub.dto.CategoryResponse;
import org.example.linkhub.entity.Category;
import org.example.linkhub.event.CategoryChangedEvent;
import org.example.linkhub.exception.BusinessException;
import org.example.linkhub.repository.CategoryRepository;
import org.example.linkhub.repository.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class CategoryService {
    public static final String TREE_KEY = "category:tree";

    private static final long TREE_BASE_TTL_SECONDS = 3600;
    private static final long TREE_TTL_JITTER_SECONDS = 300;

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public CategoryService(CategoryRepository categoryRepository,
                           PostRepository postRepository,
                           StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           ApplicationEventPublisher eventPublisher) {
        this.categoryRepository = categoryRepository;
        this.postRepository = postRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new BusinessException("父分类不存在"));
            parent.addChild(category);
        }
        Category saved = categoryRepository.save(category);

        eventPublisher.publishEvent(new CategoryChangedEvent(saved.getId(), "CREATED"));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getTree() {
        String cached = redisTemplate.opsForValue().get(TREE_KEY);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, new TypeReference<List<CategoryResponse>>() {
                });
            } catch (JacksonException e) {
                log.warn("分类树缓存损坏，回源重建", e);
            }
        }

        List<Category> all = categoryRepository.findAll();
        Map<Long, CategoryResponse> nodeMap = new HashMap<>();
        for (Category c : all) {
            nodeMap.put(c.getId(), toResponse(c));
        }
        List<CategoryResponse> root = new ArrayList<>();
        for (Category c : all) {
            CategoryResponse node = nodeMap.get(c.getId());
            if (c.getParent() != null && nodeMap.containsKey(c.getParent().getId())) {
                nodeMap.get(c.getParent().getId()).getChildren().add(node);
            } else {
                root.add(node);
            }
        }

        try {
            long ttl = TREE_BASE_TTL_SECONDS + ThreadLocalRandom.current().nextLong(TREE_TTL_JITTER_SECONDS);
            redisTemplate.opsForValue().set(TREE_KEY, objectMapper.writeValueAsString(root), ttl, TimeUnit.SECONDS);
        } catch (JacksonException ignored) {

        }
        return root;
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(404, "分类不存在"));
        if (postRepository.existsByCategoryId(categoryId)) {
            throw new BusinessException("该分类下还有帖子，禁止删除");
        }
        if (!category.getChildren().isEmpty()) {
            throw new BusinessException("该分类下还有子分类，禁止删除");
        }

        eventPublisher.publishEvent(new CategoryChangedEvent(categoryId, "DELETED"));
        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        return response;
    }
}
