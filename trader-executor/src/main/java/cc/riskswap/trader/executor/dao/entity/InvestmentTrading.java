package cc.riskswap.trader.executor.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("investment_trading")
public class InvestmentTrading {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long investmentId;
    private Long investmentLogId;
    private String asset;
    private String assetType;
    private String tradeType;
    private java.math.BigDecimal amount;
    private java.math.BigDecimal volume;
    private java.math.BigDecimal price;
    private java.math.BigDecimal share;
    private java.math.BigDecimal fee;
    private String status;
    private java.time.LocalDateTime tradeTime;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}