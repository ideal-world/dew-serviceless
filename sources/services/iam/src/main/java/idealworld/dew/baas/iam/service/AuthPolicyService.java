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

package idealworld.dew.baas.iam.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.auth.AuthPolicy;
import idealworld.dew.baas.iam.domain.auth.QAuthPolicy;
import idealworld.dew.baas.iam.domain.auth.QResource;
import idealworld.dew.baas.iam.dto.authpolicy.AuthPolicyAddOrModifyReq;
import idealworld.dew.baas.iam.dto.authpolicy.AuthPolicyResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auth policy service.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class AuthPolicyService extends IAMBasicService {

    private static final String BUSINESS_AUTH_POLICY = "AUTH_POLICY";

    @Autowired
    private AuthService authService;

    @Transactional
    public Resp<Long> addAuthPolicy(AuthPolicyAddOrModifyReq authPolicyAddReq, Long relAppId, Long relTenantId) {
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
    public Resp<Long> modifyAuthPolicy(Long authPolicyId, AuthPolicyAddOrModifyReq authPolicyModifyReq, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        if (sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.id.eq(authPolicyModifyReq.getRelResourceId()))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_AUTH_POLICY, "权限策略对应的资源不合法");
        }
        var qAuthPolicy = QAuthPolicy.authPolicy;
        if (sqlBuilder.select(qAuthPolicy.id)
                .from(qAuthPolicy)
                .where(qAuthPolicy.relSubjectKind.eq(authPolicyModifyReq.getRelSubjectKind()))
                .where(qAuthPolicy.relSubjectIds.eq(authPolicyModifyReq.getRelSubjectIds()))
                .where(qAuthPolicy.effectiveTime.eq(authPolicyModifyReq.getEffectiveTime()))
                .where(qAuthPolicy.expiredTime.eq(authPolicyModifyReq.getExpiredTime()))
                .where(qAuthPolicy.relResourceId.eq(authPolicyModifyReq.getRelResourceId()))
                .where(qAuthPolicy.actionKind.eq(authPolicyModifyReq.getActionKind()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_AUTH_POLICY, "权限策略已存在");
        }
        var authPolicy = $.bean.copyProperties(authPolicyModifyReq, AuthPolicy.class);
        authPolicy.setId(authPolicyId);
        authPolicy.setRelTenantId(relTenantId);
        authPolicy.setRelAppId(relAppId);
        return updateEntity(authPolicy);
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
                qAuthPolicy.exposeKind,
                qAuthPolicy.relAppId,
                qAuthPolicy.relTenantId))
                .from(qAuthPolicy)
                .where(qAuthPolicy.id.eq(authPolicyId))
                .where(qAuthPolicy.relTenantId.eq(relTenantId))
                .where(qAuthPolicy.relAppId.eq(relAppId)));
    }

    public Resp<Page<AuthPolicyResp>> pageAuthPolicy(Long pageNumber, Integer pageSize, Long relAppId, Long relTenantId) {
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
                qAuthPolicy.exposeKind,
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
