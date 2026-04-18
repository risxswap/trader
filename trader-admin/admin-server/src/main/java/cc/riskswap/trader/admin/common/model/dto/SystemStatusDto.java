package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;

@Data
public class SystemStatusDto {
    private String redisStatus; // "NORMAL" or "ERROR"
    private String clickHouseStatus;
    private String mysqlStatus;
}
