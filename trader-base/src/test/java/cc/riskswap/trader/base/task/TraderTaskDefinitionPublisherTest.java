package cc.riskswap.trader.base.task;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

class TraderTaskDefinitionPublisherTest {

    @Test
    void should_publish_definition_to_redis() {
        StringRedisTemplate redisTemplate = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(ops);

        TraderTask task = new SampleCollectorTask();
        TraderTaskRegistry registry = new TraderTaskRegistry(List.of(task));

        TraderTaskDefinitionPublisher publisher = new TraderTaskDefinitionPublisher(redisTemplate, "node-1", "COLLECTOR");
        publisher.publishAll(registry);

        Mockito.verify(ops).set(Mockito.eq("trader:task:def:COLLECTOR:fundSync"), Mockito.argThat(json -> {
            return JSONUtil.isTypeJSON(json) && JSONUtil.parseObj(json).getStr("taskCode").equals("fundSync");
        }));
    }

    private static class SampleCollectorTask implements CollectorTask {
        @Override public String getTaskCode() { return "fundSync"; }
        @Override public String getTaskName() { return "同步基金"; }
        @Override public boolean defaultEnabled() { return true; }
        @Override public String getParamSchema() { return "{\"type\":\"object\"}"; }
        @Override public String getDefaultParams() { return "{\"fullSync\":true}"; }
        @Override public void execute(TraderTaskContext context) { }
    }
}
