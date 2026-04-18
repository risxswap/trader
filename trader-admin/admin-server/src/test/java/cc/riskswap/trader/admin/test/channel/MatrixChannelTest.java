package cc.riskswap.trader.admin.test.channel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cc.riskswap.trader.admin.channel.MatrixChannel;

@SpringBootTest
public class MatrixChannelTest {

    @Autowired
    private MatrixChannel matrixChannel;

    @Test
    public void testPushText() {
        String markdownContent = """
                ## 📊 交易执行报告
                
                **执行时间**: `2026-03-18 21:30:00`
                **策略名称**: `双均线趋势跟踪`
                **执行状态**: ✅ **成功**
                
                ### 💰 资产概况
                | 资产类型 | 当前净值 | 变动 | 仓位占比 |
                |---------|---------|------|---------|
                | 💵 现金 | $15,200.00 | -12% | 15% |
                | 📈 股票 | $85,800.00 | +3.5% | 85% |
                | **总计** | **$101,000.00** | **+1.2%** | **100%** |
                
                ### 📝 交易明细
                * **买入**: `AAPL` @ $175.50 (数量: 100)
                * **卖出**: `TSLA` @ $180.20 (数量: 50)
                
                ### ⚠️ 风险提示
                > 当前市场波动率较高 (VIX: `22.5`)，建议关注仓位风险。系统已自动调低单次交易上限。
                
                ---
                *由 Trader-Admin 自动生成*
                """;
        
        matrixChannel.pushMarkdown("trade_log", "交易执行报告", markdownContent);
    }
}
