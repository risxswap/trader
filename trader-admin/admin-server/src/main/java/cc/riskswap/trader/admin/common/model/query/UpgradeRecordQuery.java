package cc.riskswap.trader.admin.common.model.query;

import cc.riskswap.trader.admin.common.model.dto.PageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpgradeRecordQuery extends PageDto<Object> {
    private String version;
    private String status;
}
