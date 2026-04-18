package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.event.SystemTaskStatusEvent;
import cc.riskswap.trader.base.event.TraderStreamPublisher;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.OffsetDateTime;
import java.util.Map;

public class TraderTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(TraderTaskExecutor.class);

    private final TraderTaskRegistry registry;
    private final StringRedisTemplate stringRedisTemplate;
    private final TraderTaskLock lock;
    private final TraderStreamPublisher streamPublisher;

    public TraderTaskExecutor(TraderTaskRegistry registry, StringRedisTemplate stringRedisTemplate, TraderTaskLock lock, TraderStreamPublisher streamPublisher) {
        this.registry = registry;
        this.stringRedisTemplate = stringRedisTemplate;
        this.lock = lock;
        this.streamPublisher = streamPublisher;
    }

    public void execute(String taskType, String taskCode, long fireTimeEpochSec) throws Exception {
        String key = "trader:task:instances:" + taskType;
        Object jsonObj = stringRedisTemplate.opsForHash().get(key, taskCode);
        if (jsonObj == null) {
            log.warn("Skip trader task execution because task instance is missing taskType={} taskCode={} fireTimeEpochSec={}",
                    taskType, taskCode, fireTimeEpochSec);
            return;
        }
        SystemTaskStatusEvent instance = JSONUtil.toBean(jsonObj.toString(), SystemTaskStatusEvent.class);
        if (Boolean.FALSE.equals(instance.getEnabled())) {
            log.info("Skip trader task execution because task is disabled taskType={} taskCode={} fireTimeEpochSec={}",
                    taskType, taskCode, fireTimeEpochSec);
            return;
        }
        String lockKey = "task:run:" + taskType + ":" + taskCode + ":" + fireTimeEpochSec;
        String requestId = lock.newRequestId();
        if (!lock.tryLock(lockKey, requestId)) {
            log.info("Skip trader task execution because lock was not acquired taskType={} taskCode={} fireTimeEpochSec={} lockKey={}",
                    taskType, taskCode, fireTimeEpochSec, lockKey);
            return;
        }
        TraderTask task = registry.getTask(taskCode);
        if (task == null) {
            log.warn("Skip trader task execution because task bean is missing taskType={} taskCode={} fireTimeEpochSec={}",
                    taskType, taskCode, fireTimeEpochSec);
            return;
        }
        TraderTaskContext context = new TraderTaskContext();
        context.setAppName(taskType);
        context.setTaskCode(taskCode);
        context.setTaskName(instance.getTaskName());
        context.setTriggerType(TraderTaskTriggerType.SCHEDULED.name());
        context.setParamsJson(instance.getParamsJson() == null ? "{}" : instance.getParamsJson());
        @SuppressWarnings("unchecked")
        Map<String, Object> paramsMap = JSONUtil.parseObj(context.getParamsJson()).toBean(Map.class);
        context.setParamsMap(paramsMap);
        context.setRunAt(OffsetDateTime.now());
        
        try {
            log.info("Trigger trader task execution taskType={} taskCode={} taskName={} fireTimeEpochSec={} triggerType={}",
                    taskType, taskCode, instance.getTaskName(), fireTimeEpochSec, context.getTriggerType());
            sendStatus(instance, "RUNNING");
            task.execute(context);
            log.info("Trader task execution completed taskType={} taskCode={} taskName={} fireTimeEpochSec={}",
                    taskType, taskCode, instance.getTaskName(), fireTimeEpochSec);
        } catch (Exception e) {
            sendStatus(instance, "ERROR");
            log.error("Trader task execution failed taskType={} taskCode={} taskName={} fireTimeEpochSec={}",
                    taskType, taskCode, instance.getTaskName(), fireTimeEpochSec, e);
            throw e;
        } finally {
            sendStatus(instance, "IDLE");
        }
    }

    private void sendStatus(SystemTaskStatusEvent instance, String status) {
        instance.setStatus(status);
        if (streamPublisher != null) {
            streamPublisher.publish("SYSTEM_TASK_STATUS", instance);
        }
    }
}
