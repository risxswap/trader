package cc.riskswap.trader.admin.test.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cc.riskswap.trader.admin.common.model.ResData;
import cc.riskswap.trader.admin.common.model.dto.CorrelationDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CorrelationDtoSerializationTest {

    @Test
    void shouldSerializeLargeCorrelationIdAsString() throws Exception {
        CorrelationDto dto = new CorrelationDto();
        dto.setId(9007199254740993L);

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        String json = objectMapper.writeValueAsString(ResData.success(dto));

        Assertions.assertTrue(json.contains("\"id\":\"9007199254740993\""));
    }
}
