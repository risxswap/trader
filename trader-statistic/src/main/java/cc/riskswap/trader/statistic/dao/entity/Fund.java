package cc.riskswap.trader.statistic.dao.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;

import lombok.Data;

/**
 * 基金实体类
 */
@Data
public class Fund {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;


    /**
     * 基金代码
     */
    private String code;

    /**
     * 简称
     */
    private String name;

    /**
     * 管理人
     */
    private String management;

    /**
     * 托管人
     */
    private String custodian;

    /**
     * 投资类型
     */
    private String fundType;

    /**
     * 成立日期
     */
    private OffsetDateTime foundDate;

    /**
     * 到期日期
     */
    private OffsetDateTime dueDate;

    /**
     * 上市时间
     */
    private OffsetDateTime listDate;

    /**
     * 发行日期
     */
    private OffsetDateTime issueDate;

    /**
     * 退市日期
     */
    private OffsetDateTime delistDate;

    /**
     * 发行份额(亿)
     */
    private BigDecimal issueAmount;

    /**
     * 管理费
     */
    @TableField("m_fee")
    private BigDecimal managementFee;

    /**
     * 托管费
     */
    @TableField("c_fee")
    private BigDecimal custodianFee;

    /**
     * 存续期
     */
    private BigDecimal durationYear;

    /**
     * 面值
     */
    private BigDecimal pValue;

    /**
     * 起点金额(万元)
     */
    private BigDecimal minAmount;

    /**
     * 预期收益率
     */
    private BigDecimal expReturn;

    /**
     * 业绩比较基准
     */
    private String benchmark;

    /**
     * 存续状态D摘牌 I发行 L已上市
     */
    private String status;

    /**
     * 投资风格
     */
    private String investType;

    /**
     * 基金类型
     */
    private String type;

    /**
     * 受托人
     */
    private String trustee;

    /**
     * 日常申购起始日
     */
    private OffsetDateTime purcStartdate;

    /**
     * 日常赎回起始日
     */
    private OffsetDateTime redmStartdate;

    /**
     * E场内O场外
     */
    private String market;

    private String exchange;

    /**
     * 相关性分组
     */
    private String correlation;

    private OffsetDateTime updatedAt;

    private OffsetDateTime createdAt;
}
