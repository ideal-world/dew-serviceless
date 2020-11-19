/*
 * Copyright 2020. the original author or authors.
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

package idealworld.dew.baas.iam.scene.appconsole.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.baas.common.enumeration.OptActionKind;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.auth.AuthPolicy;
import idealworld.dew.baas.iam.domain.auth.QAuthPolicy;
import idealworld.dew.baas.iam.domain.auth.QResource;
import idealworld.dew.baas.iam.enumeration.ExposeKind;
import idealworld.dew.baas.iam.scene.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.authpolicy.AuthPolicyModifyReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.authpolicy.AuthPolicyResp;
import idealworld.dew.baas.iam.scene.common.service.CommonFunctionService;
import idealworld.dew.baas.iam.scene.common.service.IAMBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用控制台下的权限策略服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class ACAuthPolicyService extends IAMBasicService {

    @Autowired
    private CommonFunctionService commonFunctionService;

    @Transactional
    public Resp<List<Long>> addAuthPolicy(AuthPolicyAddReq authPolicyAddReq, Long relAppId, Long relTenantId) {
        if (!authPolicyAddReq.getRelSubjectIds().endsWith(",")) {
            authPolicyAddReq.setRelSubjectIds(authPolicyAddReq.getRelSubjectIds() + ",");
        }
        var qResource = QResource.resource;
        if (sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.id.eq(authPolicyAddReq.getRelResourceId()))
                .where((qResource.relTenantId.eq(relTenantId).and(qResource.relAppId.eq(relAppId)))
                        .or(qResource.exposeKind.eq(ExposeKind.TENANT).and(qResource.relTenantId.eq(relTenantId)))
                        .or(qResource.exposeKind.eq(ExposeKind.GLOBAL)))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_AUTH_POLICY, "权限策略对应的资源不合法");
        }
        var subjectIds = Arrays.stream(authPolicyAddReq.getRelSubjectIds().split(","))
                .filter(id -> !id.trim().isBlank())
                .map(id -> Long.parseLong(id.trim()))
                .collect(Collectors.toList());
        Resp<Void> checkSubjectMembershipR = null;
        switch (authPolicyAddReq.getRelSubjectKind()) {
            case ROLE:
                checkSubjectMembershipR = commonFunctionService.checkRoleMembership(subjectIds, relAppId, relTenantId);
                break;
            case ACCOUNT:
                checkSubjectMembershipR = commonFunctionService.checkAccountMembership(subjectIds, relAppId, relTenantId);
                break;
            case APP:
                checkSubjectMembershipR = commonFunctionService.checkAppMembership(subjectIds, relTenantId);
                break;
            case TENANT:
                checkSubjectMembershipR = subjectIds.size() == 1 && subjectIds.get(0).longValue() == relTenantId
                        ? Resp.success(null)
                        : StandardResp.unAuthorized(BUSINESS_APP, "租户不合法");
                break;
            case GROUP_NODE:
                checkSubjectMembershipR = commonFunctionService.checkGroupNodeMembership(subjectIds, relAppId, relTenantId);
                break;
        }
        if (!checkSubjectMembershipR.ok()) {
            return Resp.error(checkSubjectMembershipR);
        }
        var qAuthPolicy = QAuthPolicy.authPolicy;
        var existsCheckQuery = sqlBuilder.select(qAuthPolicy.id)
                .from(qAuthPolicy)
                .where(qAuthPolicy.relSubjectKind.eq(authPolicyAddReq.getRelSubjectKind()))
                .where(qAuthPolicy.relSubjectIds.eq(authPolicyAddReq.getRelSubjectIds()))
                .where(qAuthPolicy.effectiveTime.eq(authPolicyAddReq.getEffectiveTime()))
                .where(qAuthPolicy.expiredTime.eq(authPolicyAddReq.getExpiredTime()))
                .where(qAuthPolicy.relResourceId.eq(authPolicyAddReq.getRelResourceId()));
        if (authPolicyAddReq.getActionKind() != null) {
            existsCheckQuery.where(qAuthPolicy.actionKind.eq(authPolicyAddReq.getActionKind()));
        }
        if (existsCheckQuery.fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_AUTH_POLICY, "权限策略已存在");
        }
        var authPolicy = $.bean.copyProperties(authPolicyAddReq, AuthPolicy.class);
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
            return saveEntities(createAuthPolicy, existsAuthPolicy, fetchAuthPolicy, modifyAuthPolicy, patchAuthPolicy, deleteAuthPolicy);
        } else {
            var saveR = saveEntity(authPolicy);
            if (!saveR.ok()) {
                return Resp.error(saveR);
            } else {
                return Resp.success(new ArrayList<>() {
                    {
                        add(saveR.getBody());
                    }
                });
            }
        }
    }

    @Transactional
    public Resp<Void> modifyAuthPolicy(Long authPolicyId, AuthPolicyModifyReq authPolicyModifyReq, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        if (authPolicyModifyReq.getRelResourceId() != null && sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.id.eq(authPolicyModifyReq.getRelResourceId()))
                .where((qResource.relTenantId.eq(relTenantId).and(qResource.relAppId.eq(relAppId)))
                        .or(qResource.exposeKind.eq(ExposeKind.TENANT).and(qResource.relTenantId.eq(relTenantId)))
                        .or(qResource.exposeKind.eq(ExposeKind.GLOBAL)))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_AUTH_POLICY, "权限策略对应的资源不合法");
        }
        if (authPolicyModifyReq.getRelSubjectKind() != null && authPolicyModifyReq.getRelSubjectIds() == null
                || authPolicyModifyReq.getRelSubjectKind() == null && authPolicyModifyReq.getRelSubjectIds() != null) {
            return StandardResp.badRequest(BUSINESS_AUTH_POLICY, "关联权限主体类型与关联权限主体Id必须同时存在");
        }
        var qAuthPolicy = QAuthPolicy.authPolicy;
        var authPolicyUpdate = sqlBuilder.update(qAuthPolicy)
                .where(qAuthPolicy.id.eq(authPolicyId))
                .where(qAuthPolicy.relTenantId.eq(relTenantId))
                .where(qAuthPolicy.relAppId.eq(relAppId));
        if (authPolicyModifyReq.getRelSubjectKind() != null) {
            authPolicyUpdate.set(qAuthPolicy.relSubjectKind, authPolicyModifyReq.getRelSubjectKind());
        }
        if (authPolicyModifyReq.getRelSubjectIds() != null) {
            if (!authPolicyModifyReq.getRelSubjectIds().endsWith(",")) {
                authPolicyModifyReq.setRelSubjectIds(authPolicyModifyReq.getRelSubjectIds() + ",");
            }
            var subjectIds = Arrays.stream(authPolicyModifyReq.getRelSubjectIds().split(","))
                    .filter(id -> !id.trim().isBlank())
                    .map(id -> Long.parseLong(id.trim()))
                    .collect(Collectors.toList());
            Resp<Void> checkSubjectMembershipR = null;
            switch (authPolicyModifyReq.getRelSubjectKind()) {
                case ROLE:
                    checkSubjectMembershipR = commonFunctionService.checkRoleMembership(subjectIds, relAppId, relTenantId);
                    break;
                case ACCOUNT:
                    checkSubjectMembershipR = commonFunctionService.checkAccountMembership(subjectIds, relAppId, relTenantId);
                    break;
                case APP:
                    checkSubjectMembershipR = commonFunctionService.checkAppMembership(subjectIds, relTenantId);
                    break;
                case TENANT:
                    checkSubjectMembershipR = subjectIds.size() == 1 && subjectIds.get(0).longValue() == relTenantId
                            ? Resp.success(null)
                            : StandardResp.unAuthorized(BUSINESS_APP, "租户不合法");
                    break;
                case GROUP_NODE:
                    checkSubjectMembershipR = commonFunctionService.checkGroupNodeMembership(subjectIds, relAppId, relTenantId);
                    break;
            }
            if (!checkSubjectMembershipR.ok()) {
                return Resp.error(checkSubjectMembershipR);
            }
            authPolicyUpdate.set(qAuthPolicy.relSubjectIds, authPolicyModifyReq.getRelSubjectIds());
        }
        if (authPolicyModifyReq.getSubjectOperator() != null) {
            authPolicyUpdate.set(qAuthPolicy.subjectOperator, authPolicyModifyReq.getSubjectOperator());
        }
        if (authPolicyModifyReq.getEffectiveTime() != null) {
            authPolicyUpdate.set(qAuthPolicy.effectiveTime, authPolicyModifyReq.getEffectiveTime());
        }
        if (authPolicyModifyReq.getExpiredTime() != null) {
            authPolicyUpdate.set(qAuthPolicy.expiredTime, authPolicyModifyReq.getExpiredTime());
        }
        if (authPolicyModifyReq.getRelResourceId() != null) {
            authPolicyUpdate.set(qAuthPolicy.relResourceId, authPolicyModifyReq.getRelResourceId());
        }
        if (authPolicyModifyReq.getActionKind() != null) {
            authPolicyUpdate.set(qAuthPolicy.actionKind, authPolicyModifyReq.getActionKind());
        }
        if (authPolicyModifyReq.getResultKind() != null) {
            authPolicyUpdate.set(qAuthPolicy.resultKind, authPolicyModifyReq.getResultKind());
        }
        if (authPolicyModifyReq.getExclusive() != null) {
            authPolicyUpdate.set(qAuthPolicy.exclusive, authPolicyModifyReq.getExclusive());
        }
        return updateEntity(authPolicyUpdate);
    }

    public Resp<AuthPolicyResp> getAuthPolicy(Long authPolicyId, Long relAppId, Long relTenantId) {
        var qAuthPolicy = QAuthPolicy.authPolicy;
        return getDTO(sqlBuilder.select(Projections.bean(AuthPolicyResp.class,
                qAuthPolicy.relSubjectKind,
                qAuthPolicy.relSubjectIds,
                qAuthPolicy.subjectOperator,
                qAuthPolicy.effectiveTime,
                qAuthPolicy.expiredTime,
                qAuthPolicy.relResourceId,
                qAuthPolicy.actionKind,
                qAuthPolicy.resultKind,
                qAuthPolicy.exclusive,
                qAuthPolicy.relAppId,
                qAuthPolicy.relTenantId))
                .from(qAuthPolicy)
                .where(qAuthPolicy.id.eq(authPolicyId))
                .where(qAuthPolicy.relTenantId.eq(relTenantId))
                .where(qAuthPolicy.relAppId.eq(relAppId)));
    }

    public Resp<Page<AuthPolicyResp>> pageAuthPolicies(Long pageNumber, Integer pageSize, Long relAppId, Long relTenantId) {
        var qAuthPolicy = QAuthPolicy.authPolicy;
        return pageDTOs(sqlBuilder.select(Projections.bean(AuthPolicyResp.class,
                qAuthPolicy.relSubjectKind,
                qAuthPolicy.relSubjectIds,
                qAuthPolicy.subjectOperator,
                qAuthPolicy.effectiveTime,
                qAuthPolicy.expiredTime,
                qAuthPolicy.relResourceId,
                qAuthPolicy.actionKind,
                qAuthPolicy.resultKind,
                qAuthPolicy.exclusive,
                qAuthPolicy.relAppId,
                qAuthPolicy.relTenantId))
                .from(qAuthPolicy)
                .where(qAuthPolicy.relTenantId.eq(relTenantId))
                .where(qAuthPolicy.relAppId.eq(relAppId)), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteAuthPolicy(Long authPolicyId, Long relAppId, Long relTenantId) {
        var qAuthPolicy = QAuthPolicy.authPolicy;
        return softDelEntity(sqlBuilder
                .selectFrom(qAuthPolicy)
                .where(qAuthPolicy.id.eq(authPolicyId))
                .where(qAuthPolicy.relTenantId.eq(relTenantId))
                .where(qAuthPolicy.relAppId.eq(relAppId)));
    }

}
