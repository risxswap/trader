package cc.riskswap.trader.collector.repository.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("fund_nav")
public class FundNav {

    private String code;

    private OffsetDateTime time;

    private BigDecimal unitNav;

    private BigDecimal accumNav;

    private BigDecimal accumDiv;

    private BigDecimal netAsset;

    private BigDecimal totalNetAsset;

    private BigDecimal adjNav;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
