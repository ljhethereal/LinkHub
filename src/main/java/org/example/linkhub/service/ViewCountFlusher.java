package org.example.linkhub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.example.linkhub.repository.PostRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ViewCountFlusher {
    private static final Logger log = LoggerFactory.getLogger(ViewCountFlusher.class);
    private static final int BATCH_SIZE = 500;

    private final StringRedisTemplate redisTemplate;
    private final PostRepository postRepository;

    public ViewCountFlusher(StringRedisTemplate redisTemplate, PostRepository postRepository) {
        this.postRepository = postRepository;
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedDelay = 30_000)
    public void flushToDb() {
        List<String> ids = redisTemplate.opsForSet().pop(PostService.DIRTY_SET_KEY, BATCH_SIZE);
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (String idStr : ids) {
            try {
                Long postId = Long.parseLong(idStr);
                String deltaStr = redisTemplate.opsForValue()
                        .getAndDelete(PostService.PENDING_KEY_PREFIX + postId);
                if (deltaStr != null) {
                    long delta = Long.parseLong(deltaStr);
                    if (delta > 0) {
                        postRepository.incrementViews(postId, delta);
                    }
                }
                redisTemplate.delete(PostService.TOTAL_KEY_PREFIX + postId);
            } catch (Exception e) {
                log.error("浏览量回写失败，重新放入脏集合: {}", idStr, e);
                redisTemplate.opsForSet().add(PostService.DIRTY_SET_KEY, idStr);
            }
        }
    }
}
