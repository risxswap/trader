package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardDto {
    /**
     * 总资产
     */
    private BigDecimal totalAsset;
    
    /**
     * 今日变动
     */
    private BigDecimal todayChange;
    
    /**
     * 累计盈亏
     */
    private BigDecimal totalProfit;
    
    /**
     * 胜率 (e.g. "65.5%")
     */
    private String winRate;
    
    /**
     * 最大回撤 (e.g. "-15.2%")
     */
    private String maxDrawdown;
    
    /**
     * 持仓数量
     */
    private Integer holdingCount;
    
    /**
     * 风险指标 (暂无数据)
     */
    private String riskIndicator;
    
    /**
     * 资产走势 (X:日期, Y:资产)
     */
    private List<ChartDataDto> assetTrend;
    
    /**
     * 资产分布 (X:名称, Y:占比)
     */
    private List<ChartDataDto> assetDistribution;
}
