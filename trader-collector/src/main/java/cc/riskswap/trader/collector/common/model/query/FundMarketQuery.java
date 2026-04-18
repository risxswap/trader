package cc.riskswap.trader.collector.common.model.query;

import java.time.LocalDate;

import lombok.Data;

@Data
public class FundMarketQuery {

    private Integer pageNo;

    private Integer pageSize;

    private String code;

    private LocalDate tradeDate;

    private LocalDate startDate;

    private LocalDate endDate;
}
