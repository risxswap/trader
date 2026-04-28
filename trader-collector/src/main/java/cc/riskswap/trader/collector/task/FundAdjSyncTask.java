package cc.riskswap.trader.collector.task;

import cc.riskswap.trader.base.logging.TraderTaskLog;
import cc.riskswap.trader.base.task.CollectorTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.collector.service.FundAdjService;
import org.springframework.stereotype.Component;

@Component
public class FundAdjSyncTask implements CollectorTask {

    private final FundAdjService fundAdjService;

    public FundAdjSyncTask(FundAdjService fundAdjService) {
        this.fundAdjService = fundAdjService;
    }

    @Override
    public String getTaskCode() {
        return "collector.fundAdj.sync";
    }

    @Override
    public String getTaskName() {
        return "基金复权同步任务";
    }

    @Override
    public boolean defaultEnabled() {
        return true;
    }

    @Override
    public String getParamSchema() {
        return "{}";
    }

    @Override
    public String getDefaultParams() {
        return "{}";
    }

    @Override
    @TraderTaskLog("基金复权同步任务")
    public void execute(TraderTaskContext context) {
        fundAdjService.syncFundAdj(context);
    }
}
