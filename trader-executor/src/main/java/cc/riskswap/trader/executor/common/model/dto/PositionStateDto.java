package cc.riskswap.trader.executor.common.model.dto;

import cc.riskswap.trader.executor.dao.entity.InvestmentPosition;
import cc.riskswap.trader.executor.common.model.param.TradingParam;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionStateDto {
    private String asset;
    private String assetType;
    private BigDecimal netQuantity = BigDecimal.ZERO;
    private BigDecimal avgPrice = BigDecimal.ZERO;

    public PositionStateDto(String asset, String assetType) {
        this.asset = asset;
        this.assetType = assetType;
    }

    public static PositionStateDto from(InvestmentPosition pos) {
        if (pos == null) {
            return null;
        }
        PositionStateDto dto = new PositionStateDto();
        dto.setAsset(pos.getAsset());
        dto.setAssetType(pos.getAssetType());
        dto.setNetQuantity(pos.getQuantity());
        if ("LONG".equals(pos.getSide())) {
            dto.setAvgPrice(pos.getBuyPrice() != null ? pos.getBuyPrice() : pos.getCostPrice());
        } else {
            dto.setAvgPrice(pos.getCostPrice());
        }
        return dto;
    }

    public void processTrade(TradingParam param) {
        if (param.getVolume() == null || param.getPrice() == null) {
            return;
        }
        // Logic for processing the trade
        BigDecimal currentTotal = this.netQuantity.multiply(this.avgPrice);
        BigDecimal newTrade = param.getVolume().multiply(param.getPrice());
        this.netQuantity = this.netQuantity.add(param.getVolume());
        
        if (this.netQuantity.signum() != 0) {
            this.avgPrice = currentTotal.add(newTrade).divide(this.netQuantity, 8, java.math.RoundingMode.HALF_UP);
        } else {
            this.avgPrice = BigDecimal.ZERO;
        }
    }
}
