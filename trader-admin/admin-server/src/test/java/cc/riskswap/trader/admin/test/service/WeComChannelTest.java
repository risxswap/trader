package cc.riskswap.trader.admin.test.service;


import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import cc.riskswap.trader.admin.channel.WeComChannel;

public class WeComChannelTest {

    @Test
    public void testPushText() throws Exception {
        WeComChannel weComChannel = new WeComChannel();

        Field webhookUrlField = WeComChannel.class.getDeclaredField("webhookUrl");
        webhookUrlField.setAccessible(true);
        webhookUrlField.set(weComChannel, "http://127.0.0.1:1");

        Field daoField = WeComChannel.class.getDeclaredField("msgPushLogDao");
        daoField.setAccessible(true);

        weComChannel.pushText("test", "Test Title", "Hello from Trae! Unit Test.");
    }
}
