package cc.riskswap.trader.base.task;

import java.util.Map;

public class TaskExecutionReportJson {

    private Long syncedCount;
    private Long failedCount;
    private String message;
    private Map<String, Object> errorDetail;

    public Long getSyncedCount() {
        return syncedCount;
    }

    public void setSyncedCount(Long syncedCount) {
        this.syncedCount = syncedCount;
    }

    public Long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Long failedCount) {
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

    public void setErrorDetail(Map<String, Object> errorDetail) {
        this.errorDetail = errorDetail;
    }
}

