package cc.riskswap.trader.base.task;

import java.time.OffsetDateTime;
import java.util.Map;

public class TraderTaskContext {

    private String appName;
    private String taskCode;
    private String taskName;
    private String triggerType;
    private String paramsJson;
    private Map<String, Object> paramsMap;
    private OffsetDateTime runAt;
    private String traceId;
    private TaskExecutionReport report;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getParamsJson() {
        return paramsJson;
    }

    public void setParamsJson(String paramsJson) {
        this.paramsJson = paramsJson;
    }

    public Map<String, Object> getParamsMap() {
        return paramsMap;
    }

    public void setParamsMap(Map<String, Object> paramsMap) {
        this.paramsMap = paramsMap;
    }

    public OffsetDateTime getRunAt() {
        return runAt;
    }

    public void setRunAt(OffsetDateTime runAt) {
        this.runAt = runAt;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public TaskExecutionReport getReport() {
        return report;
    }

    public void setReport(TaskExecutionReport report) {
        this.report = report;
    }

    public TaskExecutionReport report() {
        if (report == null) {
            report = new TaskExecutionReport();
        }
        return report;
    }
}
