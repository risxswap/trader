package cc.riskswap.trader.base.dao.query;

import lombok.Data;
import java.time.LocalDate;

@Data
public class FundNavListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String code;
    private LocalDate startTime;
    private LocalDate endTime;
}
