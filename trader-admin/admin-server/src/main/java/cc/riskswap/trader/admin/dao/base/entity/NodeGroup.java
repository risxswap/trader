package cc.riskswap.trader.admin.dao.base.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("node_group")
public class NodeGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private Integer sort;

    private Boolean isDefaultPending;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
