package cc.riskswap.trader.admin.common.model.dto;

import java.util.List;

import lombok.Data;

@Data
public class StrategiesDto {
    private String version;
    private String lastUpdated;
    private List<StrategyInfoDto> items;
}
