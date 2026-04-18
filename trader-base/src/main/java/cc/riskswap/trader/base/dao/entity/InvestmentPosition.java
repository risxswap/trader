package cc.riskswap.trader.base.dao.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 持仓实体类
 */
@Data
@TableName("investment_position")
public class InvestmentPosition {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 投资ID
     */
    private Integer investmentId;

     /**
     * 日志ID
     */
    private Integer investmentLogId;
    
    /**
     * 标的
     */
    private String asset;

    /**
     * 标的类型
     */
    private String assetType;

    /**
     * 持仓数量
     */
    private BigDecimal quantity;

    /**
     * 买入价
     */
    private BigDecimal buyPrice;

    /**
     * 持仓成本价
     */
    private BigDecimal costPrice;

    /**
     * 持仓类型/方向 (LONG/SHORT)
     */
    private String side;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
}
