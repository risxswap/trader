package cc.riskswap.trader.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 投资日志实体类
 */
@Data
@TableName("investment_log")
public class InvestmentLog {

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
     * 记录日期
     */
    private OffsetDateTime recordDate;

    /**
     * 现金
     */
    private BigDecimal cash;

    /**
     * 资产
     */
    private BigDecimal asset;

    /**
     * 盈利
     */
    private BigDecimal profit;

    /**
     * 类型
     */
    private String type;

    /**
     * 是否已通知
     */
    private Integer notified;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
}
