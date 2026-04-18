package cc.riskswap.trader.base.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 基金市场数据实体类
 */
@Data
@TableName("fund_market")
public class FundMarket {

    /**
     * TS代码
     */
    private String symbol;
    
    /**
     * 交易日期
     */
    private OffsetDateTime time;
    
    /**
     * 开盘价(元)
     */
    private BigDecimal open;

    /**
     * 最高价(元)
     */
    private BigDecimal high;

    /**
     * 最低价(元)
     */
    private BigDecimal low;

    /**
     * 收盘价(元)
     */
    private BigDecimal close;
    
    /**
     * 昨收盘价(元)
     */
    private BigDecimal preClose;
    
    /**
     * 涨跌额(元)
     */
    private BigDecimal change;
    
    /**
     * 涨跌幅(%)
     */
    private BigDecimal pctChg;
    
    /**
     * 成交量(手)
     */
    private BigDecimal vol;
    
    /**
     * 成交额(千元)
     */
    private BigDecimal amount;
    
    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
    
    /**
     * 时间周期
     */
    private String timeFrame;
}