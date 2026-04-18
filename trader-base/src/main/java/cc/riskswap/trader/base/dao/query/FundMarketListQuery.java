package cc.riskswap.trader.base.dao.query;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FundMarketListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String code;
    private LocalDate startDate;
    private LocalDate endDate;
}
