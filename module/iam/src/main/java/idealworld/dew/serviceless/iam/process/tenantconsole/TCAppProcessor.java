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

package idealworld.dew.serviceless.iam.process.tenantconsole;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.serviceless.iam.domain.ident.App;
import idealworld.dew.serviceless.iam.domain.ident.TenantIdent;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.process.IAMBasicProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppModifyReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.app.AppResp;

import java.util.HashMap;

/**
 * 租户控制台下的应用控制器.
 *
 * @author gudaoxuri
 */
public class TCAppProcessor {

    static {
        // 添加当前租户的应用
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/app", addApp());
        // 修改当前租户的某个应用
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/app/{appId}", modifyApp());
        // 获取当前租户的某个应用信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/app/{appId}", getApp());
        // 获取当前租户的应用列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/app", pageApps());
    }

    public static ProcessFun<Long> addApp() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var appAddReq = context.req.body(AppAddReq.class);
            return context.helper.existToError(
                    context.fun.sql.exists(
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
                        app.setPubKey(keys.get("PublicKey"));
                        app.setPriKey(keys.get("PrivateKey"));
                        app.setRelTenantId(relTenantId);
                        app.setStatus(CommonStatus.ENABLED);
                        return context.fun.sql.save(app);
                    })
                    .compose(appId ->
                            ExchangeProcessor.enableApp(appId, relTenantId, context)
                                    .compose(r ->
                                            context.helper.success(appId)));
        };
    }

    public static ProcessFun<Void> modifyApp() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var appId = Long.parseLong(context.req.params.get("appId"));
            var appModifyReq = context.req.body(AppModifyReq.class);
            return IAMBasicProcessor.checkAppMembership(appId, relTenantId, context)
                    .compose(resp ->
                            context.fun.sql.update(
                                    new HashMap<>() {
                                        {
                                            put("id", appId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    context.helper.convert(appModifyReq, App.class)))
                    .compose(resp -> {
                        if (appModifyReq.getStatus() != null) {
                            if (appModifyReq.getStatus() == CommonStatus.ENABLED) {
                                return ExchangeProcessor.enableApp(appId, relTenantId, context);
                            } else {
                                return ExchangeProcessor.disableApp(appId, relTenantId, context);
                            }
                        }
                        return context.helper.success(resp);
                    });
        };
    }

    public static ProcessFun<AppResp> getApp() {
        return context -> context.fun.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", context.req.params.get("tenantIdentId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                App.class)
                .compose(app -> context.helper.success(app, AppResp.class));
    }

    public static ProcessFun<Page<AppResp>> pageApps() {
        return context -> {
            var name = context.req.params.getOrDefault("name", null);
            var whereParameters = new HashMap<String, Object>() {
                {
                    put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                }
            };
            if (name != null && !name.isBlank()) {
                whereParameters.put("%name", "%" + name + "%");
            }
            return context.fun.sql.page(
                    whereParameters,
                    context.req.pageNumber(),
                    context.req.pageSize(),
                    TenantIdent.class)
                    .compose(apps -> context.helper.success(apps, AppResp.class));
        };
    }

}
