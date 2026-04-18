package cc.riskswap.trader.base.dao.base.entity;

import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 消息推送日志实体类
 */
@Data
@TableName("msg_push_log")
public class MsgPushLog {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 状态
     */
    private String status;

    /**
     * 渠道
     */
    private String channel;

    /**
     * 标题
     */
    private String title;

    /**
     * 接收人
     */
    private String recipient;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
}
