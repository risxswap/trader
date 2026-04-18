package cc.riskswap.trader.base.dao.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

@Data
@TableName("calendar")
public class Calendar {

    private Integer open;
    
    private LocalDate date;

    private LocalDate preDate;

    private String exchange;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
