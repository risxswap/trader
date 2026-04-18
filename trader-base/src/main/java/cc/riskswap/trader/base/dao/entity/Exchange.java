package cc.riskswap.trader.base.dao.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("exchange")
public class Exchange {

    @TableId
    private String code;

    private String name;

    private String timezone;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
