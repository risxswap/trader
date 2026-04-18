package cc.riskswap.trader.executor.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("fund_market")
public class FundMarket {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fundCode;
    private java.time.LocalDate time;
    private java.math.BigDecimal open;
    private java.math.BigDecimal high;
    private java.math.BigDecimal low;
    private java.math.BigDecimal close;
    private java.math.BigDecimal volume;
    private java.math.BigDecimal amount;
    private java.math.BigDecimal pctChg;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}