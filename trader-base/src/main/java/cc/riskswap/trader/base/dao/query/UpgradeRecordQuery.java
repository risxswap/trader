package cc.riskswap.trader.base.dao.query;

import cc.riskswap.trader.base.common.dto.PageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpgradeRecordQuery extends PageDto<Object> {
    private String version;
    private String status;
}
