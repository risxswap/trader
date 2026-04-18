package cc.riskswap.trader.executor.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("fund_nav")
public class FundNav {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fundCode;
    private java.time.LocalDate navDate;
    private java.math.BigDecimal unitNav;
    private java.math.BigDecimal accumNav;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}