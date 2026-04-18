package cc.riskswap.trader.base.dao.query;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FundMarketQuery {
    @NotBlank
    private String code;
    @NotNull
    private LocalDate startDate;
    @NotNull
    private LocalDate endDate;
}
