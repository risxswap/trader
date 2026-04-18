package cc.riskswap.trader.base.event;

import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

public class TraderStreamPublisher {
    public static final String STREAM_KEY = "trader:stream:sys";
    private final StringRedisTemplate stringRedisTemplate;

    public TraderStreamPublisher(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void publish(String eventType, Object payload) {
        Map<String, String> message = new HashMap<>();
        message.put("eventType", eventType);
        message.put("payload", JSONUtil.toJsonStr(payload));

        StringRecord record = StreamRecords.string(message).withStreamKey(STREAM_KEY);
        stringRedisTemplate.opsForStream().add(record);
    }
}