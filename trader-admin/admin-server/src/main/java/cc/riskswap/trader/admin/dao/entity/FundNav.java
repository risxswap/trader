package cc.riskswap.trader.admin.dao.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("fund_nav")
public class FundNav {
    
    /**
     * 时间
     */
    private OffsetDateTime time;

    /**
     * 代码
     */
    private String symbol;

    /**
     * 单位净值
     */
    @TableField("unit_nav")
    private BigDecimal unitNav;

    /**
     * 累计净值
     */
    @TableField("accum_nav")
    private BigDecimal accumNav;

    /**
     * 累计分红
     */
    @TableField("accum_div")
    private BigDecimal accumDiv;

    /**
     * 资产净值
     */
    @TableField("net_asset")
    private BigDecimal netAsset;

    /**
     * 合计资产净值
     */
    @TableField("total_net_asset")
    private BigDecimal totalNetAsset;

    /**
     * 复权净值
     */
    @TableField("adj_nav")
    private BigDecimal adjNav;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private OffsetDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private OffsetDateTime updatedAt;
}
