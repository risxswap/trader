package cc.riskswap.trader.base.task;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TraderTaskRegistry {

    private final Map<String, TraderTask> taskMap = new LinkedHashMap<>();

    public TraderTaskRegistry(List<TraderTask> tasks) {
        for (TraderTask task : tasks) {
            String code = task.getTaskCode();
            if (taskMap.containsKey(code)) {
                throw new IllegalStateException("Duplicate trader task code: " + code);
            }
            taskMap.put(code, task);
        }
    }

    public TraderTask getTask(String taskCode) {
        return taskMap.get(taskCode);
    }

    public Map<String, TraderTask> getTaskMap() {
        return Collections.unmodifiableMap(taskMap);
    }
}
