package cc.riskswap.trader.executor.pubsub.publisher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.ObjectUtils;

import cc.riskswap.trader.executor.pubsub.message.BaseMessage;
import cn.hutool.json.JSONUtil;

/**
 * 基础消息发布者
 */
@Slf4j
public abstract class BasePublisher<T extends BaseMessage> {

    @Autowired
    protected StringRedisTemplate stringRedisTemplate;

    /**
     * 获取频道名称
     *
     * @return 频道名称
     */
    protected abstract String getChannel();

    /**
     * 发布消息
     *
     * @param message 消息内容
     */
    public void publish(T message) {
        String topic = getChannel();
        if (ObjectUtils.isEmpty(topic)) {
            log.error("Topic is null, cannot publish message: {}", message);
            return;
        }
        if (ObjectUtils.isEmpty(message)) {
            log.error("Message is null, cannot publish message to topic: {}", topic);
            return;
        }
        try {
            String messageJson = JSONUtil.toJsonStr(message);
            stringRedisTemplate.convertAndSend(topic, messageJson);
            log.info("Published message to topic {}: {}", topic, messageJson);
        } catch (Exception e) {
            log.error("Failed to publish message to topic {}: {}", topic, message, e);
        }
    }
}
