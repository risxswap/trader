package cc.riskswap.trader.collector.task;

import org.springframework.stereotype.Component;

import cc.riskswap.trader.base.logging.TraderTaskLog;
import cc.riskswap.trader.base.task.CollectorTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.collector.service.FundService;

@Component
public class FundSyncTask implements CollectorTask {

    private final FundService fundService;

    public FundSyncTask(FundService fundService) {
        this.fundService = fundService;
    }

    @Override
    public String getTaskCode() {
        return "collector.fund.sync";
    }

    @Override
    public String getTaskName() {
        return "基金同步任务";
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
    @TraderTaskLog("基金同步任务")
    public void execute(TraderTaskContext context) {
        fundService.syncFund();
    }
}
