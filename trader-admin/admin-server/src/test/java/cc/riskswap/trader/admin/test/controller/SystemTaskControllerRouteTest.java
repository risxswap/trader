package cc.riskswap.trader.admin.test.controller;

import cc.riskswap.trader.admin.controller.SystemTaskController;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemTaskControllerRouteTest {

    @Test
    void should_use_task_base_route() {
        RequestMapping mapping = SystemTaskController.class.getAnnotation(RequestMapping.class);
        assertEquals("/task", mapping.value()[0]);
    }

    @Test
    void should_expose_list_update_and_trigger_routes() throws Exception {
        Method list = SystemTaskController.class.getMethod("list", cc.riskswap.trader.admin.common.model.query.SystemTaskListQuery.class);
        Method update = SystemTaskController.class.getMethod("update", cc.riskswap.trader.admin.common.model.param.SystemTaskUpdateParam.class);
        Method trigger = SystemTaskController.class.getMethod("trigger", cc.riskswap.trader.admin.common.model.param.SystemTaskTriggerParam.class);
        assertTrue(list.isAnnotationPresent(PostMapping.class));
        assertTrue(update.isAnnotationPresent(PostMapping.class));
        assertTrue(trigger.isAnnotationPresent(PostMapping.class));
        assertTrue(update.getParameters()[0].isAnnotationPresent(Valid.class));
    }

    @Test
    void should_expose_definition_and_instance_routes() throws Exception {
        Method definitions = SystemTaskController.class.getMethod("definitions", cc.riskswap.trader.admin.common.model.query.TaskDefinitionListQuery.class);
        Method create = SystemTaskController.class.getMethod("createInstance", cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceCreateParam.class);
        Method delete = SystemTaskController.class.getMethod("deleteInstance", cc.riskswap.trader.admin.common.model.param.SystemTaskInstanceDeleteParam.class);
        assertTrue(definitions.isAnnotationPresent(PostMapping.class));
        assertTrue(create.isAnnotationPresent(PostMapping.class));
        assertTrue(delete.isAnnotationPresent(PostMapping.class));
    }

    @Test
    void should_expose_detail_route() throws Exception {
        Method detail = SystemTaskController.class.getMethod("get", Long.class);
        assertTrue(detail.isAnnotationPresent(GetMapping.class));
        assertEquals("/{id}", detail.getAnnotation(GetMapping.class).value()[0]);
    }
}
