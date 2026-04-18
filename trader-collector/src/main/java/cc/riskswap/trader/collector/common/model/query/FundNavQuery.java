package cc.riskswap.trader.collector.common.model.query;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FundNavQuery {

    private String code;

    private LocalDate navDate;

    private String market;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer pageNo;

    private Integer pageSize;
}
