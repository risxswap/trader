package cc.riskswap.trader.collector.common.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * 市场数据视图对象
 * 用于前端展示的数据格式
 */
@Data
public class MarketDataDto {

    /**
     * 时间戳
     */
    private Instant timestamp;

    /**
     * 交易对符号
     */
    private String symbol;

    /**
     * 交易所
     */
    private String exchange;

    /**
     * 数据类型
     */
    private String dataType;

    /**
     * 开盘价
     */
    private BigDecimal openPrice;

    /**
     * 最高价
     */
    private BigDecimal highPrice;

    /**
     * 最低价
     */
    private BigDecimal lowPrice;

    /**
     * 收盘价
     */
    private BigDecimal closePrice;

    /**
     * 成交量
     */
    private BigDecimal volume;

    /**
     * 成交额
     */
    private BigDecimal amount;

    /**
     * 24小时价格变化
     */
    private BigDecimal priceChange24h;

    /**
     * 24小时价格变化百分比
     */
    private BigDecimal priceChangePercent24h;

    /**
     * 格式化的时间字符串
     */
    private String formattedTime;

    /**
     * 价格变化趋势（up/down/flat）
     */
    private String trend;
}