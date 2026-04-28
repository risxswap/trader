package cc.riskswap.trader.collector.task;

import org.springframework.stereotype.Component;

import cc.riskswap.trader.base.logging.TraderTaskLog;
import cc.riskswap.trader.base.task.CollectorTask;
import cc.riskswap.trader.base.task.TraderTaskContext;
import cc.riskswap.trader.collector.service.FundMarketService;

@Component
public class FundMarketSyncTask implements CollectorTask {

    private final FundMarketService fundMarketService;

    public FundMarketSyncTask(FundMarketService fundMarketService) {
        this.fundMarketService = fundMarketService;
    }

    @Override
    public String getTaskCode() {
        return "collector.fundMarket.sync";
    }

    @Override
    public String getTaskName() {
        return "基金行情同步任务";
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
    @TraderTaskLog("基金行情同步任务")
    public void execute(TraderTaskContext context) {
        fundMarketService.syncFundMarket(context);
    }
}
