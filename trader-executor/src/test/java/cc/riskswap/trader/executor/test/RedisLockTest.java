package cc.riskswap.trader.executor.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.executor.Application;
import cc.riskswap.trader.executor.lock.RedisLock;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest(classes = Application.class)
public class RedisLockTest {

    @Autowired
    private RedisLock redisLock;

    @Test
    public void testLock() {
        String key = "test_key";
        String requestId = redisLock.generateRequestId();
        long expireTime = 10;

        // Clean up before test
        try {
            redisLock.unlock(key, requestId);
        } catch (Exception e) {
            // ignore
        }
        
        // 1. 尝试获取锁
        log.info("Trying to acquire lock with key: {}, requestId: {}", key, requestId);
        boolean locked = redisLock.tryLock(key, requestId, expireTime);
        log.info("Acquired lock result: {}", locked);
        Assertions.assertTrue(locked, "Should acquire lock successfully");

        // 2. 再次尝试获取锁（应该失败）
        String requestId2 = redisLock.generateRequestId();
        log.info("Trying to acquire lock again with key: {}, requestId: {}", key, requestId2);
        boolean lockedAgain = redisLock.tryLock(key, requestId2, expireTime);
        log.info("Acquired lock again result: {}", lockedAgain);
        Assertions.assertFalse(lockedAgain, "Should fail to acquire lock again");

        // 3. 释放锁
        log.info("Trying to release lock with key: {}, requestId: {}", key, requestId);
        boolean unlocked = redisLock.unlock(key, requestId);
        log.info("Released lock result: {}", unlocked);
        Assertions.assertTrue(unlocked, "Should release lock successfully");

        // 4. 再次获取锁（应该成功）
        String requestId3 = redisLock.generateRequestId();
        log.info("Trying to acquire lock after unlock with key: {}, requestId: {}", key, requestId3);
        boolean lockedAfterUnlock = redisLock.tryLock(key, requestId3, expireTime);
        log.info("Acquired lock after unlock result: {}", lockedAfterUnlock);
        Assertions.assertTrue(lockedAfterUnlock, "Should acquire lock after unlock");
        
        // Cleanup
        redisLock.unlock(key, requestId3);
    }
}
