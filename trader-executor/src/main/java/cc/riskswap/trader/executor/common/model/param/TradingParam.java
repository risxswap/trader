package cc.riskswap.trader.executor.common.model.param;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class TradingParam {

    /**
     * 标的
     */
    private String symbol;
    
    /**
     * 标的类型
     */
    private String type;

    /**
     * 是否全仓卖出
     */
    private boolean saleAll;

    /**
     * 数量，正负标识方向
     */
    private BigDecimal volume;

    /**
     * 价格
     */
    private BigDecimal price;
}