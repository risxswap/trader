package cc.riskswap.trader.admin.common.model.query;

import java.time.LocalDate;

import lombok.Data;

@Data
public class FundNavListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    
    /**
     * 基金代码
     */
    private String code;
    
    /**
     * 开始日期 yyyy-MM-dd
     */
    private LocalDate startTime;
    
    /**
     * 结束日期 yyyy-MM-dd
     */
    private LocalDate endTime;
}
