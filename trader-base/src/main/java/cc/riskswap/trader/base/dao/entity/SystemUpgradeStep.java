package cc.riskswap.trader.base.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("system_sql_script")
public class SystemUpgradeStep {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String dbType;
    private String version;
    private String sqlText;
    private String checksum;
    private String status;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
