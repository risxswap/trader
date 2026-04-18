package cc.riskswap.trader.admin.common.model.query;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FundListQuery {
    @NotNull
    @Min(1)
    private Integer pageNo;
    @NotNull
    @Min(1)
    private Integer pageSize;
    private String keyword;
    private String sortBy;
    private String sortOrder;
    private String market;
    private String management;
    private String custodian;
    private String fundType;
    private Double managementFeeMin;
    private Double managementFeeMax;
    private Double custodianFeeMin;
    private Double custodianFeeMax;
}
