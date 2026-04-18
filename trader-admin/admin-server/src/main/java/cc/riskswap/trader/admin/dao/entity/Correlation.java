package cc.riskswap.trader.admin.dao.entity;

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
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 标的
     */
    private String symbol1;

    /**
     * 标的类型
     */
    private String symbol1Type;

    /**
     * 相关标的
     */
    private String symbol2;

    /**
     * 相关标的类型
     */
    private String symbol2Type;

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
