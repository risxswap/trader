package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class PageDto<T> {
    private List<T> items;
    private long total;
    private int pageNo;
    private int pageSize;
}
