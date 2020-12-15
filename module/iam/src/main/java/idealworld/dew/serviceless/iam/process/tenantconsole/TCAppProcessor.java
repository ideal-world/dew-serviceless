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
import idealworld.dew.serviceless.iam.process.common.CommonProcessor;
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
public class TCAppProcessor {

    {
        // 添加当前租户的应用
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/app", addApp());
        // 修改当前租户的某个应用
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/app/{appId}", modifyApp());
        // 获取当前租户的某个应用信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/app/{appId}", getApp());
        // 获取当前租户的应用列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/app", pageApps());
    }

    public ProcessFun<Long> addApp() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var appAddReq = context.helper.parseBody(context.req.body, AppAddReq.class);
            return context.fun.sql.exists(
                    new HashMap<>() {
                        {
                            put("name", appAddReq.getName());
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    App.class,
                    context)
                    .compose(existsResult -> {
                        if (existsResult) {
                            return context.helper.error(new ConflictException("应用名称已存在"));
                        }
                        return context.helper.success(null);
                    })
                    .compose(resp -> {
                        var app = $.bean.copyProperties(appAddReq, App.class);
                        var keys = $.security.asymmetric.generateKeys("RSA", 1024);
                        app.setPubKey(keys.get("PublicKey"));
                        app.setPriKey(keys.get("PrivateKey"));
                        app.setRelTenantId(relTenantId);
                        app.setStatus(CommonStatus.ENABLED);
                        return context.fun.sql.save(app, context);
                    })
                    .compose(appId -> {
                        exchangeProcessor.enableApp(appId, relTenantId);
                        return sendMQBySave(
                                saveR,
                                App.class
                        );
                    });
        };
    }

    public ProcessFun<Void> modifyApp() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var appId = Long.parseLong(context.req.params.get("appId"));
            var appModifyReq = context.helper.parseBody(context.req.body, AppModifyReq.class);
            return CommonProcessor.checkAppMembership(appId, relTenantId, context)
                    .compose(resp ->
                            context.fun.sql.update(new HashMap<>() {
                                {
                                    put("id", appId);
                                    put("rel_tenant_id", relTenantId);
                                }
                            },
                                    context.helper.convert(appModifyReq, App.class),
                                    context)
                    )
                    .compose(resp -> {
                        sendMQByUpdate(
                                updateEntity(appUpdate),
                                App.class,
                                appId
                        );
                        if (appModifyReq.getStatus() != null) {
                            if (appModifyReq.getStatus() == CommonStatus.ENABLED) {
                                exchangeProcessor.enableApp(appId, relTenantId);
                            } else {
                                exchangeProcessor.disableApp(appId, relTenantId);
                            }
                        }
                        return Future.succeededFuture(resp);
                    });
        };
    }

    public ProcessFun<AppResp> getApp() {
        return context -> context.fun.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", context.req.params.get("tenantIdentId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                App.class, context)
                .compose(app -> context.helper.success(app, AppResp.class));
    }

    public ProcessFun<Page<AppResp>> pageApps() {
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
                    TenantIdent.class, context
            )
                    .compose(apps -> context.helper.success(apps, AppResp.class));
        };
    }

}
