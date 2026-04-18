package cc.riskswap.trader.admin.common.model.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemTaskInstanceCreateParam {
    @NotBlank
    private String taskType;
    @NotBlank
    private String taskCode;
    @NotBlank
    private String taskName;
    @NotBlank
    private String cron;
    private Boolean enabled;
    @NotBlank
    private String status;
    private String paramsJson;
    private String remark;
}
