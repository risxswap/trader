package cc.riskswap.trader.base;

import cc.riskswap.trader.base.config.TraderNodeProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TraderNodePropertiesTest {

    @Test
    void shouldReadNodeIdFromTraderNodeIdSystemProperty() {
        String originalNodeId = System.getProperty("TRADER_NODE_ID");
        try {
            System.setProperty("TRADER_NODE_ID", "node-from-env");
            assertThat(new TraderNodeProperties().getId()).isEqualTo("node-from-env");
        } finally {
            if (originalNodeId == null) {
                System.clearProperty("TRADER_NODE_ID");
            } else {
                System.setProperty("TRADER_NODE_ID", originalNodeId);
            }
        }
    }

    @Test
    void shouldReadNodeTypeAndNameFromSystemProperty() {
        String originalNodeType = System.getProperty("TRADER_NODE_TYPE");
        String originalNodeName = System.getProperty("TRADER_NODE_NAME");
        try {
            System.setProperty("TRADER_NODE_TYPE", "maker");
            System.setProperty("TRADER_NODE_NAME", "node-a");
            TraderNodeProperties properties = new TraderNodeProperties();
            assertThat(properties.getType()).isEqualTo("maker");
            assertThat(properties.getName()).isEqualTo("node-a");
        } finally {
            if (originalNodeType == null) {
                System.clearProperty("TRADER_NODE_TYPE");
            } else {
                System.setProperty("TRADER_NODE_TYPE", originalNodeType);
            }
            if (originalNodeName == null) {
                System.clearProperty("TRADER_NODE_NAME");
            } else {
                System.setProperty("TRADER_NODE_NAME", originalNodeName);
            }
        }
    }

    @Test
    void shouldFallbackToBoundPropertiesWhenSystemPropertyNotSet() {
        String originalNodeType = System.getProperty("TRADER_NODE_TYPE");
        String originalNodeName = System.getProperty("TRADER_NODE_NAME");
        try {
            System.clearProperty("TRADER_NODE_TYPE");
            System.clearProperty("TRADER_NODE_NAME");
            TraderNodeProperties properties = new TraderNodeProperties();
            properties.setType("  taker  ");
            properties.setName("  node-b ");
            assertThat(properties.getType()).isEqualTo("taker");
            assertThat(properties.getName()).isEqualTo("node-b");
        } finally {
            if (originalNodeType == null) {
                System.clearProperty("TRADER_NODE_TYPE");
            } else {
                System.setProperty("TRADER_NODE_TYPE", originalNodeType);
            }
            if (originalNodeName == null) {
                System.clearProperty("TRADER_NODE_NAME");
            } else {
                System.setProperty("TRADER_NODE_NAME", originalNodeName);
            }
        }
    }

    @Test
    void shouldReadNodeIdFromBoundPropertyWhenSystemPropertyNotSet() {
        String originalNodeId = System.getProperty("TRADER_NODE_ID");
        try {
            System.clearProperty("TRADER_NODE_ID");
            TraderNodeProperties properties = new TraderNodeProperties();
            properties.setId("  node-from-config  ");
            assertThat(properties.getId()).isEqualTo("node-from-config");
        } finally {
            if (originalNodeId == null) {
                System.clearProperty("TRADER_NODE_ID");
            } else {
                System.setProperty("TRADER_NODE_ID", originalNodeId);
            }
        }
    }
}
