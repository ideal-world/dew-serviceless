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
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.serviceless.iam.domain.auth.AuthPolicy;
import idealworld.dew.serviceless.iam.domain.auth.Resource;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy.AuthPolicyModifyReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy.AuthPolicyResp;
import idealworld.dew.serviceless.iam.process.IAMBasicProcessor;
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
public class ACAuthPolicyProcessor {

    static {
        // 添加当前应用的权限策略
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/app/authpolicy", addAuthPolicy());
        // 修改当前应用的某个权限策略
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/app/authpolicy/{authPolicyId}", modifyAuthPolicy());
        // getAuthPolicy
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/authpolicy/{authPolicyId}", getAuthPolicy());
        // 获取当前应用的权限策略列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/authpolicy", pageAuthPolicies());
        // 删除当前应用的某个权限策略
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/app/authpolicy/{authPolicyId}", deleteAuthPolicy());
    }

    public static ProcessFun<Void> addAuthPolicy() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var authPolicyAddReq = context.req.body(AuthPolicyAddReq.class);
            if (!authPolicyAddReq.getRelSubjectIds().endsWith(",")) {
                authPolicyAddReq.setRelSubjectIds(authPolicyAddReq.getRelSubjectIds() + ",");
            }
            var subjectIds = new ArrayList<Long>();
            final String[] resourceUrl = new String[1];
            return context.helper.notExistToError(
                    context.fun.sql.getOne(
                            String.format("SELECT uri FROM %s" +
                                            "  WHERE id = #{id} AND " +
                                            "  (( rel_tenant_id = #{rel_tenant_id} AND rel_app_id = #{rel_app_id} )" +
                                            "     OR ( expose_kind = #{expose_kind_tenant} AND rel_tenant_id = #{rel_tenant_id} )" +
                                            "     OR ( expose_kind = #{expose_kind_global} ))",
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
                                context.fun.sql.exists(
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
                            return context.fun.sql.save(new ArrayList<>() {
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
                            return context.fun.sql.save(authPolicy)
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
        };
    }

    public static ProcessFun<Void> modifyAuthPolicy() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var authPolicyId = Long.parseLong(context.req.params.get("authPolicyId"));
            var authPolicyModifyReq = context.req.body(AuthPolicyModifyReq.class);
            if (authPolicyModifyReq.getRelSubjectKind() != null && authPolicyModifyReq.getRelSubjectIds() == null
                    || authPolicyModifyReq.getRelSubjectKind() == null && authPolicyModifyReq.getRelSubjectIds() != null) {
                throw new BadRequestException("关联权限主体类型与关联权限主体Id必须同时存在");
            }
            return context.helper.notExistToError(
                    context.fun.sql.exists(
                            String.format("SELECT id FROM %s" +
                                            "  WHERE id = #{id} AND " +
                                            "  (( rel_tenant_id = #{rel_tenant_id} AND rel_app_id = #{rel_app_id} )" +
                                            "     OR ( expose_kind = #{expose_kind_tenant} AND rel_tenant_id = #{rel_tenant_id} )" +
                                            "     OR ( expose_kind = #{expose_kind_global} ))",
                                    new Resource().tableName()),
                            new HashMap<>() {
                                {
                                    put("id", authPolicyModifyReq.getRelResourceId());
                                    put("expose_kind_tenant", ExposeKind.TENANT);
                                    put("expose_kind_global", ExposeKind.GLOBAL);
                                    put("rel_app_id", relAppId);
                                    put("rel_tenant_id", relTenantId);
                                }
                            }), () -> new UnAuthorizedException("权限策略对应的资源不合法"))
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
                            context.fun.sql.getOne(authPolicyId, AuthPolicy.class)
                    )
                    .compose(originalAuthPolicy ->
                            context.fun.sql.update(
                                    new HashMap<>() {
                                        {
                                            put("id", authPolicyId);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    context.helper.convert(authPolicyModifyReq, AuthPolicy.class))
                                    .compose(resp ->
                                            context.fun.sql.getOne(
                                                    new HashMap<>() {
                                                        {
                                                            put("id", originalAuthPolicy.getRelResourceId());
                                                        }
                                                    },
                                                    Resource.class)
                                    )
                                    .compose(originalAuthPolicyResource ->
                                            context.fun.sql.getOne(
                                                    new HashMap<>() {
                                                        {
                                                            put("id", authPolicyId);
                                                        }
                                                    },
                                                    AuthPolicy.class)
                                                    .compose(newAuthPolicy ->
                                                            context.fun.sql.getOne(
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
        };
    }

    public static ProcessFun<AuthPolicyResp> getAuthPolicy() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var authPolicyId = Long.parseLong(context.req.params.get("authPolicyId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", authPolicyId);
                            put("rel_app_id", relAppId);
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    AuthPolicy.class)
                    .compose(authPolicy -> context.helper.success(authPolicy, AuthPolicyResp.class));
        };
    }

    public static ProcessFun<Page<AuthPolicyResp>> pageAuthPolicies() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var subjectKind = context.req.params.getOrDefault("subjectKind", null);
            var whereParameters = new HashMap<String, Object>() {
                {
                    put("rel_app_id", relAppId);
                    put("rel_tenant_id", relTenantId);
                }
            };
            if (subjectKind != null && !subjectKind.isBlank()) {
                whereParameters.put("%rel_subject_kind", "%" + subjectKind + "%");
            }
            return context.fun.sql.page(
                    whereParameters,
                    context.req.pageNumber(),
                    context.req.pageSize(),
                    AuthPolicy.class)
                    .compose(authPolicy -> context.helper.success(authPolicy, AuthPolicyResp.class));

        };
    }

    public static ProcessFun<Void> deleteAuthPolicy() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var authPolicyId = Long.parseLong(context.req.params.get("authPolicyId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", authPolicyId);
                        }
                    },
                    AuthPolicy.class)
                    .compose(originalAuthPolicy ->
                            context.fun.sql.getOne(
                                    new HashMap<>() {
                                        {
                                            put("id", originalAuthPolicy.getRelResourceId());
                                        }
                                    },
                                    Resource.class)
                                    .compose(originalAuthPolicyResource ->
                                            context.fun.sql.softDelete(
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
        };
    }

}
