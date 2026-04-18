package cc.riskswap.trader.base.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("system_upgrade")
public class SystemUpgrade {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String version;
    private String title;
    private String description;
    private String status;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private String operator;
    private String errorMessage;
    private String checksum;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
