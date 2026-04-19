package cc.riskswap.trader.base.task;

import cc.riskswap.trader.base.dao.SystemTaskDao;
import cc.riskswap.trader.base.dao.entity.SystemTask;

import java.time.OffsetDateTime;

public class SystemTaskStatusStore {

    private final SystemTaskDao systemTaskDao;

    public SystemTaskStatusStore(SystemTaskDao systemTaskDao) {
        this.systemTaskDao = systemTaskDao;
    }

    public void writeStatus(String taskType, String taskCode, String status, String result, Long version) {
        SystemTask task = systemTaskDao.getByTaskTypeAndTaskCode(taskType, taskCode);
        if (task == null) {
            return;
        }
        task.setStatus(status);
        task.setResult(result);
        task.setVersion(version);
        task.setUpdatedAt(OffsetDateTime.now());
        systemTaskDao.updateById(task);
    }
}
