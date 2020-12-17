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

package idealworld.dew.serviceless.iam.exchange;

import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectOperatorKind;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.domain.ident.App;
import idealworld.dew.serviceless.iam.domain.ident.AppIdent;
import idealworld.dew.serviceless.iam.domain.ident.Tenant;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeProcessor {

    public static void publish(OptActionKind actionKind, String subjectCategory, Object subjectId, Object detailData, ProcessContext context) {
        context.fun.eb.publish(
                IAMConstant.MODULE_GATEWAY_NAME,
                actionKind,
                "http://" + context.moduleName + "/" + subjectCategory + "/" + subjectId,
                JsonObject.mapFrom(detailData).toString(),
                new HashMap<>());
    }

    public static Future<Void> cacheAppIdents(ProcessContext context) {
        if (!context.fun.cache.isLeader()) {
            return context.helper.success();
        }
        return context.fun.sql.list(
                String.format("SELECT ident.ak, ident.sk, ident.rel_app_id, ident.valid_time, app.rel_tenant_id FROM %s AS ident" +
                                "  INNER JOIN %s app ON app.id = ident.rel_app_id AND app.status = #{status}" +
                                "  INNER JOIN %s tenant ON tenant.id = app.rel_tenant_id AND tenant.status = #{status}" +
                                "  WHERE ident.valid_time > #{valid_time}",
                        new AppIdent().tableName(), new App().tableName(), new Tenant().tableName()),
                new HashMap<>() {
                    {
                        put("status", CommonStatus.ENABLED);
                        put("valid_time", new Date());
                    }
                })
                .compose(appIdents ->
                        CompositeFuture.all(appIdents.stream()
                                .map(appIdent -> {
                                    var ak = appIdent.getString("ident.ak");
                                    var sk = appIdent.getString("ident.sk");
                                    var appId = appIdent.getLong("ident.rel_app_id");
                                    var validTime = Date.from(appIdent.getInstant("ident.valid_time"));
                                    var tenantId = appIdent.getLong("app.rel_tenant_id");
                                    return changeAppIdent(ak, sk, validTime, appId, tenantId, context);
                                })
                                .collect(Collectors.toList())))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> enableTenant(Long tenantId, ProcessContext context) {
        return context.fun.sql.list(
                String.format("SELECT ident.ak, ident.sk, ident.rel_app_id, ident.valid_time FROM %s AS ident" +
                                "  INNER JOIN %s app ON app.id = ident.rel_app_id AND app.status = #{status}" +
                                "  WHERE app.rel_tenant_id = #{rel_tenant_id} AND ident.valid_time > #{valid_time}",
                        new AppIdent().tableName(), new App().tableName()),
                new HashMap<>() {
                    {
                        put("status", CommonStatus.ENABLED);
                        put("rel_tenant_id", tenantId);
                        put("valid_time", new Date());
                    }
                })
                .compose(appIdents ->
                        CompositeFuture.all(appIdents.stream()
                                .map(appIdent -> {
                                    var ak = appIdent.getString("ident.ak");
                                    var sk = appIdent.getString("ident.sk");
                                    var appId = appIdent.getLong("ident.rel_app_id");
                                    var validTime = Date.from(appIdent.getInstant("ident.valid_time"));
                                    return changeAppIdent(ak, sk, validTime, appId, tenantId, context);
                                })
                                .collect(Collectors.toList())))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> disableTenant(Long tenantId, ProcessContext context) {
        return context.fun.sql.list(
                String.format("SELECT ident.ak FROM %s AS ident" +
                        "  INNER JOIN %s app ON app.id = ident.rel_app_id" +
                        "  WHERE app.rel_tenant_id = #{rel_tenant_id}", new AppIdent().tableName(), new App().tableName()),
                new HashMap<>() {
                    {
                        put("rel_tenant_id", tenantId);
                    }
                })
                .compose(appIdents ->
                        CompositeFuture.all(appIdents.stream()
                                .map(appIdent -> {
                                    var ak = appIdent.getString("ident.ak");
                                    return deleteAppIdent(ak, context);
                                })
                                .collect(Collectors.toList())))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> enableApp(Long appId, Long tenantId, ProcessContext context) {
        return context.fun.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", appId);
                    }
                },
                App.class)
                .compose(app -> {
                    var publicKey = app.getPubKey();
                    var privateKey = app.getPriKey();
                    return context.fun.cache.set(IAMConstant.CACHE_APP_INFO + appId, tenantId + "\n" + publicKey + "\n" + privateKey);
                })
                .compose(resp ->
                        context.fun.sql.list(
                                String.format("SELECT ak, sk, valid_time FROM %s" +
                                        "  WHERE rel_app_id = #{rel_app_id} AND valid_time > #{valid_time}", new AppIdent().tableName()),
                                new HashMap<>() {
                                    {
                                        put("rel_app_id", appId);
                                        put("valid_time", new Date());
                                    }
                                }))
                .compose(appIdents ->
                        CompositeFuture.all(appIdents.stream()
                                .map(appIdent -> {
                                    var ak = appIdent.getString("ak");
                                    var sk = appIdent.getString("sk");
                                    var validTime = Date.from(appIdent.getInstant("valid_time"));
                                    return changeAppIdent(ak, sk, validTime, appId, tenantId, context);
                                })
                                .collect(Collectors.toList())))
                .compose(resp -> context.helper.success());
    }

    public static Future<Void> disableApp(Long appId, Long tenantId, ProcessContext context) {
        return context.fun.sql.list(
                new HashMap<>() {
                    {
                        put("rel_app_id", appId);
                    }
                },
                AppIdent.class)
                .compose(appIdents ->
                        CompositeFuture.all(appIdents.stream().map(appIdent -> deleteAppIdent(appIdent.getAk(), context)).collect(Collectors.toList()))
                )
                .compose(resp -> context.fun.cache.del(IAMConstant.CACHE_APP_INFO + appId));
    }

    public static Future<Void> changeAppIdent(AppIdent appIdent, Long appId, Long tenantId, ProcessContext context) {
        return changeAppIdent(appIdent.getAk(), appIdent.getSk(), appIdent.getValidTime(), appId, tenantId, context);
    }

    public static Future<Void> deleteAppIdent(String ak, ProcessContext context) {
        return context.fun.cache.del(IAMConstant.CACHE_APP_AK + ak);
    }

    private static Future<Void> changeAppIdent(String ak, String sk, Date validTime, Long appId, Long tenantId, ProcessContext context) {
        return context.fun.cache.del(IAMConstant.CACHE_APP_AK + ak)
                .compose(resp -> {
                    if (validTime == null) {
                        return context.fun.cache.set(IAMConstant.CACHE_APP_AK + ak, sk + ":" + tenantId + ":" + appId);
                    } else {
                        return context.fun.cache.setex(IAMConstant.CACHE_APP_AK + ak, sk + ":" + tenantId + ":" + appId,
                                (validTime.getTime() - System.currentTimeMillis()) / 1000);
                    }
                });
    }

    public static Future<Void> addPolicy(AuthPolicyInfo policyInfo, ProcessContext context) {
        if ((policyInfo.getSubjectOperator() == AuthSubjectOperatorKind.INCLUDE
                || policyInfo.getSubjectOperator() == AuthSubjectOperatorKind.LIKE)
                && policyInfo.subjectKind != AuthSubjectKind.GROUP_NODE) {
            return context.helper.error(new BadRequestException("权限主体运算类型为INCLUDE/LIKE时权限主体只能为GROUP_NODE"));
        }
        policyInfo.setResourceUri(URIHelper.formatUri(policyInfo.getResourceUri()));
        var key = IAMConstant.CACHE_AUTH_POLICY
                + policyInfo.getResourceUri().replace("//", "") + ":"
                + policyInfo.getActionKind().toString().toLowerCase();
        return context.fun.cache.get(key)
                .compose(strPolicyValue -> {
                    var policyValue = strPolicyValue != null ? new JsonObject(strPolicyValue) : new JsonObject();
                    if (!policyValue.containsKey(policyInfo.subjectOperator.toString().toLowerCase())) {
                        policyValue.put(policyInfo.subjectOperator.toString().toLowerCase(), new JsonObject());
                    }
                    if (!policyValue.getJsonObject(policyInfo.subjectOperator.toString().toLowerCase()).containsKey(policyInfo.subjectKind.toString().toLowerCase())) {
                        policyValue.getJsonObject(policyInfo.subjectOperator.toString().toLowerCase()).put(policyInfo.subjectKind.toString().toLowerCase(), new JsonArray());
                    }
                    policyValue.getJsonObject(policyInfo.subjectOperator.toString().toLowerCase()).getJsonArray(policyInfo.subjectKind.toString().toLowerCase()).add(policyInfo.getSubjectId());
                    return context.fun.cache.set(key, policyValue.toString());
                });
    }

    public static Future<Void> removePolicy(AuthPolicyInfo policyInfo, ProcessContext context) {
        policyInfo.setResourceUri(URIHelper.formatUri(policyInfo.getResourceUri()));
        var key = IAMConstant.CACHE_AUTH_POLICY
                + policyInfo.getResourceUri().replace("//", "") + ":"
                + policyInfo.getActionKind().toString().toLowerCase();
        return context.fun.cache.get(key)
                .compose(strPolicyValue -> {
                    if (strPolicyValue == null) {
                        return context.helper.success();
                    }
                    var policyValue = new JsonObject(strPolicyValue);
                    if (!policyValue.containsKey(policyInfo.subjectOperator.toString().toLowerCase())
                            || !policyValue.getJsonObject(policyInfo.subjectOperator.toString().toLowerCase()).containsKey(policyInfo.subjectKind.toString().toLowerCase())) {
                        return context.helper.success();
                    }
                    policyValue.getJsonObject(policyInfo.subjectOperator.toString().toLowerCase()).getJsonObject(policyInfo.subjectKind.toString().toLowerCase()).remove(policyInfo.getSubjectId());
                    return context.fun.cache.set(key, policyValue.toString());
                });
    }

    public static Future<Void> removePolicy(String resourceUri, OptActionKind actionKind, ProcessContext context) {
        resourceUri = URIHelper.formatUri(resourceUri);
        var key = IAMConstant.CACHE_AUTH_POLICY
                + resourceUri.replace("//", "") + ":"
                + actionKind.toString().toLowerCase();
        return context.fun.cache.del(key);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthPolicyInfo implements Serializable {

        private String resourceUri;

        private OptActionKind actionKind;

        private AuthSubjectKind subjectKind;

        private String subjectId;

        private AuthSubjectOperatorKind subjectOperator;

    }

}
