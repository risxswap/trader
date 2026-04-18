package cc.riskswap.trader.admin.common.model.dto;

import java.util.List;

import lombok.Data;

@Data
public class ExecutorInfoDto {

    private String executorId;

    private List<StrategyInfoDto> strategies;

}
