package cc.riskswap.trader.admin.dao.query;

import cc.riskswap.trader.admin.dao.dto.PageDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UpgradeRecordQuery extends PageDto<Object> {
    private String version;
    private String status;
}
