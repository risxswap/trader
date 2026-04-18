package cc.riskswap.trader.executor.lock;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis分布式锁工具类
 */
@Slf4j
@Component
public class RedisLock {

    private static final String LOCK_PREFIX = "lock:";
    private static final Long RELEASE_SUCCESS = 1L;

    private final StringRedisTemplate stringRedisTemplate;

    public RedisLock(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 尝试获取锁
     *
     * @param key        锁的键值 (不包含前缀)
     * @param requestId  请求标识，用于解锁时的校验
     * @param expireTime 过期时间，单位秒
     * @return 是否获取成功
     */
    @SuppressWarnings("null")
    public boolean tryLock(String key, String requestId, long expireTime) {
        String lockKey = LOCK_PREFIX + key;
        try {
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, requestId, expireTime, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.error("Failed to acquire lock for key: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 释放锁
     *
     * @param key       锁的键值 (不包含前缀)
     * @param requestId 请求标识，必须与加锁时一致
     * @return 是否释放成功
     */
    @SuppressWarnings("null")
    public boolean unlock(String key, String requestId) {
        String lockKey = LOCK_PREFIX + key;
        // Lua脚本保证原子性：先判断value是否一致，一致则删除
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        
        try {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptText(script);
            redisScript.setResultType(Long.class);
            
            Long result = stringRedisTemplate.execute(redisScript, Collections.singletonList(lockKey), requestId);
            return RELEASE_SUCCESS.equals(result);
        } catch (Exception e) {
            log.error("Failed to release lock for key: {}", lockKey, e);
            return false;
        }
    }

    /**
     * 生成唯一的请求标识
     * @return UUID字符串
     */
    public String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
