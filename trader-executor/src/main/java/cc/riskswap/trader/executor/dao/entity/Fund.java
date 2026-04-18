package cc.riskswap.trader.executor.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("fund")
public class Fund {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String symbol;
    private String name;
    private String type;
    private String manager;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}