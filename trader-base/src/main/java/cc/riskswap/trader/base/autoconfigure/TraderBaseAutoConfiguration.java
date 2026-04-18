package cc.riskswap.trader.base.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        TraderDataSourceAutoConfiguration.class,
        TraderRedisAutoConfiguration.class,
        TraderLoggingAutoConfiguration.class
})
public class TraderBaseAutoConfiguration {
}
