package cc.riskswap.trader.admin.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cc.riskswap.trader.base.dao.MsgPushLogDao;
import cc.riskswap.trader.base.dao.entity.MsgPushLog;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import java.time.OffsetDateTime;

/**
 * 企业微信推送渠道
 */
@Slf4j
@Service
public class WeComChannel {

    @Value("${trader.wecom.webhook-url:}")
    private String webhookUrl;

    @Resource(name = "adminMsgPushLogDao")
    private MsgPushLogDao msgPushLogDao;

    /**
     * 发送文本消息
     *
     * @param type    消息类型
     * @param title   标题
     * @param content 消息内容
     */
    public void pushText(String type, String title, String content) {
        String result = null;
        String status = "SUCCESS";
        try {
            // 构建请求体
            JSONObject text = new JSONObject();
            text.set("content", content);
            
            JSONObject params = new JSONObject();
            params.set("msgtype", "text");
            params.set("text", text);

            // 发送请求
            log.info("Sending WeCom message: {}", content);
            result = HttpUtil.post(webhookUrl, params.toString());
            log.info("WeCom response: {}", result);

            // 检查响应
            JSONObject response = JSONUtil.parseObj(result);
            if (response.getInt("errcode") != 0) {
                status = "FAIL";
                log.error("WeCom push failed: {}", result);
            }
        } catch (Exception e) {
            status = "FAIL";
            result = e.getMessage();
            log.error("WeCom push exception", e);
        } finally {
            // 记录日志
            saveLog(type, title, content, status, result);
        }
    }

    /**
     * 发送Markdown消息
     *
     * @param type    消息类型
     * @param title   标题
     * @param content 消息内容
     */
    public void pushMarkdown(String type, String title, String content) {
        String result = null;
        String status = "SUCCESS";
        try {
            // 构建请求体
            JSONObject markdown = new JSONObject();
            markdown.set("content", content);
            
            JSONObject params = new JSONObject();
            params.set("msgtype", "markdown");
            params.set("markdown", markdown);

            // 发送请求
            log.info("Sending WeCom markdown message: {}", content);
            result = HttpUtil.post(webhookUrl, params.toString());
            log.info("WeCom response: {}", result);

            // 检查响应
            JSONObject response = JSONUtil.parseObj(result);
            if (response.getInt("errcode") != 0) {
                status = "FAIL";
                log.error("WeCom push failed: {}", result);
            }
        } catch (Exception e) {
            status = "FAIL";
            result = e.getMessage();
            log.error("WeCom push exception", e);
        } finally {
            // 记录日志
            saveLog(type, title, content, status, result);
        }
    }

    private void saveLog(String type, String title, String content, String status, String response) {
        try {
            MsgPushLog log = new MsgPushLog();
            log.setType(StringUtils.hasText(type) ? type : "text");
            log.setContent(content);
            log.setStatus(status);
            log.setChannel("WeCom");
            log.setTitle(StringUtils.hasText(title) ? title : "Robot Push");
            log.setRecipient("Robot");
            log.setCreatedAt(OffsetDateTime.now());
            log.setUpdatedAt(OffsetDateTime.now());
            msgPushLogDao.save(log);
        } catch (Exception e) {
            log.error("Failed to save push log", e);
        }
    }
}
