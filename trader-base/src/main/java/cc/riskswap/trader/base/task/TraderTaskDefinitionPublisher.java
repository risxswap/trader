package cc.riskswap.trader.base.task;

import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.OffsetDateTime;
import java.util.Map;

public class TraderTaskDefinitionPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final String nodeId;
    private final String nodeType;

    public TraderTaskDefinitionPublisher(StringRedisTemplate stringRedisTemplate, String nodeId, String nodeType) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
    }

    public void publishAll(TraderTaskRegistry registry) {
        for (Map.Entry<String, TraderTask> entry : registry.getTaskMap().entrySet()) {
            TraderTask task = entry.getValue();
            TraderTaskType taskType = toTaskType(task);
            TraderTaskDefinition definition = new TraderTaskDefinition();
            definition.setTaskType(taskType.name());
            definition.setTaskCode(task.getTaskCode());
            definition.setTaskName(task.getTaskName());
            definition.setDefaultEnabled(task.defaultEnabled());
            definition.setParamSchema(task.getParamSchema());
            definition.setDefaultParamsJson(task.getDefaultParams());
            definition.setImplClass(task.getClass().getName());
            definition.setReportNodeId(nodeId);
            definition.setReportNodeType(nodeType);
            definition.setReportAt(OffsetDateTime.now());
            stringRedisTemplate.opsForValue().set(key(taskType, task.getTaskCode()), JSONUtil.toJsonStr(definition));
        }
    }

    private String key(TraderTaskType taskType, String taskCode) {
        return "trader:task:def:" + taskType.name() + ":" + taskCode;
    }

    private TraderTaskType toTaskType(TraderTask task) {
        if (task instanceof CollectorTask) {
            return TraderTaskType.COLLECTOR;
        }
        if (task instanceof StatisticTask) {
            return TraderTaskType.STATISTIC;
        }
        return TraderTaskType.STRATEGY;
    }
}
