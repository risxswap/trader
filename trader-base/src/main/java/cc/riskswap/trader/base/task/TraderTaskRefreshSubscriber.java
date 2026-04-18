package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.event.SystemTaskStatusEvent;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
public class TraderTaskRefreshSubscriber {

    private final String taskType;
    private final StringRedisTemplate stringRedisTemplate;
    private final TraderTaskSchedulerService schedulerService;

    public TraderTaskRefreshSubscriber(String taskType, StringRedisTemplate stringRedisTemplate, TraderTaskSchedulerService schedulerService) {
        this.taskType = taskType;
        this.stringRedisTemplate = stringRedisTemplate;
        this.schedulerService = schedulerService;
    }

    public void handle(String rawMessage) throws Exception {
        if (StrUtil.isBlank(rawMessage)) {
            return;
        }
        handle(JSONUtil.toBean(rawMessage, TraderTaskRefreshMessage.class));
    }

    public void handle(TraderTaskRefreshMessage message) throws Exception {
        if (message == null || message.taskType() == null || !taskType.equals(message.taskType())) {
            return;
        }
        log.info("Receive trader task refresh message eventType={} taskType={} taskCode={} version={}",
                message.eventType(), message.taskType(), message.taskCode(), message.version());
        if ("TASK_DELETED".equals(message.eventType())) {
            schedulerService.delete(message.taskCode());
            return;
        }
        String key = "trader:task:instances:" + message.taskType();
        Object jsonObj = stringRedisTemplate.opsForHash().get(key, message.taskCode());
        if (jsonObj != null) {
            SystemTaskStatusEvent task = JSONUtil.toBean(jsonObj.toString(), SystemTaskStatusEvent.class);
            if ("TASK_TRIGGER".equals(message.eventType())) {
                log.info("Dispatch TASK_TRIGGER to scheduler taskType={} taskCode={} taskName={}",
                        task.getTaskType(), task.getTaskCode(), task.getTaskName());
                schedulerService.trigger(task);
            } else {
                log.info("Dispatch TASK_UPDATED to scheduler taskType={} taskCode={} taskName={}",
                        task.getTaskType(), task.getTaskCode(), task.getTaskName());
                schedulerService.refresh(task);
            }
        }
    }
}
