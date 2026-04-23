package cc.riskswap.trader.admin.test.controller;

import cc.riskswap.trader.admin.controller.TaskLogController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

public class TaskLogControllerRouteTest {

    @Test
    void shouldUseTaskLogPrefixWithoutApiSegment() {
        RequestMapping requestMapping = TaskLogController.class.getAnnotation(RequestMapping.class);
        Assertions.assertNotNull(requestMapping);
        Assertions.assertArrayEquals(new String[]{"/logs/task"}, requestMapping.value());
    }

    @Test
    void shouldExposeTaskLogRoutes() {
        Method[] methods = TaskLogController.class.getDeclaredMethods();
        boolean hasListRoute = Arrays.stream(methods)
                .anyMatch(method -> hasGetMapping(method, ""));
        boolean hasDetailRoute = Arrays.stream(methods)
                .anyMatch(method -> hasGetMapping(method, "/{id}"));
        boolean hasDeleteRoute = Arrays.stream(methods)
                .anyMatch(method -> hasPostMapping(method, "/delete"));

        Assertions.assertTrue(hasListRoute);
        Assertions.assertTrue(hasDetailRoute);
        Assertions.assertTrue(hasDeleteRoute);
    }

    private boolean hasGetMapping(Method method, String path) {
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping == null) {
            return false;
        }
        if (getMapping.value().length == 0) {
            return path.isEmpty();
        }
        return Arrays.asList(getMapping.value()).contains(path);
    }

    private boolean hasPostMapping(Method method, String path) {
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping == null) {
            return false;
        }
        if (postMapping.value().length == 0) {
            return path.isEmpty();
        }
        return Arrays.asList(postMapping.value()).contains(path);
    }
}
