package cc.riskswap.trader.admin.common.model.query;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class FundAdjListQuery {
    @NotNull
    @Min(1)
    private Integer pageNo;
    @NotNull
    @Min(1)
    private Integer pageSize;
    private String code;
    private LocalDate startDate;
    private LocalDate endDate;
}
