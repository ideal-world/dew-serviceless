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
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.auth.AuthPolicy;
import idealworld.dew.baas.iam.domain.auth.QAuthPolicy;
import idealworld.dew.baas.iam.domain.auth.QResource;
import idealworld.dew.baas.iam.scene.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.authpolicy.AuthPolicyModifyReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.authpolicy.AuthPolicyResp;
import idealworld.dew.baas.iam.scene.common.service.IAMBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 应用控制台下的权限策略服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class ACAuthPolicyService extends IAMBasicService {

    @Transactional
    public Resp<Long> addAuthPolicy(AuthPolicyAddReq authPolicyAddReq, Long relAppId, Long relTenantId) {
        if (!authPolicyAddReq.getRelSubjectIds().endsWith(",")) {
            authPolicyAddReq.setRelSubjectIds(authPolicyAddReq.getRelSubjectIds() + ",");
        }
        var qResource = QResource.resource;
        if (sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.id.eq(authPolicyAddReq.getRelResourceId()))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_AUTH_POLICY, "权限策略对应的资源不合法");
        }
        var qAuthPolicy = QAuthPolicy.authPolicy;
        if (sqlBuilder.select(qAuthPolicy.id)
                .from(qAuthPolicy)
                .where(qAuthPolicy.relSubjectKind.eq(authPolicyAddReq.getRelSubjectKind()))
                .where(qAuthPolicy.relSubjectIds.eq(authPolicyAddReq.getRelSubjectIds()))
                .where(qAuthPolicy.effectiveTime.eq(authPolicyAddReq.getEffectiveTime()))
                .where(qAuthPolicy.expiredTime.eq(authPolicyAddReq.getExpiredTime()))
                .where(qAuthPolicy.relResourceId.eq(authPolicyAddReq.getRelResourceId()))
                .where(qAuthPolicy.actionKind.eq(authPolicyAddReq.getActionKind()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_AUTH_POLICY, "权限策略已存在");
        }
        var authPolicy = $.bean.copyProperties(authPolicyAddReq, AuthPolicy.class);
        authPolicy.setRelTenantId(relTenantId);
        authPolicy.setRelAppId(relAppId);
        return saveEntity(authPolicy);
    }

    @Transactional
    public Resp<Void> modifyAuthPolicy(Long authPolicyId, AuthPolicyModifyReq authPolicyModifyReq, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        if (authPolicyModifyReq.getRelResourceId() != null && sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.id.eq(authPolicyModifyReq.getRelResourceId()))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_AUTH_POLICY, "权限策略对应的资源不合法");
        }
        var qAuthPolicy = QAuthPolicy.authPolicy;
        var authPolicyUpdate = sqlBuilder.update(qResource)
                .where(qResource.id.eq(authPolicyId))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId));
        if (authPolicyModifyReq.getRelSubjectKind() != null) {
            authPolicyUpdate.set(qAuthPolicy.relSubjectKind, authPolicyModifyReq.getRelSubjectKind());
        }
        if (authPolicyModifyReq.getRelSubjectIds() != null) {
            if (!authPolicyModifyReq.getRelSubjectIds().endsWith(",")) {
                authPolicyModifyReq.setRelSubjectIds(authPolicyModifyReq.getRelSubjectIds() + ",");
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
        if (authPolicyModifyReq.getRelSubjectAppId() != null) {
            authPolicyUpdate.set(qAuthPolicy.relSubjectAppId, authPolicyModifyReq.getRelSubjectAppId());
        }
        if (authPolicyModifyReq.getRelSubjectTenantId() != null) {
            authPolicyUpdate.set(qAuthPolicy.relSubjectTenantId, authPolicyModifyReq.getRelSubjectTenantId());
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
                qAuthPolicy.relSubjectAppId,
                qAuthPolicy.relSubjectTenantId,
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
                qAuthPolicy.relSubjectAppId,
                qAuthPolicy.relSubjectTenantId,
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
