package cc.riskswap.trader.base.task;

import java.util.LinkedHashMap;
import java.util.Map;

public class TaskExecutionReport {

    private long syncedCount;
    private long failedCount;
    private String message;
    private final Map<String, Object> errorDetail = new LinkedHashMap<>();

    public long getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(long syncedCount) {
        this.syncedCount = syncedCount;
    }

    public long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(long failedCount) {
        this.failedCount = failedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getErrorDetail() {
        return errorDetail;
    }

    public void addSynced(long delta) {
        this.syncedCount += delta;
    }

    public void addFailed(long delta) {
        this.failedCount += delta;
    }

    public void putErrorDetail(String key, Object value) {
        if (key == null || key.isBlank()) {
            return;
        }
        this.errorDetail.put(key, value);
    }

    public TaskExecutionReportJson toJson() {
        TaskExecutionReportJson json = new TaskExecutionReportJson();
        json.setSyncedCount(syncedCount);
        json.setFailedCount(failedCount);
        json.setMessage(message);
        json.setErrorDetail(errorDetail.isEmpty() ? null : new LinkedHashMap<>(errorDetail));
        return json;
    }
}

