package cc.riskswap.trader.base.dao.entity;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class FundAdj {

    /**
     * 基金代码
     */
    private String symbol;

    /**
     * 日期
     */
    private OffsetDateTime time;

    /**
     * 调整因子
     */
    private Double adjFactor;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
    
}
