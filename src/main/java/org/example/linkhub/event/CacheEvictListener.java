package org.example.linkhub.event;

import org.example.linkhub.service.CategoryService;
import org.example.linkhub.service.PostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CacheEvictListener {
    private static final Logger log = LoggerFactory.getLogger(CacheEvictListener.class);

    private final StringRedisTemplate redisTemplate;

    public CacheEvictListener(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostChanged(PostChangedEvent event) {
        Long postId = event.postId();
        redisTemplate.delete(PostService.DETAIL_KEY_PREFIX + postId);
        if (PostChangedEvent.DELETED.equals(event.action())) {
            redisTemplate.delete(PostService.PENDING_KEY_PREFIX + postId);
            redisTemplate.delete(PostService.TOTAL_KEY_PREFIX + postId);
            redisTemplate.opsForSet().remove(PostService.DIRTY_SET_KEY, String.valueOf(postId));
        }
        log.debug("缓存失效: post {} {}", postId, event.action());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCategoryChanged(CategoryChangedEvent event) {
        redisTemplate.delete(CategoryService.TREE_KEY);
        log.debug("缓存失效: category {} {}", event.categoryId(), event.action());
    }
}
