package cc.riskswap.trader.admin.test.controller;

import cc.riskswap.trader.admin.controller.NodeController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Arrays;

public class NodeControllerRouteTest {

    @Test
    void shouldUseNodePrefixWithoutApiSegment() throws NoSuchMethodException {
        RequestMapping requestMapping = NodeController.class.getAnnotation(RequestMapping.class);
        Assertions.assertNotNull(requestMapping);
        Assertions.assertArrayEquals(new String[]{"/node"}, requestMapping.value());
    }

    @Test
    void shouldExposeNodeManagementRoutes() {
        Method[] methods = NodeController.class.getDeclaredMethods();
        boolean hasListRoute = Arrays.stream(methods)
                .anyMatch(method -> hasGetMapping(method, "/list"));
        boolean hasGroupListRoute = Arrays.stream(methods)
                .anyMatch(method -> hasGetMapping(method, "/group/list"));
        boolean hasCreateGroupRoute = Arrays.stream(methods)
                .anyMatch(method -> hasPostMapping(method, "/group"));
        boolean hasUpdateGroupRoute = Arrays.stream(methods)
                .anyMatch(method -> hasPutMapping(method, "/group"));
        boolean hasDeleteGroupRoute = Arrays.stream(methods)
                .anyMatch(method -> hasDeleteMapping(method, "/group/{id}"));
        boolean hasDetailRoute = Arrays.stream(methods)
                .anyMatch(method -> hasGetMapping(method, "/{id}"));
        boolean hasHistoryRoute = Arrays.stream(methods)
                .anyMatch(method -> hasGetMapping(method, "/{nodeId}/history"));
        boolean hasApproveRoute = Arrays.stream(methods)
                .anyMatch(method -> hasPostMapping(method, "/approve"));
        boolean hasUpdateRoute = Arrays.stream(methods)
                .anyMatch(method -> hasPutMapping(method, ""));
        boolean hasDeleteRoute = Arrays.stream(methods)
                .anyMatch(method -> hasDeleteMapping(method, "/{id}"));

        Assertions.assertTrue(hasListRoute);
        Assertions.assertTrue(hasGroupListRoute);
        Assertions.assertTrue(hasCreateGroupRoute);
        Assertions.assertTrue(hasUpdateGroupRoute);
        Assertions.assertTrue(hasDeleteGroupRoute);
        Assertions.assertTrue(hasDetailRoute);
        Assertions.assertTrue(hasHistoryRoute);
        Assertions.assertTrue(hasApproveRoute);
        Assertions.assertTrue(hasUpdateRoute);
        Assertions.assertTrue(hasDeleteRoute);
    }

    private boolean hasGetMapping(Method method, String path) {
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        if (getMapping == null) {
            return false;
        }
        return Arrays.asList(getMapping.value()).contains(path);
    }

    private boolean hasPostMapping(Method method, String path) {
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        if (postMapping == null) {
            return false;
        }
        return Arrays.asList(postMapping.value()).contains(path);
    }

    private boolean hasPutMapping(Method method, String path) {
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        if (putMapping == null) {
            return false;
        }
        if (putMapping.value().length == 0) {
            return path.isEmpty();
        }
        return Arrays.asList(putMapping.value()).contains(path);
    }

    private boolean hasDeleteMapping(Method method, String path) {
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        if (deleteMapping == null) {
            return false;
        }
        return Arrays.asList(deleteMapping.value()).contains(path);
    }
}
