package cc.riskswap.trader.collector.common.model.query;

import java.time.LocalDate;

import lombok.Data;

@Data
public class CalendarQuery {
    
    private String exchange;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer isOpen;
}
