package cc.riskswap.trader.admin.common.model.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class MsgPushLogDto {
    private Integer id;
    private String type;
    private String content;
    private String status;
    private String channel;
    private String title;
    private String recipient;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
