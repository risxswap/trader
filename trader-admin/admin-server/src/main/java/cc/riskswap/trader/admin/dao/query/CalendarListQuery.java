package cc.riskswap.trader.admin.dao.query;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CalendarListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String exchange;
    private LocalDate startDate;
    private LocalDate endDate;
}
