package cc.riskswap.trader.admin.dao.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("node")
public class Node {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String nodeId;

    private String nodeName;

    private String nodeType;

    private Long nodeGroupId;

    private String approvalStatus;

    private String hostname;

    private String primaryIp;

    private String remark;

    private OffsetDateTime approvedAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
