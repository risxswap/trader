package cc.riskswap.trader.executor.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("investment")
public class Investment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String groupName;
    private String status;
    private String cron;
    private String strategy;
    private String strategyConfig;
    private java.math.BigDecimal budget;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}