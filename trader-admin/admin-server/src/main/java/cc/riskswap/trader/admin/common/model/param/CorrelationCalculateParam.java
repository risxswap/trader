package cc.riskswap.trader.admin.common.model.param;

import lombok.Data;

/**
 * 基金相关性计算参数
 */
@Data
public class CorrelationCalculateParam {
    /**
     * 标的1
     */
    private String asset1;
    
    /**
     * 标的2
     */
    private String asset2;
    
    /**
     * 时间周期 (e.g., 30D, 1Y)
     */
    private String period;
}
