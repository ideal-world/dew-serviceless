/*
 * Copyright 2021. gudaoxuri
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

import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.NotFoundException;
import idealworld.dew.framework.exception.UnAuthorizedException;
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
 * 任务控制器.
 *
 * @author gudaoxuri
 */
@Slf4j
public class TaskProcessor extends EventBusProcessor {

    private static Vertx _vertx;

    public TaskProcessor(String moduleName, TaskConfig config, Vertx vertx, ProcessContext context) {
        super(moduleName);
        _vertx = vertx;
        ScriptProcessor.init();
        loadTasks(context);
    }

    {
        // 初始化任务列表
        addProcessor(OptActionKind.CREATE, "/task", eventBusContext -> {
            if (eventBusContext.req.identOptInfo.getAccountId().longValue() != DewAuthConstant.AK_SK_IDENT_ACCOUNT_FLAG) {
                throw new UnAuthorizedException("该方法仅允许AKSK认证模式下使用");
            }
            return initTasks(
                    eventBusContext.req.body(String.class),
                    eventBusContext.req.identOptInfo.getAppCode(),
                    eventBusContext.context);
        });
        // 添加当前应用的任务
        addProcessor(OptActionKind.CREATE, "/task/{taskCode}", eventBusContext -> {
            if (eventBusContext.req.identOptInfo.getAccountId().longValue() != DewAuthConstant.AK_SK_IDENT_ACCOUNT_FLAG) {
                throw new UnAuthorizedException("该方法仅允许AKSK认证模式下使用");
            }
            return addTask(
                    eventBusContext.req.params.get("taskCode"),
                    eventBusContext.req.params.getOrDefault("cron", ""),
                    eventBusContext.req.body(String.class),
                    eventBusContext.req.identOptInfo.getAppCode(),
                    eventBusContext.context);
        });
        // 修改当前应用的某个任务
        addProcessor(OptActionKind.MODIFY, "/task/{taskCode}", eventBusContext -> {
            if (eventBusContext.req.identOptInfo.getAccountId().longValue() != DewAuthConstant.AK_SK_IDENT_ACCOUNT_FLAG) {
                throw new UnAuthorizedException("该方法仅允许AKSK认证模式下使用");
            }
            return modifyTask(
                    eventBusContext.req.params.get("taskCode"),
                    eventBusContext.req.params.getOrDefault("cron", ""),
                    eventBusContext.req.body(String.class),
                    eventBusContext.req.identOptInfo.getAppCode(),
                    eventBusContext.context);
        });
        // 删除当前应用的某个任务
        addProcessor(OptActionKind.DELETE, "/task/{taskCode}", eventBusContext -> {
            if (eventBusContext.req.identOptInfo.getAccountId().longValue() != DewAuthConstant.AK_SK_IDENT_ACCOUNT_FLAG) {
                throw new UnAuthorizedException("该方法仅允许AKSK认证模式下使用");
            }
            return deleteTask(
                    eventBusContext.req.params.get("taskCode"),
                    eventBusContext.req.identOptInfo.getAppCode(),
                    eventBusContext.context);
        });
        // 执行当前应用的某个任务
        addProcessor(OptActionKind.CREATE, "/exec/{taskCode}", eventBusContext ->
                execTask(
                        eventBusContext.req.params.get("taskCode"),
                        eventBusContext.req.identOptInfo.getUnauthorizedAppCode(),
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

                        initTasks(initTask.getFun(), initTask.getRelAppCode(), context);
                        taskDefs.stream().filter(def ->
                                !def.getCode().isBlank()).forEach(def ->
                                addTask(def.getCode(), def.getCron(), def.getFun(), def.getRelAppCode(), context));
                    }
                })
                .onFailure(e -> context.helper.error(e));
    }

    public static Future<Void> initTasks(String funs, String appCode, ProcessContext context) {
        ScriptProcessor.init(appCode, funs);
        var taskDef = TaskDef.builder()
                .code("")
                .cron("")
                .fun(funs)
                .relAppCode(appCode)
                .build();
        return context.sql.tx(context, () ->
                context.sql.getOne(new HashMap<>() {
                    {
                        put("code", "");
                        put("rel_app_code", appCode);
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

    public static Future<Void> addTask(String code, String cron, String fun, String appCode, ProcessContext context) {
        ScriptProcessor.add(appCode, code, fun);
        if (!cron.isBlank()) {
            CronHelper.addJob(code, appCode, cron, (name, group) -> {
                if (!context.cache.isLeader()) {
                    return;
                }
                execTask(code, appCode, new ArrayList<>(), true, null, context);
            });
        }
        var taskDef = TaskDef.builder()
                .code(code)
                .cron(cron)
                .fun(fun)
                .relAppCode(appCode)
                .build();
        return context.sql.save(taskDef)
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> modifyTask(String code, String cron, String fun, String appCode, ProcessContext context) {
        ScriptProcessor.add(appCode, code, fun);
        CronHelper.removeJob(code, appCode);
        if (!cron.isBlank()) {
            CronHelper.addJob(code, appCode, cron, (name, group) -> {
                if (!context.cache.isLeader()) {
                    return;
                }
                execTask(code, appCode, new ArrayList<>(), true, null, context);
            });
        }
        return context.helper.notExistToError(
                context.sql.exists(new HashMap<>() {
                    {
                        put("code", code);
                        put("rel_app_code", appCode);
                    }
                }, TaskDef.class), () -> new NotFoundException("找不到对应的任务[" + code + "]"))
                .compose(resp ->
                        context.sql.update(new HashMap<>() {
                            {
                                put("code", code);
                                put("rel_app_code", appCode);
                            }
                        }, TaskDef.builder()
                                .cron(cron)
                                .fun(fun)
                                .build()))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> deleteTask(String code, String appCode, ProcessContext context) {
        CronHelper.removeJob(code, appCode);
        return context.helper.notExistToError(
                context.sql.exists(new HashMap<>() {
                    {
                        put("code", code);
                        put("rel_app_code", appCode);
                    }
                }, TaskDef.class), () -> new NotFoundException("找不到对应的任务[" + code + "]"))
                .compose(resp ->
                        context.sql.softDelete(new HashMap<>() {
                            {
                                put("code", code);
                                put("rel_app_code", appCode);
                            }
                        }, TaskDef.class))
                .compose(resp -> context.helper.success());
    }

    public static Future<Object> execTask(String code, String appCode, List<?> parameters, Boolean fromTimer,
                                          IdentOptExchangeInfo identOptCacheInfo, ProcessContext context) {
        if (!fromTimer) {
            return _vertx.getOrCreateContext().executeBlocking(promise -> {
                try {
                    var result = ScriptProcessor.execute(appCode, code, parameters, identOptCacheInfo);
                    promise.complete(result);
                } catch (Exception e) {
                    log.warn("Execute task error: {}", e.getMessage(), e);
                    promise.fail(e);
                }
            });
        }
        log.trace("Executing timer task[{}-{}]", appCode, code);
        return context.helper.notExistToError(
                context.sql.getOne(new HashMap<>() {
                    {
                        put("code", code);
                        put("rel_app_code", appCode);
                    }
                }, TaskDef.class), () -> new NotFoundException("找不到对应的任务[" + code + "]"))
                .compose(taskDef ->
                        context.sql.save(TaskInst.builder()
                                .startTime(System.currentTimeMillis())
                                .endTime(0L)
                                .success(false)
                                .message("")
                                .relTaskDefCode(taskDef.getCode())
                                .build()))
                .compose(taskInstId ->
                        _vertx.getOrCreateContext().executeBlocking(promise -> {
                            try {
                                var result = ScriptProcessor.execute(appCode, code, parameters, null);
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
