package cc.riskswap.trader.executor.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("investment_position")
public class InvestmentPosition {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long investmentId;
    private String asset;
    private String assetType;
    private java.math.BigDecimal quantity;
    private String side;
    private java.math.BigDecimal buyPrice;
    private java.math.BigDecimal costPrice;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}