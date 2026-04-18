package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class UpgradeRecordDetailDto extends UpgradeRecordDto {
    private List<UpgradeStepDto> steps;
}
