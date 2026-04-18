package cc.riskswap.trader.executor.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InvestmentLock {

    @Autowired
    private RedisLock redisLock;

    private static final String LOCK_PREFIX = "investment:";

    private static final long DEFAULT_EXPIRE_TIME = 20;

    public String getLockKey(Long investmentId) {
        return LOCK_PREFIX + investmentId;
    }

    /**
     * 尝试获取投资锁
     *
     * @param investmentId 投资ID
     * @param requestId    请求标识
     * @param expireTime   过期时间，单位秒
     * @return 是否获取成功
     */
    public boolean tryLock(Long investmentId, String requestId) {
        String lockKey = getLockKey(investmentId);
        return redisLock.tryLock(lockKey, requestId, DEFAULT_EXPIRE_TIME);
    }

    /**
     * 释放投资锁
     *
     * @param investmentId 投资ID
     * @param requestId    请求标识
     */
    public void unLock(Long investmentId, String requestId) {
        String lockKey = getLockKey(investmentId);
        redisLock.unlock(lockKey, requestId);
    }

    public String generateRequestId() {
        return redisLock.generateRequestId();
    }
}
