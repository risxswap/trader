package cc.riskswap.trader.admin.common.model.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SystemTaskUpdateParam {
    @NotNull
    private Long id;
    @NotBlank
    private String cron;
    private Boolean enabled;
    private String status;
    private String paramsJson;
    private String remark;
}
