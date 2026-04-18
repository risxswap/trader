package cc.riskswap.trader.base.dao.param;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 投资参数
 */
@Data
public class InvestmentParam {
    private Integer id;
    private String name;
    private String groupName;
    private String targetType;
    private String investType;
    private Integer brokerId;
    private List<String> targets;
    private BigDecimal budget;
    private String strategy;
    private String strategyConfig;
    private String cron;
    private String executorId;
    private String status;
}
