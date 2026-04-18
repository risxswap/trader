package cc.riskswap.trader.admin.common.model.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class HardwareInfoDto {

    private double cpuUsage;

    private long memoryUsed;

    private long memoryTotal;

    private long diskUsed;

    private long diskTotal;

    private LocalDateTime reportTime;

}
