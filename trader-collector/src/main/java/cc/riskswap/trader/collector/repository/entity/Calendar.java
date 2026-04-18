package cc.riskswap.trader.collector.repository.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class Calendar {

    private Integer open;
    
    private LocalDate date;

    private LocalDate preDate;

    private String exchange;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}