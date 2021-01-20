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

package idealworld.dew.serviceless.iam.process.appconsole;

import com.ecfront.dew.common.Page;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.util.KeyHelper;
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
public class ACAppProcessor extends EventBusProcessor {

    {
        // 添加当前应用的认证
        addProcessor(OptActionKind.CREATE, "/console/app/app/ident", eventBusContext ->
                addAppIdent(eventBusContext.req.body(AppIdentAddReq.class), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前应用的某个认证
        addProcessor(OptActionKind.PATCH, "/console/app/app/ident/{appIdentId}", eventBusContext ->
                modifyAppIdent(Long.parseLong(eventBusContext.req.params.get("appIdentId")), eventBusContext.req.body(AppIdentModifyReq.class), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的认证列表信息
        addProcessor(OptActionKind.FETCH, "/console/app/app/ident", eventBusContext ->
                pageAppIdents(eventBusContext.req.params.getOrDefault("note", null), eventBusContext.req.pageNumber(), eventBusContext.req.pageSize(), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 删除当前应用的某个认证
        addProcessor(OptActionKind.DELETE, "/console/app/app/ident/{appIdentId}", eventBusContext ->
                deleteAppIdent(Long.parseLong(eventBusContext.req.params.get("appIdentId")), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));

        // 获取当前应用的某个认证SK
        addProcessor(OptActionKind.FETCH, "/console/app/app/ident/{appIdentId}/sk", eventBusContext ->
                showSk(Long.parseLong(eventBusContext.req.params.get("appIdentId")), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的公钥
        addProcessor(OptActionKind.FETCH, "/console/app/app/publicKey", eventBusContext ->
                showPublicKey(eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的私钥
        addProcessor(OptActionKind.FETCH, "/console/app/app/privateKey", eventBusContext ->
                showPrivateKey(eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
    }

    public ACAppProcessor(String moduleName) {
        super(moduleName);
    }

    public static Future<Long> addAppIdent(AppIdentAddReq appIdentAddReq, Long relAppId, Long relTenantId, ProcessContext context) {
        var appIdent = context.helper.convert(appIdentAddReq, AppIdent.class);
        appIdent.setAk(KeyHelper.generateAk());
        appIdent.setSk(KeyHelper.generateSk(appIdent.getAk()));
        appIdent.setRelAppId(relAppId);
        return context.sql.save(appIdent)
                .compose(saveAppIdentId ->
                        ExchangeProcessor.changeAppIdent(appIdent, relAppId, relTenantId, context)
                                .compose(r -> context.helper.success(saveAppIdentId)));
    }

    public static Future<Void> modifyAppIdent(Long appIdentId, AppIdentModifyReq appIdentModifyReq, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.update(
                new HashMap<>() {
                    {
                        put("id", appIdentId);
                        put("rel_app_id", relAppId);
                    }
                },
                context.helper.convert(appIdentModifyReq, AppIdent.class))
                .compose(resp ->
                        context.sql.getOne(new HashMap<>() {
                            {
                                put("id", appIdentId);
                                put("rel_app_id", relAppId);
                            }
                        }, AppIdent.class)
                                .compose(appIdent ->
                                        ExchangeProcessor.changeAppIdent(appIdent, relAppId, relTenantId, context)));
    }

    public static Future<Page<AppIdentResp>> pageAppIdents(String note, Long pageNumber, Long pageSize, Long relAppId, Long relTenantId, ProcessContext context) {
        var whereParameters = new HashMap<String, Object>() {
            {
                put("rel_app_id", relAppId);
            }
        };
        if (note != null && !note.isBlank()) {
            whereParameters.put("%note", "%" + note + "%");
        }
        return context.sql.page(
                whereParameters,
                pageNumber,
                pageSize,
                AppIdent.class)
                .compose(appIdents -> context.helper.success(appIdents, AppIdentResp.class));
    }

    public static Future<Void> deleteAppIdent(Long appIdentId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", appIdentId);
                        put("rel_app_id", relAppId);
                    }
                },
                AppIdent.class)
                .compose(fetchAppIdent ->
                        context.sql.softDelete(
                                new HashMap<>() {
                                    {
                                        put("id", appIdentId);
                                        put("rel_app_id", relAppId);
                                    }
                                },
                                AppIdent.class)
                                .compose(resp ->
                                        ExchangeProcessor.deleteAppIdent(fetchAppIdent.getAk(), context)));
    }

    public static Future<String> showSk(Long appIdentId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", appIdentId);
                        put("rel_app_id", relAppId);
                    }
                },
                AppIdent.class)
                .compose(fetchAppIdent -> Future.succeededFuture(fetchAppIdent.getSk()));
    }

    public static Future<String> showPublicKey(Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", relAppId);
                    }
                },
                App.class)
                .compose(fetchApp -> Future.succeededFuture(fetchApp.getPubKey()));
    }

    public static Future<String> showPrivateKey(Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", relAppId);
                    }
                },
                App.class)
                .compose(fetchApp -> Future.succeededFuture(fetchApp.getPriKey()));
    }

}
