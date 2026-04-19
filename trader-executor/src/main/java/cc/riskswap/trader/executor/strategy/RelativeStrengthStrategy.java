package cc.riskswap.trader.executor.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cc.riskswap.trader.executor.common.ExecutorContext;
import cc.riskswap.trader.base.strategy.config.RelativeStrengthStrategyConfig;
import cc.riskswap.trader.base.dao.CorrelationDao;
import cc.riskswap.trader.base.dao.FundDao;
import cc.riskswap.trader.base.dao.FundNavDao;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class RelativeStrengthStrategy extends BaseStrategy<RelativeStrengthStrategyConfig> {

    static {
        name = "RelativeStrengthStrategy";
        desc = "相对强度策略";
    }
    @Autowired
    private FundDao fundDao;

    @Autowired
    private CorrelationDao correlationDao;

    @Autowired
    private FundNavDao fundNavDao;

    @Override
    public String getTaskCode() {
        return "relativeStrengthStrategy";
    }

    @Override
    public String getTaskName() {
        return desc;
    }

    @Override
    public boolean defaultEnabled() {
        return false;
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
    public void run(ExecutorContext context) {
        
    }

}
