package cc.riskswap.trader.base.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 相关性实体类
 */
@Data
@TableName("correlation")
public class Correlation {

    /**
     * 主键ID
     */
    @TableId(type = IdType.INPUT)
    private Long id;

    /**
     * 标的
     */
    private String asset1;

    /**
     * 标的类型
     */
    private String asset1Type;

    /**
     * 相关标的
     */
    private String asset2;

    /**
     * 相关标的类型
     */
    private String asset2Type;

    /**
     * 相关系数
     */
    private BigDecimal coefficient;
    
    /**
     * p值
     */
    private BigDecimal pValue;

    /**
     * 时间周期
     */
    private String period;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
}
