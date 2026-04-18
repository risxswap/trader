package cc.riskswap.trader.executor.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("investment_log")
public class InvestmentLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long investmentId;
    private java.time.LocalDateTime recordDate;
    private java.math.BigDecimal cash;
    private java.math.BigDecimal asset;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}