package cc.riskswap.trader.collector.common.model.query;

import java.time.LocalDate;

import lombok.Data;

@Data
public class FundAdjQuery {

    private String code;

    private LocalDate tradeDate;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer pageNo;

    private Integer pageSize;
}
