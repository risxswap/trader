package cc.riskswap.trader.admin.channel;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import cc.riskswap.trader.base.dao.MsgPushLogDao;
import cc.riskswap.trader.base.dao.entity.MsgPushLog;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MatrixChannel {

    @Value("${trader.matrix.homeserver:}")
    private String homeserver;

    @Value("${trader.matrix.room-id:}")
    private String roomId;

    @Value("${trader.matrix.access-token:}")
    private String accessToken;

    @Resource(name = "adminMsgPushLogDao")
    private MsgPushLogDao msgPushLogDao;

    public void pushText(String type, String title, String content) {
        String result = null;
        String status = "SUCCESS";
        try {
            if (!StringUtils.hasText(homeserver) || !StringUtils.hasText(roomId) || !StringUtils.hasText(accessToken)) {
                status = "FAIL";
                result = "Matrix channel config missing";
                return;
            }

            String url = buildSendUrl();
            JSONObject params = new JSONObject();
            params.set("msgtype", "m.text");
            params.set("body", content);
            
            doSend(url, params, type, title, content, status, result);
        } catch (Exception e) {
            log.error("Matrix push text exception", e);
        }
    }

    public void pushMarkdown(String type, String title, String content) {
        String result = null;
        String status = "SUCCESS";
        try {
            if (!StringUtils.hasText(homeserver) || !StringUtils.hasText(roomId) || !StringUtils.hasText(accessToken)) {
                status = "FAIL";
                result = "Matrix channel config missing";
                return;
            }

            String url = buildSendUrl();
            JSONObject params = new JSONObject();
            params.set("msgtype", "m.text");
            params.set("body", content); 
            
            // Simple markdown to HTML converter for Matrix
            String html = content;
            html = html.replaceAll("(?m)^## (.*?)$", "<h2>$1</h2>");
            html = html.replaceAll("(?m)^### (.*?)$", "<h3>$1</h3>");
            html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<b>$1</b>");
            html = html.replaceAll("`([^`]+)`", "<code>$1</code>");
            html = html.replaceAll("(?m)^\\* (.*?)$", "<li>$1</li>");
            html = html.replaceAll("(?m)^> (.*?)$", "<blockquote>$1</blockquote>");
            
            // Handle markdown tables
            if (html.contains("|")) {
                html = html.replaceAll("(?m)^\\|(.+)\\|$", "<tr><td>$1</td></tr>");
                html = html.replaceAll("</td></tr>\n<tr><td>---", ""); // remove separator line
                html = html.replaceAll("<tr><td>(.*?)</td></tr>", "<tr><td>$1</td></tr>".replace("|", "</td><td>"));
                html = "<table>" + html + "</table>"; // This is a rough approximation, but works for simple tables
            }
            
            html = html.replaceAll("\n", "<br/>");
            
            params.set("format", "org.matrix.custom.html");
            params.set("formatted_body", html);
            
            doSend(url, params, type, title, content, status, result);
        } catch (Exception e) {
            log.error("Matrix push markdown exception", e);
        }
    }

    private void doSend(String url, JSONObject params, String type, String title, String content, String status, String result) {
        try {
            result = HttpRequest.put(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(params.toString())
                    .timeout(5000)
                    .execute()
                    .body();
            log.info("result:{}", result);
            if (JSONUtil.isTypeJSON(result)) {
                JSONObject response = JSONUtil.parseObj(result);
                
                // Auto join if not in room
                if ("M_FORBIDDEN".equals(response.getStr("errcode")) && 
                    response.getStr("error") != null && 
                    response.getStr("error").contains("join")) {
                    log.info("Bot is not in the room, attempting to join...");
                    if (joinRoom()) {
                        // Retry once
                        result = HttpRequest.put(url)
                                .header("Authorization", "Bearer " + accessToken)
                                .body(params.toString())
                                .timeout(5000)
                                .execute()
                                .body();
                        log.info("Retry result:{}", result);
                        if (JSONUtil.isTypeJSON(result)) {
                            response = JSONUtil.parseObj(result);
                        }
                    }
                }

                if (response.containsKey("errcode")) {
                    status = "FAIL";
                }
            } else {
                status = "FAIL";
            }
        } catch (Exception e) {
            status = "FAIL";
            result = e.getMessage();
            log.error("Matrix doSend exception", e);
        } finally {
            saveLog(type, title, content, status, result);
        }
    }

    private boolean joinRoom() {
        String base = homeserver;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        
        String actualRoomId = roomId;
        if (!actualRoomId.startsWith("!") && !actualRoomId.startsWith("#")) {
            actualRoomId = "!" + actualRoomId;
        }
        
        String url = base + "/_matrix/client/v3/join/" + URLEncoder.encode(actualRoomId, StandardCharsets.UTF_8);
        try {
            String result = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .body("{}")
                    .timeout(5000)
                    .execute()
                    .body();
            log.info("Join room result: {}", result);
            return JSONUtil.isTypeJSON(result) && JSONUtil.parseObj(result).containsKey("room_id");
        } catch (Exception e) {
            log.error("Failed to join room", e);
            return false;
        }
    }

    private String buildSendUrl() {
        String base = homeserver;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        
        // Ensure roomId starts with ! or #
        String actualRoomId = roomId;
        if (!actualRoomId.startsWith("!") && !actualRoomId.startsWith("#")) {
            actualRoomId = "!" + actualRoomId;
        }
        
        String encodedRoomId = URLEncoder.encode(actualRoomId, StandardCharsets.UTF_8);
        String txnId = UUID.randomUUID().toString();
        return base + "/_matrix/client/v3/rooms/" + encodedRoomId + "/send/m.room.message/" + txnId;
    }

    private void saveLog(String type, String title, String content, String status, String response) {
        try {
            MsgPushLog log = new MsgPushLog();
            log.setType(StringUtils.hasText(type) ? type : "text");
            log.setContent(content);
            log.setStatus(status);
            log.setChannel("Matrix");
            log.setTitle(StringUtils.hasText(title) ? title : "Robot Push");
            log.setRecipient(StringUtils.hasText(roomId) ? roomId : "Room");
            log.setCreatedAt(OffsetDateTime.now());
            log.setUpdatedAt(OffsetDateTime.now());
            msgPushLogDao.save(log);
        } catch (Exception e) {
            log.error("Failed to save push log", e);
        }
    }
}
