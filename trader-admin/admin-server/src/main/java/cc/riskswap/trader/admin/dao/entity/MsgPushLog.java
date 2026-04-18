package cc.riskswap.trader.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("msg_push_log")
public class MsgPushLog {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String type;
    private String content;
    private String status;
    private String channel;
    private String title;
    private String recipient;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
