package cc.riskswap.trader.base.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "trader.node")
public class TraderNodeProperties {

    private static final String NODE_ID_ENV = "TRADER_NODE_ID";
    private static final String NODE_TYPE_ENV = "TRADER_NODE_TYPE";
    private static final String NODE_NAME_ENV = "TRADER_NODE_NAME";

    private String id;
    private String type;
    private String name;

    public String getId() {
        return resolveValue(NODE_ID_ENV, id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return resolveValue(NODE_TYPE_ENV, type);
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return resolveValue(NODE_NAME_ENV, name);
    }

    public void setName(String name) {
        this.name = name;
    }

    private String resolveValue(String environmentKey, String fallback) {
        String value = System.getenv(environmentKey);
        if (!StringUtils.hasText(value)) {
            value = System.getProperty(environmentKey);
        }
        if (!StringUtils.hasText(value)) {
            value = fallback;
        }
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
