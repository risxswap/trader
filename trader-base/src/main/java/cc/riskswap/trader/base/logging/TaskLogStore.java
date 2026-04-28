package cc.riskswap.trader.base.logging;

import cc.riskswap.trader.base.dao.TaskLogDao;

import java.time.LocalDateTime;

public class TaskLogStore {

    private final TaskLogDao taskLogDao;

    public TaskLogStore(TaskLogDao taskLogDao) {
        this.taskLogDao = taskLogDao;
    }

    public void writeRunning(String taskName, String taskGroup, LocalDateTime startTime, String traceId) {
        taskLogDao.createRunningLog(taskName, taskGroup, startTime, traceId);
    }

    public void writeFinished(String traceId, String status, Long costMs, String remark) {
        writeFinished(traceId, status, costMs, remark, null, null);
    }

    public void writeFinished(String traceId, String status, Long costMs, String remark, String content, String errorMsg) {
        taskLogDao.updateLogByTraceId(traceId, status, costMs, remark, content, errorMsg);
    }
}
