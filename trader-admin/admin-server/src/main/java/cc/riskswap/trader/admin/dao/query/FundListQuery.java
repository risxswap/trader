package cc.riskswap.trader.admin.dao.query;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class FundListQuery {
    private Integer pageNo = 1;
    private Integer pageSize = 20;
    private String keyword;
    private String market;
    private String management;
    private String custodian;
    private String fundType;
    private BigDecimal managementFeeMin;
    private BigDecimal managementFeeMax;
    private BigDecimal custodianFeeMin;
    private BigDecimal custodianFeeMax;
    private String sortBy;
    private String sortOrder;
}
