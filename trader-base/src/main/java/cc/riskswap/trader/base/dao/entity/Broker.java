package cc.riskswap.trader.base.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 经纪人实体类
 */
@Data
@TableName("broker")
public class Broker {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 名字
     */
    private String name;

    /**
     * 代号
     */
    private String code;

    /**
     * 初始资金
     */
    private BigDecimal initialCapital;

    /**
     * 当前资金
     */
    private BigDecimal currentCapital;

    /**
     * 简介
     */
    private String intro;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
}
