package cc.riskswap.trader.base.task;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

class TraderTaskLockTest {

    @Test
    void should_try_lock_by_set_if_absent() {
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(ops);
        Mockito.when(ops.setIfAbsent(Mockito.eq("lock:task:run:COLLECTOR:fundSync:1"), Mockito.anyString(), Mockito.eq(600L), Mockito.eq(TimeUnit.SECONDS)))
                .thenReturn(true);

        TraderTaskLock lock = new TraderTaskLock(redisTemplate, 600);
        boolean locked = lock.tryLock("task:run:COLLECTOR:fundSync:1", "rid");
        Assertions.assertTrue(locked);
    }
}
