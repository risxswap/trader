package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.logging.TaskLogExecutionContext;
import cc.riskswap.trader.base.logging.TaskLogStore;
import cc.riskswap.trader.base.event.SystemTaskStatusEvent;
import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class TraderTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(TraderTaskExecutor.class);

    private final TraderTaskRegistry registry;
    private final StringRedisTemplate stringRedisTemplate;
    private final TraderTaskLock lock;
    private final SystemTaskStatusStore statusStore;
    private final TaskLogStore taskLogStore;

    public TraderTaskExecutor(
            TraderTaskRegistry registry,
            StringRedisTemplate stringRedisTemplate,
            TraderTaskLock lock,
            SystemTaskStatusStore statusStore,
            TaskLogStore taskLogStore
    ) {
        this.registry = registry;
        this.stringRedisTemplate = stringRedisTemplate;
        this.lock = lock;
        this.statusStore = statusStore;
        this.taskLogStore = taskLogStore;
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
        String traceId = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.now();
        long startedMs = System.currentTimeMillis();

        try {
            TaskLogExecutionContext.markExecutorManaged(true);
            if (taskLogStore != null) {
                taskLogStore.writeRunning(instance.getTaskName(), instance.getTaskCode(), startedAt, traceId);
            }
            log.info("Trigger trader task execution taskType={} taskCode={} taskName={} fireTimeEpochSec={} triggerType={}",
                    taskType, taskCode, instance.getTaskName(), fireTimeEpochSec, context.getTriggerType());
            sendStatus(instance, "RUNNING", null);
            task.execute(context);
            log.info("Trader task execution completed taskType={} taskCode={} taskName={} fireTimeEpochSec={}",
                    taskType, taskCode, instance.getTaskName(), fireTimeEpochSec);
            sendStatus(instance, "STOPPED", "SUCCESS");
            if (taskLogStore != null) {
                taskLogStore.writeFinished(traceId, "SUCCESS", System.currentTimeMillis() - startedMs,
                        buildTaskLogRemark(context, taskType, taskCode, "SUCCESS", null));
            }
        } catch (Exception e) {
            sendStatus(instance, "STOPPED", "FAILED");
            if (taskLogStore != null) {
                taskLogStore.writeFinished(traceId, "FAILED", System.currentTimeMillis() - startedMs,
                        buildTaskLogRemark(context, taskType, taskCode, "FAILED", e));
            }
            log.error("Trader task execution failed taskType={} taskCode={} taskName={} fireTimeEpochSec={}",
                    taskType, taskCode, instance.getTaskName(), fireTimeEpochSec, e);
            throw e;
        } finally {
            TaskLogExecutionContext.clear();
        }
    }

    private void sendStatus(SystemTaskStatusEvent instance, String status, String result) {
        instance.setStatus(status);
        instance.setResult(result);
        if (statusStore != null) {
            statusStore.writeStatus(instance.getTaskType(), instance.getTaskCode(), status, result, instance.getVersion());
        }
    }

    private String buildTaskLogRemark(TraderTaskContext context, String taskType, String taskCode, String status, Exception e) {
        StringBuilder builder = new StringBuilder();
        builder.append("triggerType=").append(context.getTriggerType())
                .append(", taskType=").append(taskType)
                .append(", taskCode=").append(taskCode)
                .append(", status=").append(status)
                .append(", paramsJson=").append(context.getParamsJson());
        if (e != null) {
            builder.append(", error=").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
        }
        return builder.toString();
    }
}
