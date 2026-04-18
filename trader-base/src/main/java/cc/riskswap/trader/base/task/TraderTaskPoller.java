package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.event.SystemTaskStatusEvent;
import cn.hutool.json.JSONUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.HashMap;
import java.util.Map;

public class TraderTaskPoller {

    private final String taskType;
    private final StringRedisTemplate stringRedisTemplate;
    private final TraderTaskSchedulerService schedulerService;
    private final Map<String, Long> versionCache = new HashMap<>();

    public TraderTaskPoller(String taskType, StringRedisTemplate stringRedisTemplate, TraderTaskSchedulerService schedulerService) {
        this.taskType = taskType;
        this.stringRedisTemplate = stringRedisTemplate;
        this.schedulerService = schedulerService;
    }

    public void poll() throws Exception {
        String key = "trader:task:instances:" + taskType;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(key);
        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String taskCode = (String) entry.getKey();
            String json = (String) entry.getValue();
            SystemTaskStatusEvent task = JSONUtil.toBean(json, SystemTaskStatusEvent.class);
            
            Long currentVersion = versionCache.get(task.getTaskCode());
            if (currentVersion == null || !currentVersion.equals(task.getVersion())) {
                schedulerService.refresh(task);
                versionCache.put(task.getTaskCode(), task.getVersion());
            }
        }
    }
}
