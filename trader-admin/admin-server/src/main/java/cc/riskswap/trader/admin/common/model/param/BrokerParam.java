package cc.riskswap.trader.admin.common.model.param;

import lombok.Data;
import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;

@Data
public class BrokerParam {
    private Integer id;
    
    @NotBlank(message = "名字不能为空")
    private String name;
    
    @NotBlank(message = "代号不能为空")
    private String code;
    
    private BigDecimal initialCapital;
    private BigDecimal currentCapital;
    private String intro;
    private String remark;
}
