package cc.riskswap.trader.admin.dao.base.param;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class NodeGroupParam {

    private Long id;

    @NotBlank(message = "分组名称不能为空")
    private String name;

    @NotBlank(message = "分组编码不能为空")
    private String code;

    private Integer sort;
}
