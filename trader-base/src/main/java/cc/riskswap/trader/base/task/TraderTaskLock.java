package cc.riskswap.trader.base.task;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class TraderTaskLock {

    private final StringRedisTemplate stringRedisTemplate;
    private final long expireSeconds;

    public TraderTaskLock(StringRedisTemplate stringRedisTemplate, long expireSeconds) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.expireSeconds = expireSeconds;
    }

    public String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public boolean tryLock(String key, String requestId) {
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent("lock:" + key, requestId, expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(result);
    }
}
