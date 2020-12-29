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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 事件触发服务.
 *
 * @author gudaoxuri
 */
@Slf4j
public class TaskProcessor extends EventBusProcessor {

    private static Vertx _vertx;

    public TaskProcessor(String moduleName, TaskConfig config, Vertx vertx) {
        super(moduleName);
        _vertx = vertx;
        String requirejs = new BufferedReader(new InputStreamReader(TaskProcessor.class.getResourceAsStream("/requirejs.js")))
                .lines().collect(Collectors.joining("\n"));
        String dewSDK = new BufferedReader(new InputStreamReader(TaskProcessor.class.getResourceAsStream("/DewSDK_browserify.js")))
                .lines().collect(Collectors.joining("\n"));
        ScriptProcessor.init(config.getGatewayServerUrl(), requirejs, dewSDK);
    }

    {
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
                        eventBusContext.context));

    }

    public static Future<Void> addTask(String code, String cron, String fun, Long appId, ProcessContext context) {
        ScriptProcessor.add(appId, code, fun);
        if (!cron.isBlank()) {
            CronHelper.addJob(code, appId + "", cron, (name, group) -> {
                if (!context.cache.isLeader()) {
                    return;
                }
                execTask(code, appId, context);
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
                execTask(code, appId, context);
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

    public static Future<Void> execTask(String code, Long appId, ProcessContext context) {
        log.trace("Executing task[{}-{}]", appId, code);
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
                                ScriptProcessor.execute(appId, code);
                                context.sql.update(taskInstId, TaskInst.builder()
                                        .endTime(System.currentTimeMillis())
                                        .success(true)
                                        .message("")
                                        .build());
                            } catch (Exception e) {
                                log.warn("Execute task error: {}", e.getMessage(), e);
                                context.sql.update(taskInstId, TaskInst.builder()
                                        .endTime(System.currentTimeMillis())
                                        .success(false)
                                        .message(e.getMessage())
                                        .build());
                            }
                            promise.complete();
                        }));
    }

}