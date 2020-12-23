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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.serviceless.iam.domain.auth.AuthPolicy;
import idealworld.dew.serviceless.iam.domain.auth.Resource;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.process.IAMBasicProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy.AuthPolicyModifyReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy.AuthPolicyResp;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 应用控制台下的权限策略控制器.
 *
 * @author gudaoxuri
 */
public class ACAuthPolicyProcessor extends EventBusProcessor {

    public ACAuthPolicyProcessor(String moduleName) {
        super(moduleName);
    }

    {
        // 添加当前应用的权限策略
        addProcessor(OptActionKind.CREATE, "/console/app/authpolicy", eventBusContext ->
                addAuthPolicy(eventBusContext.req.body(AuthPolicyAddReq.class), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前应用的某个权限策略
        addProcessor(OptActionKind.PATCH, "/console/app/authpolicy/{authPolicyId}", eventBusContext ->
                modifyAuthPolicy(Long.parseLong(eventBusContext.req.params.get("authPolicyId")), eventBusContext.req.body(AuthPolicyModifyReq.class), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // getAuthPolicy
        addProcessor(OptActionKind.FETCH, "/console/app/authpolicy/{authPolicyId}", eventBusContext ->
                getAuthPolicy(Long.parseLong(eventBusContext.req.params.get("authPolicyId")), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的权限策略列表信息
        addProcessor(OptActionKind.FETCH, "/console/app/authpolicy", eventBusContext ->
                pageAuthPolicies(eventBusContext.req.params.getOrDefault("subjectKind", null), eventBusContext.req.pageNumber(), eventBusContext.req.pageSize(), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 删除当前应用的某个权限策略
        addProcessor(OptActionKind.DELETE, "/console/app/authpolicy/{authPolicyId}", eventBusContext ->
                deleteAuthPolicy(Long.parseLong(eventBusContext.req.params.get("authPolicyId")), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
    }

    public static Future<Void> addAuthPolicy(AuthPolicyAddReq authPolicyAddReq, Long relAppId, Long relTenantId, ProcessContext context) {
        if (!authPolicyAddReq.getRelSubjectIds().endsWith(",")) {
            authPolicyAddReq.setRelSubjectIds(authPolicyAddReq.getRelSubjectIds() + ",");
        }
        var subjectIds = new ArrayList<Long>();
        final String[] resourceUrl = new String[1];
        return context.helper.notExistToError(
                context.sql.getOne(
                        String.format("SELECT uri FROM %s" +
                                        " WHERE id = #{id} AND" +
                                        " ((rel_tenant_id = #{rel_tenant_id} AND rel_app_id = #{rel_app_id})" +
                                        " OR (expose_kind = #{expose_kind_tenant} AND rel_tenant_id = #{rel_tenant_id})" +
                                        " OR expose_kind = #{expose_kind_global})",
                                new Resource().tableName()),
                        new HashMap<>() {
                            {
                                put("id", authPolicyAddReq.getRelResourceId());
                                put("expose_kind_tenant", ExposeKind.TENANT);
                                put("expose_kind_global", ExposeKind.GLOBAL);
                                put("rel_app_id", relAppId);
                                put("rel_tenant_id", relTenantId);
                            }
                        }), () -> new UnAuthorizedException("权限策略对应的资源不合法"))
                .compose(fetchResourceUri -> {
                    resourceUrl[0] = fetchResourceUri.getString("uri");
                    subjectIds.addAll(Arrays.stream(authPolicyAddReq.getRelSubjectIds().split(","))
                            .filter(id -> !id.trim().isBlank())
                            .map(id -> Long.parseLong(id.trim()))
                            .collect(Collectors.toList()));
                    Future<Void> checkSubjectMembershipR = null;
                    switch (authPolicyAddReq.getRelSubjectKind()) {
                        case ROLE:
                            checkSubjectMembershipR = IAMBasicProcessor.checkRoleMembership(subjectIds, relAppId, relTenantId, context);
                            break;
                        case ACCOUNT:
                            checkSubjectMembershipR = IAMBasicProcessor.checkAccountMembership(subjectIds, relAppId, relTenantId, context);
                            break;
                        case APP:
                            checkSubjectMembershipR = IAMBasicProcessor.checkAppMembership(subjectIds, relTenantId, context);
                            break;
                        case TENANT:
                            if (subjectIds.size() == 1 && subjectIds.get(0).longValue() == relTenantId) {
                                checkSubjectMembershipR = Future.succeededFuture();
                            } else {
                                throw new UnAuthorizedException("租户不合法");
                            }
                            break;
                        case GROUP_NODE:
                            checkSubjectMembershipR = IAMBasicProcessor.checkGroupNodeMembership(subjectIds, relAppId, relTenantId, context);
                            break;
                    }
                    return checkSubjectMembershipR;
                })
                .compose(resp -> {
                    var whereParameters = new HashMap<String, Object>() {
                        {
                            put("rel_subject_kind", authPolicyAddReq.getRelSubjectKind());
                            // TODO 是否存在需要拆分判断
                            put("rel_subject_ids", authPolicyAddReq.getRelSubjectIds());
                            put("effective_time", authPolicyAddReq.getEffectiveTime());
                            put("expired_time", authPolicyAddReq.getExpiredTime());
                            put("rel_resource_id", authPolicyAddReq.getRelResourceId());
                        }
                    };
                    if (authPolicyAddReq.getActionKind() != null) {
                        whereParameters.put("action_kind", authPolicyAddReq.getActionKind());
                    }
                    return context.helper.existToError(
                            context.sql.exists(
                                    whereParameters,
                                    AuthPolicy.class), () -> new UnAuthorizedException("权限策略已存在"));
                })
                .compose(resp -> {
                    var authPolicy = context.helper.convert(authPolicyAddReq, AuthPolicy.class);
                    authPolicy.setRelTenantId(relTenantId);
                    authPolicy.setRelAppId(relAppId);
                    if (authPolicy.getActionKind() == null) {
                        var createAuthPolicy = $.bean.copyProperties(authPolicy, AuthPolicy.class);
                        createAuthPolicy.setActionKind(OptActionKind.CREATE);
                        var existsAuthPolicy = $.bean.copyProperties(authPolicy, AuthPolicy.class);
                        existsAuthPolicy.setActionKind(OptActionKind.EXISTS);
                        var fetchAuthPolicy = $.bean.copyProperties(authPolicy, AuthPolicy.class);
                        fetchAuthPolicy.setActionKind(OptActionKind.FETCH);
                        var modifyAuthPolicy = $.bean.copyProperties(authPolicy, AuthPolicy.class);
                        modifyAuthPolicy.setActionKind(OptActionKind.MODIFY);
                        var patchAuthPolicy = $.bean.copyProperties(authPolicy, AuthPolicy.class);
                        patchAuthPolicy.setActionKind(OptActionKind.PATCH);
                        var deleteAuthPolicy = $.bean.copyProperties(authPolicy, AuthPolicy.class);
                        deleteAuthPolicy.setActionKind(OptActionKind.DELETE);
                        return context.sql.save(new ArrayList<>() {
                            {
                                add(createAuthPolicy);
                                add(existsAuthPolicy);
                                add(fetchAuthPolicy);
                                add(modifyAuthPolicy);
                                add(patchAuthPolicy);
                                add(deleteAuthPolicy);
                            }
                        })
                                .compose(r -> {
                                    var exchangeProcess = new ArrayList<Future>();
                                    subjectIds.forEach(subjectId -> {
                                        exchangeProcess.add(
                                                ExchangeProcessor.addPolicy(
                                                        ExchangeProcessor.AuthPolicyInfo.builder()
                                                                .resourceUri(resourceUrl[0])
                                                                .actionKind(OptActionKind.CREATE)
                                                                .subjectKind(authPolicy.getRelSubjectKind())
                                                                .subjectOperator(authPolicy.getSubjectOperator())
                                                                .subjectId(subjectId + "")
                                                                .build(), context));
                                        exchangeProcess.add(
                                                ExchangeProcessor.addPolicy(
                                                        ExchangeProcessor.AuthPolicyInfo.builder()
                                                                .resourceUri(resourceUrl[0])
                                                                .actionKind(OptActionKind.EXISTS)
                                                                .subjectKind(authPolicy.getRelSubjectKind())
                                                                .subjectOperator(authPolicy.getSubjectOperator())
                                                                .subjectId(subjectId + "")
                                                                .build(), context));
                                        exchangeProcess.add(
                                                ExchangeProcessor.addPolicy(
                                                        ExchangeProcessor.AuthPolicyInfo.builder()
                                                                .resourceUri(resourceUrl[0])
                                                                .actionKind(OptActionKind.FETCH)
                                                                .subjectKind(authPolicy.getRelSubjectKind())
                                                                .subjectOperator(authPolicy.getSubjectOperator())
                                                                .subjectId(subjectId + "")
                                                                .build(), context));
                                        exchangeProcess.add(
                                                ExchangeProcessor.addPolicy(
                                                        ExchangeProcessor.AuthPolicyInfo.builder()
                                                                .resourceUri(resourceUrl[0])
                                                                .actionKind(OptActionKind.MODIFY)
                                                                .subjectKind(authPolicy.getRelSubjectKind())
                                                                .subjectOperator(authPolicy.getSubjectOperator())
                                                                .subjectId(subjectId + "")
                                                                .build(), context));
                                        exchangeProcess.add(
                                                ExchangeProcessor.addPolicy(
                                                        ExchangeProcessor.AuthPolicyInfo.builder()
                                                                .resourceUri(resourceUrl[0])
                                                                .actionKind(OptActionKind.PATCH)
                                                                .subjectKind(authPolicy.getRelSubjectKind())
                                                                .subjectOperator(authPolicy.getSubjectOperator())
                                                                .subjectId(subjectId + "")
                                                                .build(), context));
                                        exchangeProcess.add(
                                                ExchangeProcessor.addPolicy(
                                                        ExchangeProcessor.AuthPolicyInfo.builder()
                                                                .resourceUri(resourceUrl[0])
                                                                .actionKind(OptActionKind.DELETE)
                                                                .subjectKind(authPolicy.getRelSubjectKind())
                                                                .subjectOperator(authPolicy.getSubjectOperator())
                                                                .subjectId(subjectId + "")
                                                                .build(), context));
                                    });
                                    return CompositeFuture.all(exchangeProcess);
                                })
                                .compose(r -> context.helper.success());
                    } else {
                        return context.sql.save(authPolicy)
                                .compose(r ->
                                        CompositeFuture.all(subjectIds.stream()
                                                .map(id ->
                                                        ExchangeProcessor.addPolicy(
                                                                ExchangeProcessor.AuthPolicyInfo.builder()
                                                                        .resourceUri(resourceUrl[0])
                                                                        .actionKind(authPolicy.getActionKind())
                                                                        .subjectKind(authPolicy.getRelSubjectKind())
                                                                        .subjectOperator(authPolicy.getSubjectOperator())
                                                                        .subjectId(id + "")
                                                                        .build(), context))
                                                .collect(Collectors.toList())))
                                .compose(r -> context.helper.success());
                    }
                });
    }

    public static Future<Void> modifyAuthPolicy(Long authPolicyId, AuthPolicyModifyReq authPolicyModifyReq, Long relAppId, Long relTenantId, ProcessContext context) {
        if (authPolicyModifyReq.getRelSubjectKind() != null && authPolicyModifyReq.getRelSubjectIds() == null
                || authPolicyModifyReq.getRelSubjectKind() == null && authPolicyModifyReq.getRelSubjectIds() != null) {
            throw new BadRequestException("关联权限主体类型与关联权限主体Id必须同时存在");
        }
        return Future.succeededFuture()
                .compose(resp -> {
                    if (authPolicyModifyReq.getRelResourceId() == null) {
                        return Future.succeededFuture();
                    }
                    return context.helper.notExistToError(
                            context.sql.exists(
                                    String.format("SELECT id FROM %s" +
                                                    " WHERE id = #{id} AND " +
                                                    " ((rel_tenant_id = #{rel_tenant_id} AND rel_app_id = #{rel_app_id})" +
                                                    " OR (expose_kind = #{expose_kind_tenant} AND rel_tenant_id = #{rel_tenant_id})" +
                                                    " OR expose_kind = #{expose_kind_global})",
                                            new Resource().tableName()),
                                    new HashMap<>() {
                                        {
                                            put("id", authPolicyModifyReq.getRelResourceId());
                                            put("expose_kind_tenant", ExposeKind.TENANT);
                                            put("expose_kind_global", ExposeKind.GLOBAL);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    }), () -> new UnAuthorizedException("权限策略对应的资源不合法"));
                })
                .compose(resp -> {
                    if (authPolicyModifyReq.getRelSubjectIds() != null) {
                        if (!authPolicyModifyReq.getRelSubjectIds().endsWith(",")) {
                            authPolicyModifyReq.setRelSubjectIds(authPolicyModifyReq.getRelSubjectIds() + ",");
                        }
                        var subjectIds = Arrays.stream(authPolicyModifyReq.getRelSubjectIds().split(","))
                                .filter(id -> !id.trim().isBlank())
                                .map(id -> Long.parseLong(id.trim()))
                                .collect(Collectors.toList());
                        Future<Void> checkSubjectMembershipR = null;
                        switch (authPolicyModifyReq.getRelSubjectKind()) {
                            case ROLE:
                                checkSubjectMembershipR = IAMBasicProcessor.checkRoleMembership(subjectIds, relAppId, relTenantId, context);
                                break;
                            case ACCOUNT:
                                checkSubjectMembershipR = IAMBasicProcessor.checkAccountMembership(subjectIds, relAppId, relTenantId, context);
                                break;
                            case APP:
                                checkSubjectMembershipR = IAMBasicProcessor.checkAppMembership(subjectIds, relTenantId, context);
                                break;
                            case TENANT:
                                if (subjectIds.size() == 1 && subjectIds.get(0).longValue() == relTenantId) {
                                    checkSubjectMembershipR = Future.succeededFuture();
                                } else {
                                    throw new UnAuthorizedException("租户不合法");
                                }
                                break;
                            case GROUP_NODE:
                                checkSubjectMembershipR = IAMBasicProcessor.checkGroupNodeMembership(subjectIds, relAppId, relTenantId, context);
                                break;
                        }
                        return checkSubjectMembershipR;
                    }
                    return context.helper.success();
                })
                .compose(resp ->
                        context.sql.getOne(authPolicyId, AuthPolicy.class)
                )
                .compose(originalAuthPolicy ->
                        context.sql.update(
                                new HashMap<>() {
                                    {
                                        put("id", authPolicyId);
                                        put("rel_app_id", relAppId);
                                        put("rel_tenant_id", relTenantId);
                                    }
                                },
                                context.helper.convert(authPolicyModifyReq, AuthPolicy.class))
                                .compose(resp ->
                                        context.sql.getOne(
                                                new HashMap<String, Object>() {
                                                    {
                                                        put("id", originalAuthPolicy.getRelResourceId());
                                                    }
                                                },
                                                Resource.class)
                                )
                                .compose(originalAuthPolicyResource ->
                                        context.sql.getOne(
                                                new HashMap<>() {
                                                    {
                                                        put("id", authPolicyId);
                                                    }
                                                },
                                                AuthPolicy.class)
                                                .compose(newAuthPolicy ->
                                                        context.sql.getOne(
                                                                new HashMap<>() {
                                                                    {
                                                                        put("id", newAuthPolicy.getRelResourceId());
                                                                    }
                                                                },
                                                                Resource.class)
                                                                .compose(newAuthPolicyResource -> {
                                                                    var exchangeProcesses = new ArrayList<Future>();
                                                                    exchangeProcesses.addAll(Arrays.stream(originalAuthPolicy.getRelSubjectIds().split(","))
                                                                            .filter(id -> !id.trim().isBlank())
                                                                            .map(id -> Long.parseLong(id.trim()))
                                                                            .map(id ->
                                                                                    ExchangeProcessor.removePolicy(
                                                                                            ExchangeProcessor.AuthPolicyInfo.builder()
                                                                                                    .resourceUri(originalAuthPolicyResource.getUri())
                                                                                                    .actionKind(originalAuthPolicy.getActionKind())
                                                                                                    .subjectKind(originalAuthPolicy.getRelSubjectKind())
                                                                                                    .subjectOperator(originalAuthPolicy.getSubjectOperator())
                                                                                                    .subjectId(id + "")
                                                                                                    .build(), context))
                                                                            .collect(Collectors.toList()));
                                                                    exchangeProcesses.addAll(Arrays.stream(newAuthPolicy.getRelSubjectIds().split(","))
                                                                            .filter(id -> !id.trim().isBlank())
                                                                            .map(id -> Long.parseLong(id.trim()))
                                                                            .map(id ->
                                                                                    ExchangeProcessor.removePolicy(
                                                                                            ExchangeProcessor.AuthPolicyInfo.builder()
                                                                                                    .resourceUri(originalAuthPolicyResource.getUri())
                                                                                                    .actionKind(newAuthPolicy.getActionKind())
                                                                                                    .subjectKind(newAuthPolicy.getRelSubjectKind())
                                                                                                    .subjectOperator(newAuthPolicy.getSubjectOperator())
                                                                                                    .subjectId(id + "")
                                                                                                    .build(), context))
                                                                            .collect(Collectors.toList()));
                                                                    return CompositeFuture.all(exchangeProcesses);
                                                                })
                                                                .compose(r -> context.helper.success())))
                );
    }

    public static Future<AuthPolicyResp> getAuthPolicy(Long authPolicyId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", authPolicyId);
                        put("rel_app_id", relAppId);
                        put("rel_tenant_id", relTenantId);
                    }
                },
                AuthPolicy.class)
                .compose(authPolicy -> context.helper.success(authPolicy, AuthPolicyResp.class));
    }

    public static Future<Page<AuthPolicyResp>> pageAuthPolicies(String subjectKind, Long pageNumber, Long pageSize, Long relAppId, Long relTenantId, ProcessContext context) {
        var whereParameters = new HashMap<String, Object>() {
            {
                put("rel_app_id", relAppId);
                put("rel_tenant_id", relTenantId);
            }
        };
        if (subjectKind != null && !subjectKind.isBlank()) {
            whereParameters.put("%rel_subject_kind", "%" + subjectKind + "%");
        }
        return context.sql.page(
                whereParameters,
                pageNumber,
                pageSize,
                AuthPolicy.class)
                .compose(authPolicy -> context.helper.success(authPolicy, AuthPolicyResp.class));
    }

    public static Future<Void> deleteAuthPolicy(Long authPolicyId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", authPolicyId);
                    }
                },
                AuthPolicy.class)
                .compose(originalAuthPolicy ->
                        context.sql.getOne(
                                new HashMap<>() {
                                    {
                                        put("id", originalAuthPolicy.getRelResourceId());
                                    }
                                },
                                Resource.class)
                                .compose(originalAuthPolicyResource ->
                                        context.sql.softDelete(
                                                new HashMap<>() {
                                                    {
                                                        put("id", authPolicyId);
                                                        put("rel_app_id", relAppId);
                                                        put("rel_tenant_id", relTenantId);
                                                    }
                                                },
                                                AuthPolicy.class)
                                                .compose(resp ->
                                                        CompositeFuture.all(Arrays.stream(originalAuthPolicy.getRelSubjectIds().split(","))
                                                                .filter(id -> !id.trim().isBlank())
                                                                .map(id -> Long.parseLong(id.trim()))
                                                                .map(id ->
                                                                        ExchangeProcessor.removePolicy(
                                                                                ExchangeProcessor.AuthPolicyInfo.builder()
                                                                                        .resourceUri(originalAuthPolicyResource.getUri())
                                                                                        .actionKind(originalAuthPolicy.getActionKind())
                                                                                        .subjectKind(originalAuthPolicy.getRelSubjectKind())
                                                                                        .subjectOperator(originalAuthPolicy.getSubjectOperator())
                                                                                        .subjectId(id + "")
                                                                                        .build(), context))
                                                                .collect(Collectors.toList())))
                                                .compose(r -> context.helper.success())));
    }

}
