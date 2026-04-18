package cc.riskswap.trader.admin.dao.base.param;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class NodeApproveParam {

    @NotNull(message = "节点ID不能为空")
    private Long id;

    @NotNull(message = "节点分组不能为空")
    private Long nodeGroupId;
}
