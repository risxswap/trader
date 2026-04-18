package cc.riskswap.trader.admin.common.model.dto;

import lombok.Data;

@Data
public class StrategyInfoDto {

    /**
     * 类型
     */
    private String className;

    /**
     * 策略名字
     */
    private String name;

    /**
     * 策略配置
     */
    private String configSchame;

    /**
     * 策略参数配置值
     */
    private String config;


}
