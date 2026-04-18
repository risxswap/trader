package cc.riskswap.trader.base.task;

import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

public class TraderTaskRefreshPublisher {

    public static final String TOPIC = "trader:task:refresh";

    private final StringRedisTemplate stringRedisTemplate;

    public TraderTaskRefreshPublisher(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void publish(TraderTaskRefreshMessage message) {
        stringRedisTemplate.convertAndSend(TOPIC, JSONUtil.toJsonStr(message));
    }
}
