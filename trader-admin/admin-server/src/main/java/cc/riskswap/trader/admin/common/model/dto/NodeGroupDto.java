package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;

@Data
public class NodeGroupDto {
    private Long id;
    private String name;
    private String code;
    private Integer sort;
    private Boolean defaultPending;
    private Long nodeCount;
}
