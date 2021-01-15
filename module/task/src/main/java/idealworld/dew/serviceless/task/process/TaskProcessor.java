/*
 * Copyright 2020. gudaoxuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package idealworld.dew.serviceless.task.process;

import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.NotFoundException;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.serviceless.task.TaskConfig;
import idealworld.dew.serviceless.task.domain.TaskDef;
import idealworld.dew.serviceless.task.domain.TaskInst;
import idealworld.dew.serviceless.task.helper.CronHelper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 事件触发服务.
 *
 * @author gudaoxuri
 */
@Slf4j
public class TaskProcessor extends EventBusProcessor {

    private static Vertx _vertx;

    public TaskProcessor(String moduleName, TaskConfig config, Vertx vertx, ProcessContext context) {
        super(moduleName);
        _vertx = vertx;
        ScriptProcessor.init(config.getGatewayServerUrl());
        loadTasks(context);
    }

    {
        // 初始化任务列表
        addProcessor(OptActionKind.CREATE, "/task", eventBusContext ->
                initTasks(
                        eventBusContext.req.body(String.class),
                        eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.context));
        // 添加当前应用的任务
        addProcessor(OptActionKind.CREATE, "/task/{taskCode}", eventBusContext ->
                addTask(
                        eventBusContext.req.params.get("taskCode"),
                        eventBusContext.req.params.getOrDefault("cron", ""),
                        eventBusContext.req.body(String.class),
                        eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.context));
        // 修改当前应用的某个任务
        addProcessor(OptActionKind.MODIFY, "/task/{taskCode}", eventBusContext ->
                modifyTask(
                        eventBusContext.req.params.get("taskCode"),
                        eventBusContext.req.params.getOrDefault("cron", ""),
                        eventBusContext.req.body(String.class),
                        eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.context));
        // 删除当前应用的某个任务
        addProcessor(OptActionKind.DELETE, "/task/{taskCode}", eventBusContext ->
                deleteTask(
                        eventBusContext.req.params.get("taskCode"),
                        eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.context));
        // 执行当前应用的某个任务
        addProcessor(OptActionKind.CREATE, "/exec/{taskCode}", eventBusContext ->
                execTask(
                        eventBusContext.req.params.get("taskCode"),
                        eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.body(List.class),
                        false,
                        eventBusContext.req.identOptInfo,
                        eventBusContext.context));
    }

    public static void loadTasks(ProcessContext context) {
        context.sql.list(new HashMap<>(), TaskDef.class)
                .onSuccess(taskDefs -> {
                    if (!taskDefs.isEmpty()) {
                        var initTask = taskDefs.stream().filter(def -> def.getCode().isBlank()).findAny().get();
                        initTasks(initTask.getFun(), initTask.getRelAppId(), context);
                        taskDefs.stream().filter(def -> !def.getCode().isBlank()).forEach(def -> addTask(def.getCode(), def.getCron(), def.getFun(), def.getRelAppId(), context));
                    }
                })
                .onFailure(e -> context.helper.error(e));
    }

    public static Future<Void> initTasks(String funs, Long appId, ProcessContext context) {
        ScriptProcessor.init(appId, funs);
        var taskDef = TaskDef.builder()
                .code("")
                .cron("")
                .fun(funs)
                .relAppId(appId)
                .build();
        return context.sql.tx(context, () ->
                context.sql.getOne(new HashMap<>() {
                    {
                        put("code", "");
                        put("rel_app_id", appId);
                    }
                }, TaskDef.class)
                        .compose(existTaskDef -> {
                            if (existTaskDef != null) {
                                taskDef.setId(existTaskDef.getId());
                                return context.sql.update(taskDef);
                            } else {
                                return context.sql.save(taskDef)
                                        .compose(resp -> context.helper.success());
                            }
                        }));
    }

    public static Future<Void> addTask(String code, String cron, String fun, Long appId, ProcessContext context) {
        ScriptProcessor.add(appId, code, fun);
        if (!cron.isBlank()) {
            CronHelper.addJob(code, appId + "", cron, (name, group) -> {
                if (!context.cache.isLeader()) {
                    return;
                }
                execTask(code, appId, new ArrayList<>(), true, null, context);
            });
        }
        var taskDef = TaskDef.builder()
                .code(code)
                .cron(cron)
                .fun(fun)
                .relAppId(appId)
                .build();
        return context.sql.save(taskDef)
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> modifyTask(String code, String cron, String fun, Long appId, ProcessContext context) {
        ScriptProcessor.add(appId, code, fun);
        CronHelper.removeJob(code, appId + "");
        if (!cron.isBlank()) {
            CronHelper.addJob(code, appId + "", cron, (name, group) -> {
                if (!context.cache.isLeader()) {
                    return;
                }
                execTask(code, appId, new ArrayList<>(), true, null, context);
            });
        }
        return context.helper.notExistToError(
                context.sql.exists(new HashMap<>() {
                    {
                        put("code", code);
                        put("rel_app_id", appId);
                    }
                }, TaskDef.class), () -> new NotFoundException("任务[" + code + "]不存在"))
                .compose(resp ->
                        context.sql.update(new HashMap<>() {
                            {
                                put("code", code);
                                put("rel_app_id", appId);
                            }
                        }, TaskDef.builder()
                                .cron(cron)
                                .fun(fun)
                                .build()))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> deleteTask(String code, Long appId, ProcessContext context) {
        CronHelper.removeJob(code, appId + "");
        return context.helper.notExistToError(
                context.sql.exists(new HashMap<>() {
                    {
                        put("code", code);
                        put("rel_app_id", appId);
                    }
                }, TaskDef.class), () -> new NotFoundException("任务[" + code + "]不存在"))
                .compose(resp ->
                        context.sql.softDelete(new HashMap<>() {
                            {
                                put("code", code);
                                put("rel_app_id", appId);
                            }
                        }, TaskDef.class))
                .compose(resp -> context.helper.success());
    }

    public static Future<Object> execTask(String code, Long appId, List<?> parameters, Boolean fromTimer, IdentOptCacheInfo identOptCacheInfo, ProcessContext context) {
        if (!fromTimer) {
            return _vertx.getOrCreateContext().executeBlocking(promise -> {
                try {
                    var result = ScriptProcessor.execute(appId, code, parameters, identOptCacheInfo);
                    promise.complete(result);
                } catch (Exception e) {
                    log.warn("Execute task error: {}", e.getMessage(), e);
                    promise.fail(e);
                }
            });
        }
        log.trace("Executing timer task[{}-{}]", appId, code);
        return context.helper.notExistToError(
                context.sql.getOne(new HashMap<>() {
                    {
                        put("code", code);
                        put("rel_app_id", appId);
                    }
                }, TaskDef.class), () -> new NotFoundException("任务[" + code + "]不存在"))
                .compose(taskDef ->
                        context.sql.save(TaskInst.builder()
                                .startTime(System.currentTimeMillis())
                                .endTime(0L)
                                .success(false)
                                .message("")
                                .relTaskDefCode(taskDef.getCode())
                                .relAppId(taskDef.getRelAppId())
                                .build()))
                .compose(taskInstId ->
                        _vertx.getOrCreateContext().executeBlocking(promise -> {
                            try {
                                var result = ScriptProcessor.execute(appId, code, parameters, null);
                                context.sql.update(taskInstId, TaskInst.builder()
                                        .endTime(System.currentTimeMillis())
                                        .success(true)
                                        .message("")
                                        .build());
                                promise.complete(result);
                            } catch (Exception e) {
                                log.warn("Execute timer task error: {}", e.getMessage(), e);
                                context.sql.update(taskInstId, TaskInst.builder()
                                        .endTime(System.currentTimeMillis())
                                        .success(false)
                                        .message(e.getMessage())
                                        .build());
                                promise.fail(e);
                            }
                        }));
    }

}
