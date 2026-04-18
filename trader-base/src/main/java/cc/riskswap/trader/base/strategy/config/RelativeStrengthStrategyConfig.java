package cc.riskswap.trader.base.strategy.config;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RelativeStrengthStrategyConfig extends BaseStrategyConfig {

    @JsonPropertyDescription("调仓周期")
    @NotBlank
    private String rebalanceCycle;

    @JsonPropertyDescription("排名")
    private Integer topNum;
}
