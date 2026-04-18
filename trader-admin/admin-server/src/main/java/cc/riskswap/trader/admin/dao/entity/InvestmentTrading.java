package cc.riskswap.trader.admin.dao.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 交易实体类
 */
@Data
@TableName("investment_trading")
public class InvestmentTrading {

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
     * 投资日志ID
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
     * 数量，正负标识方向
     */
    private BigDecimal volume;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
}
