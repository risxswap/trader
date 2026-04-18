package cc.riskswap.trader.admin.dao.entity;

import lombok.Data;

/**
 * 相关性重复版本业务键
 */
@Data
public class CorrelationDuplicateGroup {

    private String asset1;

    private String asset2;

    private String period;
}
