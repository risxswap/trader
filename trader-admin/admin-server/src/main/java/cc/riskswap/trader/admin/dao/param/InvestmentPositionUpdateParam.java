package cc.riskswap.trader.admin.dao.param;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

@Data
public class InvestmentPositionUpdateParam {

    @NotNull(message = "ID不能为空")
    private Integer id;

    /**
     * 投资ID
     */
    @NotNull(message = "投资ID不能为空")
    private Integer investmentId;

    /**
     * 投资日志ID
     */
    private Integer investmentLogId;

    /**
     * 标的
     */
    @NotBlank(message = "标的不能为空")
    private String asset;

    /**
     * 标的类型
     */
    @NotBlank(message = "类型不能为空")
    private String assetType;

    /**
     * 持仓数量
     */
    @NotNull(message = "数量不能为空")
    private BigDecimal quantity;

    /**
     * 买入价
     */
    @NotNull(message = "买入价不能为空")
    private BigDecimal buyPrice;

    /**
     * 持仓成本价
     */
    @NotNull(message = "成本价不能为空")
    private BigDecimal costPrice;

    /**
     * 持仓类型/方向
     */
    @NotBlank(message = "持仓方向不能为空")
    private String side;
}
