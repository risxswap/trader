package cc.riskswap.trader.base.dao.base.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NodeParam {

    @NotNull(message = "节点ID不能为空")
    private Long id;

    @NotBlank(message = "节点名称不能为空")
    private String nodeName;

    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    @NotNull(message = "节点分组不能为空")
    private Long nodeGroupId;

    private String hostname;

    private String primaryIp;

    private String remark;
}
