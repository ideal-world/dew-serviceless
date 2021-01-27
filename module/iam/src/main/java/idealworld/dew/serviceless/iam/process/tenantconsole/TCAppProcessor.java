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

package idealworld.dew.serviceless.iam.process.tenantconsole;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.serviceless.iam.domain.ident.App;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.process.IAMBasicProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppModifyReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppResp;
import io.vertx.core.Future;

import java.util.HashMap;

/**
 * 租户控制台下的应用控制器.
 *
 * @author gudaoxuri
 */
public class TCAppProcessor extends EventBusProcessor {

    {
        // 添加当前租户的应用
        addProcessor(OptActionKind.CREATE, "/console/tenant/app", eventBusContext ->
                addApp(eventBusContext.req.body(AppAddReq.class), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context, false));
        // 修改当前租户的某个应用
        addProcessor(OptActionKind.PATCH, "/console/tenant/app/{appId}", eventBusContext ->
                modifyApp(Long.parseLong(eventBusContext.req.params.get("appId")), eventBusContext.req.body(AppModifyReq.class), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前租户的某个应用信息
        addProcessor(OptActionKind.FETCH, "/console/tenant/app/{appId}", eventBusContext ->
                getApp(Long.parseLong(eventBusContext.req.params.get("appId")), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前租户的某个应用的Id
        addProcessor(OptActionKind.FETCH, "/console/tenant/app/id", eventBusContext ->
                getAppIdByCode(eventBusContext.req.params.get("appCode"), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前租户的应用列表信息
        addProcessor(OptActionKind.FETCH, "/console/tenant/app", eventBusContext ->
                pageApps(eventBusContext.req.params.getOrDefault("name", null), eventBusContext.req.pageNumber(), eventBusContext.req.pageSize(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
    }

    public TCAppProcessor(String moduleName) {
        super(moduleName);
    }

    public static Future<Long> addApp(AppAddReq appAddReq, Long relTenantId, ProcessContext context, Boolean createIamApp) {
        var appCode = createIamApp ? context.moduleName : "ap" + $.field.createUUID().toLowerCase();
        return context.helper.existToError(
                context.sql.exists(
                        new HashMap<>() {
                            {
                                put("name", appAddReq.getName());
                                put("rel_tenant_id", relTenantId);
                            }
                        },
                        App.class), () -> new ConflictException("应用名称已存在"))
                .compose(resp -> {
                    var app = $.bean.copyProperties(appAddReq, App.class);
                    var keys = $.security.asymmetric.generateKeys("RSA", 1024);
                    app.setOpenId(appCode);
                    app.setPubKey(keys.get("PublicKey"));
                    app.setPriKey(keys.get("PrivateKey"));
                    app.setRelTenantId(relTenantId);
                    app.setStatus(CommonStatus.ENABLED);
                    return context.sql.save(app);
                })
                .compose(appId ->
                        ExchangeProcessor.enableApp(appCode, appId, relTenantId, context)
                                .compose(r ->
                                        context.helper.success(appId)));
    }

    public static Future<Void> modifyApp(Long appId, AppModifyReq appModifyReq, Long relTenantId, ProcessContext context) {
        return IAMBasicProcessor.checkAppMembership(appId, relTenantId, context)
                .compose(resp -> {
                    if (appModifyReq.getName() != null) {
                        return context.helper.existToError(
                                context.sql.exists(
                                        new HashMap<>() {
                                            {
                                                put("!id", appId);
                                                put("name", appModifyReq.getName());
                                                put("rel_tenant_id", relTenantId);
                                            }
                                        },
                                        App.class), () -> new ConflictException("应用名称已存在"));
                    } else {
                        return context.helper.success(true);
                    }
                })
                .compose(resp ->
                        context.sql.update(
                                new HashMap<>() {
                                    {
                                        put("id", appId);
                                        put("rel_tenant_id", relTenantId);
                                    }
                                },
                                context.helper.convert(appModifyReq, App.class)))
                .compose(resp ->
                        context.sql.getOne(String.format("SELECT open_id FROM %s WHERE id = #{id}", new App().tableName()), new HashMap<>() {
                            {
                                put("id", appId);
                            }
                        }))
                .compose(appInfo -> {
                    if (appModifyReq.getStatus() != null) {
                        if (appModifyReq.getStatus() == CommonStatus.ENABLED) {
                            return ExchangeProcessor.enableApp(appInfo.getString("open_id"), appId, relTenantId, context);
                        } else {
                            return ExchangeProcessor.disableApp(appInfo.getString("open_id"), appId, relTenantId, context);
                        }
                    }
                    return context.helper.success(null);
                });
    }

    public static Future<AppResp> getApp(Long appId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", appId);
                        put("rel_tenant_id", relTenantId);
                    }
                },
                App.class)
                .compose(app -> context.helper.success(app, AppResp.class));
    }

    public static Future<Long> getAppIdByCode(String appCode, Long relTenantId, ProcessContext context) {
        return IAMBasicProcessor.getAppIdByCode(appCode, relTenantId, context);
    }

    public static Future<Page<AppResp>> pageApps(String name, Long pageNumber, Long pageSize, Long relTenantId, ProcessContext context) {
        var whereParameters = new HashMap<String, Object>() {
            {
                put("rel_tenant_id", relTenantId);
            }
        };
        if (name != null && !name.isBlank()) {
            whereParameters.put("%name", "%" + name + "%");
        }
        return context.sql.page(
                whereParameters,
                pageNumber,
                pageSize,
                App.class)
                .compose(apps -> context.helper.success(apps, AppResp.class));
    }

}
