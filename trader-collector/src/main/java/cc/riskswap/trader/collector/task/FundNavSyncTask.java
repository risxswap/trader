package cc.riskswap.trader.collector.task;

import cc.riskswap.trader.base.logging.TraderTaskLog;
import cc.riskswap.trader.base.task.CollectorTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.collector.service.FundNavService;
import org.springframework.stereotype.Component;

@Component
public class FundNavSyncTask implements CollectorTask {

    private final FundNavService fundNavService;

    public FundNavSyncTask(FundNavService fundNavService) {
        this.fundNavService = fundNavService;
    }

    @Override
    public String getTaskCode() {
        return "collector.fundNav.sync";
    }

    @Override
    public String getTaskName() {
        return "基金净值同步任务";
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
    @TraderTaskLog("基金净值同步任务")
    public void execute(TraderTaskContext context) {
        fundNavService.syncFundNav(context);
    }
}
