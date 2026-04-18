package cc.riskswap.trader.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 投资实体类
 */
@Data
@TableName(value = "investment", autoResultMap = true)
public class Investment {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 名字
     */
    private String name;

    /**
     * 标的类型
     */
    private String targetType;

    /**
     * 投资类型
     */
    private String investType;

    /**
     * 标的列表
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> targets;

    /**
     * 预算
     */
    private BigDecimal budget;

    /**
     * 券商ID
     */
    private Integer brokerId;

    /**
     * 策略
     */
    private String strategy;

    /**
     * 策略配置
     */
    private String strategyConfig;

    /**
     * 定时任务表达式
     */
    private String cron;

    /**
     * 分组名字
     */
    private String groupName;
    
    /**
     * 状态
     * @see InvestmentStatusEnum
     */
    private String status;

    /**
     * 执行器ID
     */
    private String executorId;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    private OffsetDateTime updatedAt;
}
