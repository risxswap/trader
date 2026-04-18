package cc.riskswap.trader.admin.test.controller;

import cc.riskswap.trader.admin.controller.CorrelationController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

public class CorrelationControllerStructureTest {

    @Test
    void shouldNotExposeManualCalculateEndpoint() {
        boolean hasCalculateEndpoint = Arrays.stream(CorrelationController.class.getDeclaredMethods())
                .anyMatch(this::isCalculateEndpoint);

        Assertions.assertFalse(hasCalculateEndpoint);
    }

    private boolean isCalculateEndpoint(Method method) {
        if (!"calculate".equals(method.getName())) {
            return false;
        }
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        return postMapping != null && Arrays.asList(postMapping.value()).contains("/calculate");
    }
}
