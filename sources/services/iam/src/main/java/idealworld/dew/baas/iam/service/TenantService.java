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
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.CommonStatus;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.ident.*;
import idealworld.dew.baas.iam.dto.tenant.*;
import idealworld.dew.baas.iam.enumeration.AccountIdentKind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 租户服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class TenantService extends IAMBasicService {

    private static final Map<String, Pattern> VALID_RULES = new ConcurrentHashMap<>();

    private static final String BUSINESS_TENANT = "TENANT";
    private static final String BUSINESS_TENANT_IDENT = "TENANT_IDENT";
    private static final String BUSINESS_TENANT_CERT = "TENANT_CERT";

    @Transactional
    public Resp<Long> addTenant(TenantAddReq tenantAddReq) {
        var tenant = $.bean.copyProperties(tenantAddReq, Tenant.class);
        tenant.setStatus(CommonStatus.ENABLED);
        return saveEntity(tenant);
    }

    @Transactional
    public Resp<Void> modifyTenant(Long tenantId, TenantModifyReq tenantModifyReq, Long relTenantId) {
        if (tenantId.longValue() != relTenantId.longValue()) {
            return StandardResp.unAuthorized(BUSINESS_TENANT, "租户Id不合法");
        }
        var qTenant = QTenant.tenant;
        var tenantUpdate = sqlBuilder.update(qTenant)
                .where(qTenant.id.eq(tenantId));
        if (tenantModifyReq.getName() != null) {
            tenantUpdate.set(qTenant.name, tenantModifyReq.getName());
        }
        if (tenantModifyReq.getIcon() != null) {
            tenantUpdate.set(qTenant.icon, tenantModifyReq.getIcon());
        }
        if (tenantModifyReq.getAllowAccountRegister() != null) {
            tenantUpdate.set(qTenant.allowAccountRegister, tenantModifyReq.getAllowAccountRegister());
        }
        if (tenantModifyReq.getParameters() != null) {
            tenantUpdate.set(qTenant.parameters, tenantModifyReq.getParameters());
        }
        if (tenantModifyReq.getStatus() != null) {
            tenantUpdate.set(qTenant.status, tenantModifyReq.getStatus());
        }
        return updateEntity(tenantUpdate);
    }

    public Resp<TenantResp> getTenant(Long tenantId, Long relTenantId) {
        if (tenantId.longValue() != relTenantId.longValue()) {
            return StandardResp.unAuthorized(BUSINESS_TENANT, "租户Id不合法");
        }
        var qTenant = QTenant.tenant;
        return getDTO(sqlBuilder.select(Projections.bean(TenantResp.class,
                qTenant.id,
                qTenant.name,
                qTenant.icon,
                qTenant.allowAccountRegister,
                qTenant.parameters,
                qTenant.status))
                .from(qTenant)
                .where(qTenant.id.eq(tenantId)));
    }

    public Resp<Page<TenantResp>> pageTenants(Long pageNumber, Integer pageSize, Long relTenantId) {
        var qTenant = QTenant.tenant;
        return pageDTOs(sqlBuilder.select(Projections.bean(TenantResp.class,
                qTenant.id,
                qTenant.name,
                qTenant.icon,
                qTenant.allowAccountRegister,
                qTenant.parameters,
                qTenant.status))
                .from(qTenant), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteTenant(Long tenantId, Long relTenantId) {
        if (tenantId.longValue() != relTenantId.longValue()) {
            return StandardResp.unAuthorized(BUSINESS_TENANT, "租户Id不合法");
        }
        var qTenant = QTenant.tenant;
        return softDelEntity(sqlBuilder
                .selectFrom(qTenant)
                .where(qTenant.id.eq(tenantId)));
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addTenantIdent(TenantIdentAddReq tenantIdentAddReq, Long relTenantId) {
        var tenantIdent = $.bean.copyProperties(tenantIdentAddReq, TenantIdent.class);
        tenantIdent.setRelTenantId(relTenantId);
        return saveEntity(tenantIdent);
    }

    @Transactional
    public Resp<Void> modifyTenantIdent(Long tenantIdentId, TenantIdentModifyReq tenantIdentModifyReq, Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        var tenantIdentUpdate = sqlBuilder.update(qTenantIdent)
                .where(qTenantIdent.id.eq(tenantIdentId))
                .where(qTenantIdent.relTenantId.eq(relTenantId));
        if (tenantIdentModifyReq.getKind() != null) {
            tenantIdentUpdate.set(qTenantIdent.kind, tenantIdentModifyReq.getKind());
        }
        if (tenantIdentModifyReq.getValidAKRule() != null) {
            tenantIdentUpdate.set(qTenantIdent.validAKRule, tenantIdentModifyReq.getValidAKRule());
        }
        if (tenantIdentModifyReq.getValidAKRuleNote() != null) {
            tenantIdentUpdate.set(qTenantIdent.validAKRuleNote, tenantIdentModifyReq.getValidAKRuleNote());
        }
        if (tenantIdentModifyReq.getValidSKRule() != null) {
            tenantIdentUpdate.set(qTenantIdent.validSKRule, tenantIdentModifyReq.getValidSKRule());
        }
        if (tenantIdentModifyReq.getValidSKRuleNote() != null) {
            tenantIdentUpdate.set(qTenantIdent.validSKRuleNote, tenantIdentModifyReq.getValidSKRuleNote());
        }
        if (tenantIdentModifyReq.getValidTimeSec() != null) {
            tenantIdentUpdate.set(qTenantIdent.validTimeSec, tenantIdentModifyReq.getValidTimeSec());
        }
        return updateEntity(tenantIdentUpdate);
    }

    public Resp<TenantIdentResp> getTenantIdent(Long tenantIdentId, Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        return getDTO(sqlBuilder.select(Projections.bean(TenantIdentResp.class,
                qTenantIdent.id,
                qTenantIdent.kind,
                qTenantIdent.validAKRule,
                qTenantIdent.validAKRuleNote,
                qTenantIdent.validSKRule,
                qTenantIdent.validSKRuleNote,
                qTenantIdent.validTimeSec))
                .from(qTenantIdent)
                .where(qTenantIdent.id.eq(tenantIdentId))
                .where(qTenantIdent.relTenantId.eq(relTenantId)));
    }

    public Resp<Page<TenantIdentResp>> pageTenantIdents(Long pageNumber, Integer pageSize, Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        return pageDTOs(sqlBuilder.select(Projections.bean(TenantIdentResp.class,
                qTenantIdent.id,
                qTenantIdent.kind,
                qTenantIdent.validAKRule,
                qTenantIdent.validAKRuleNote,
                qTenantIdent.validSKRule,
                qTenantIdent.validSKRuleNote,
                qTenantIdent.validTimeSec))
                .from(qTenantIdent)
                .where(qTenantIdent.relTenantId.eq(relTenantId)), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteTenantIdent(Long tenantIdentId, Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        return softDelEntity(sqlBuilder
                .selectFrom(qTenantIdent)
                .where(qTenantIdent.id.eq(tenantIdentId))
                .where(qTenantIdent.relTenantId.eq(relTenantId)));
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addTenantCert(TenantCertAddReq tenantCertAddReq, Long relTenantId) {
        var tenantCert = $.bean.copyProperties(tenantCertAddReq, TenantCert.class);
        tenantCert.setRelTenantId(relTenantId);
        return saveEntity(tenantCert);
    }

    @Transactional
    public Resp<Void> modifyTenantCert(Long tenantCertId, TenantCertModifyReq tenantCertModifyReq, Long relTenantId) {
        var qTenantCert = QTenantCert.tenantCert;
        var tenantCertUpdate = sqlBuilder.update(qTenantCert)
                .where(qTenantCert.id.eq(tenantCertId))
                .where(qTenantCert.relTenantId.eq(relTenantId));
        if (tenantCertModifyReq.getCategory() != null) {
            tenantCertUpdate.set(qTenantCert.category, tenantCertModifyReq.getCategory());
        }
        if (tenantCertModifyReq.getVersion() != null) {
            tenantCertUpdate.set(qTenantCert.version, tenantCertModifyReq.getVersion());
        }
        return updateEntity(tenantCertUpdate);
    }

    public Resp<TenantCertResp> getTenantCert(Long tenantCertId, Long relTenantId) {
        var qTenantCert = QTenantCert.tenantCert;
        return getDTO(sqlBuilder.select(Projections.bean(TenantCertResp.class,
                qTenantCert.id,
                qTenantCert.category,
                qTenantCert.version))
                .from(qTenantCert)
                .where(qTenantCert.id.eq(tenantCertId))
                .where(qTenantCert.relTenantId.eq(relTenantId)));
    }

    public Resp<Page<TenantCertResp>> pageTenantCerts(Long pageNumber, Integer pageSize, Long relTenantId) {
        var qTenantCert = QTenantCert.tenantCert;
        return pageDTOs(sqlBuilder.select(Projections.bean(TenantCertResp.class,
                qTenantCert.id,
                qTenantCert.category,
                qTenantCert.version))
                .from(qTenantCert)
                .where(qTenantCert.relTenantId.eq(relTenantId)), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteTenantCert(Long tenantCertId, Long relTenantId) {
        var qTenantCert = QTenantCert.tenantCert;
        return softDelEntity(sqlBuilder
                .selectFrom(qTenantCert)
                .where(qTenantCert.id.eq(tenantCertId))
                .where(qTenantCert.relTenantId.eq(relTenantId)));
    }

    // --------------------------------------------------------------------

    Resp<Date> validRuleAndGetValidEndTime(AccountIdentKind kind, @Nullable String ak, @Nullable String sk, Long relTenantId) {
        if (relTenantId.equals(Constant.OBJECT_UNDEFINED)) {
            // 表示租户管理员注册时临时分配的虚拟租户号
            return StandardResp.success(Constant.MAX_TIME);
        }
        var qTenantIdent = QTenantIdent.tenantIdent;
        var tenantIdent = sqlBuilder
                .select(qTenantIdent.validAKRule,
                        qTenantIdent.validSKRule,
                        qTenantIdent.validTimeSec)
                .from(qTenantIdent)
                .where(qTenantIdent.kind.eq(kind))
                .where(qTenantIdent.relTenantId.eq(relTenantId))
                .fetchOne();
        if (tenantIdent == null) {
            return StandardResp.badRequest(BUSINESS_TENANT_IDENT, "认证不存在或已禁用");
        }
        var validAkRule = tenantIdent.get(0, String.class);
        var validSkRule = tenantIdent.get(1, String.class);
        var validTimeSec = tenantIdent.get(2, Long.class);
        if (ak != null && !StringUtils.isEmpty(validAkRule)) {
            if (!VALID_RULES.containsKey(validAkRule)) {
                VALID_RULES.put(validAkRule, Pattern.compile(validAkRule));
            }
            if (!VALID_RULES.get(validAkRule).matcher(ak).matches()) {
                return StandardResp.badRequest(BUSINESS_TENANT_CERT, "认证名规则不合法");
            }
        }
        if (sk != null && !StringUtils.isEmpty(validSkRule)) {
            if (!VALID_RULES.containsKey(validSkRule)) {
                VALID_RULES.put(validSkRule, Pattern.compile(validSkRule));
            }
            if (!VALID_RULES.get(validSkRule).matcher(sk).matches()) {
                return StandardResp.badRequest(BUSINESS_TENANT_CERT, "认证密钥规则不合法");
            }
        }
        return StandardResp.success(validTimeSec == null || validTimeSec.equals(Constant.OBJECT_UNDEFINED)
                ? Constant.MAX_TIME
                : new Date(System.currentTimeMillis() + validTimeSec * 1000));
    }

}
