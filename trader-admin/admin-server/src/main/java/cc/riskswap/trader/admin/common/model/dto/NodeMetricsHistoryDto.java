package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;
import java.util.List;

@Data
public class NodeMetricsHistoryDto {
    private List<String> timestamps;
    private List<Float> cpuUsages;
    private List<Float> memoryUsages;
}
