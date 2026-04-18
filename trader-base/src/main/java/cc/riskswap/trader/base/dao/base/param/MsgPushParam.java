package cc.riskswap.trader.base.dao.base.param;

import lombok.Data;

@Data
public class MsgPushParam {
    
    /**
     * 消息类型
     */
    private String type;

    /**
     * 标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 渠道类型 (支持 WeCom/Matrix)
     */
    private String channel;
}
