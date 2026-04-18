package cc.riskswap.trader.admin.common.model.query;

import java.time.LocalDate;

import lombok.Data;

@Data
public class CalendarListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private Integer year;
    private String exchange;
    private LocalDate startDate;
    private LocalDate endDate;
}
