package cc.riskswap.trader.executor.common.model.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class InvestmentInfoDto {

    private Integer id;

    private String name;

    private String groupName;

    private String strategy;

    private String cron;
    
    private String status;
    
    private String executorId;

    private LocalDateTime reportTime;

}
