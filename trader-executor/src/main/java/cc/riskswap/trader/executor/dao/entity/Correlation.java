package cc.riskswap.trader.executor.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("correlation")
public class Correlation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceCode;
    private String targetCode;
    private String targetType;
    private java.math.BigDecimal value;
    private java.time.LocalDate calcDate;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}