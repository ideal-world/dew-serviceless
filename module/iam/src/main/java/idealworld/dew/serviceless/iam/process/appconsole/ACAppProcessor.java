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

package idealworld.dew.serviceless.iam.process.appconsole;

import com.ecfront.dew.common.Page;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.framework.util.KeyHelper;
import idealworld.dew.serviceless.iam.domain.ident.AccountIdent;
import idealworld.dew.serviceless.iam.domain.ident.App;
import idealworld.dew.serviceless.iam.domain.ident.AppIdent;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.app.AppIdentModifyReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.app.AppIdentResp;
import io.vertx.core.Future;

import java.util.HashMap;

/**
 * 应用控制台下的应用控制器.
 *
 * @author gudaoxuri
 */
public class ACAppProcessor {

    {
        // 添加当前应用的认证
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/app/app/ident", addAppIdent());
        // 修改当前应用的某个认证
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/app/app/ident/{appIdentId}", modifyAppIdent());
        // 获取当前应用的认证列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/app/ident", pageAppIdents());
        // 删除当前应用的某个认证
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/app/app/ident/{appIdentId}", deleteAppIdent());

        // 获取当前应用的某个认证SK
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/app/ident/{appIdentId}/sk", showSk());
        // 获取当前应用的公钥
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/app/ident/{appIdentId}/publicKey", showPublicKey());
        // 获取当前应用的私钥
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/app/ident/{appIdentId}/privateKey", showPrivateKey());
    }

    public ProcessFun<Long> addAppIdent() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var appIdentAddReq = context.req.body(AppIdentAddReq.class);
            var appIdent = context.helper.convert(appIdentAddReq, AppIdent.class);
            appIdent.setAk(KeyHelper.generateAk());
            appIdent.setSk(KeyHelper.generateSk(appIdent.getAk()));
            appIdent.setRelAppId(relAppId);
            return context.fun.sql.save(appIdent)
                    .compose(saveAppIdentId ->
                            ExchangeProcessor.changeAppIdent(appIdent, relAppId, relTenantId, context)
                                    .compose(r -> context.helper.success(saveAppIdentId)));
        };
    }

    public ProcessFun<Void> modifyAppIdent() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var appIdentId = Long.parseLong(context.req.params.get("appIdentId"));
            var appIdentModifyReq = context.req.body(AppIdentModifyReq.class);
            return context.fun.sql.update(
                    new HashMap<>() {
                        {
                            put("id", appIdentId);
                            put("rel_app_id", relAppId);
                        }
                    },
                    context.helper.convert(appIdentModifyReq, AppIdent.class))
                    .compose(resp ->
                            context.fun.sql.getOne(new HashMap<>() {
                                {
                                    put("id", appIdentId);
                                    put("rel_app_id", relAppId);
                                }
                            }, AppIdent.class)
                                    .compose(appIdent ->
                                            ExchangeProcessor.changeAppIdent(appIdent, relAppId, relTenantId, context)));
        };
    }

    public ProcessFun<Page<AppIdentResp>> pageAppIdents() {
        return context -> {
            var relAppId = context.req.identOptInfo.getAppId();
            var note = context.req.params.getOrDefault("note", null);
            var whereParameters = new HashMap<String, Object>() {
                {
                    put("rel_app_id", relAppId);
                }
            };
            if (note != null && !note.isBlank()) {
                whereParameters.put("%note", "%" + note + "%");
            }
            return context.fun.sql.page(
                    whereParameters,
                    context.req.pageNumber(),
                    context.req.pageSize(),
                    AppIdent.class)
                    .compose(appIdents -> context.helper.success(appIdents, AppIdentResp.class));
        };
    }

    public ProcessFun<Void> deleteAppIdent() {
        return context -> {
            var relAppId = context.req.identOptInfo.getAppId();
            var appIdentId = Long.parseLong(context.req.params.get("appIdentId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", relAppId);
                            put("rel_app_id", appIdentId);
                        }
                    },
                    AppIdent.class)
                    .compose(fetchAppIdent ->
                            context.fun.sql.softDelete(
                                    new HashMap<>() {
                                        {
                                            put("id", relAppId);
                                            put("rel_app_id", appIdentId);
                                        }
                                    },
                                    AppIdent.class)
                                    .compose(resp ->
                                            ExchangeProcessor.deleteAppIdent(fetchAppIdent.getAk(), context)));
        };
    }

    public ProcessFun<String> showSk() {
        return context -> {
            var relAppId = context.req.identOptInfo.getAppId();
            var appIdentId = Long.parseLong(context.req.params.get("appIdentId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", relAppId);
                            put("rel_app_id", appIdentId);
                        }
                    },
                    AccountIdent.class)
                    .compose(fetchAppIdent -> Future.succeededFuture(fetchAppIdent.getSk()));
        };
    }

    public ProcessFun<String> showPublicKey() {
        return context -> {
            var relAppId = context.req.identOptInfo.getAppId();
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", relAppId);
                        }
                    },
                    App.class)
                    .compose(fetchApp -> Future.succeededFuture(fetchApp.getPubKey()));
        };
    }

    public ProcessFun<String> showPrivateKey() {
        return context -> {
            var relAppId = context.req.identOptInfo.getAppId();
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", relAppId);
                        }
                    },
                    App.class)
                    .compose(fetchApp -> Future.succeededFuture(fetchApp.getPriKey()));
        };
    }

}
