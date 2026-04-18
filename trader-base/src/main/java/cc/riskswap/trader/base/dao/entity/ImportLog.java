package cc.riskswap.trader.base.dao.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ImportLog {
    
    private Integer id;

    private String file;

    private String status;

    private String type;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
