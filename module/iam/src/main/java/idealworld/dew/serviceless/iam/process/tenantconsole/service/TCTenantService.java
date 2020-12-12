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

package idealworld.dew.serviceless.iam.process.tenantconsole.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.serviceless.common.enumeration.CommonStatus;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.iam.domain.ident.*;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.scene.common.service.IAMBasicService;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.tenant.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 租户控制台下的租户服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class TCTenantService extends IAMBasicService {

    @Autowired
    private ExchangeProcessor exchangeProcessor;

    @Transactional
    public Resp<Void> modifyTenant(TenantModifyReq tenantModifyReq, Long relTenantId) {
        var qTenant = QTenant.tenant;
        var tenantUpdate = sqlBuilder.update(qTenant)
                .where(qTenant.id.eq(relTenantId));
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
        var updateR = updateEntity(tenantUpdate);
        if (!updateR.ok()) {
            return updateR;
        }
        if (tenantModifyReq.getStatus() != null) {
            if (tenantModifyReq.getStatus() == CommonStatus.ENABLED) {
                exchangeProcessor.enableTenant(relTenantId);
            } else {
                exchangeProcessor.disableTenant(relTenantId);
            }
        }
        return updateR;
    }

    public Resp<TenantResp> getTenant(Long relTenantId) {
        var qTenant = QTenant.tenant;
        return getDTO(sqlBuilder.select(Projections.bean(TenantResp.class,
                qTenant.id,
                qTenant.name,
                qTenant.icon,
                qTenant.allowAccountRegister,
                qTenant.parameters,
                qTenant.status))
                .from(qTenant)
                .where(qTenant.id.eq(relTenantId)));
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addTenantIdent(TenantIdentAddReq tenantIdentAddReq, Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        if (sqlBuilder.select(qTenantIdent.id)
                .from(qTenantIdent)
                .where(qTenantIdent.kind.eq(tenantIdentAddReq.getKind()))
                .where(qTenantIdent.relTenantId.eq(relTenantId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_TENANT_IDENT, "租户认证类型已存在");
        }
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
        if (tenantIdentModifyReq.getValidAkRule() != null) {
            tenantIdentUpdate.set(qTenantIdent.validAkRule, tenantIdentModifyReq.getValidAKRule());
        }
        if (tenantIdentModifyReq.getValidAkRuleNote() != null) {
            tenantIdentUpdate.set(qTenantIdent.validAKRuleNote, tenantIdentModifyReq.getValidAKRuleNote());
        }
        if (tenantIdentModifyReq.getValidSkRule() != null) {
            tenantIdentUpdate.set(qTenantIdent.validSKRule, tenantIdentModifyReq.getValidSKRule());
        }
        if (tenantIdentModifyReq.getValidSkRuleNote() != null) {
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
        var qTenantCert = QTenantCert.tenantCert;
        tenantCertAddReq.setCategory(tenantCertAddReq.getCategory().toLowerCase());
        if (sqlBuilder.select(qTenantCert.id)
                .from(qTenantCert)
                .where(qTenantCert.category.eq(tenantCertAddReq.getCategory()))
                .where(qTenantCert.relTenantId.eq(relTenantId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_TENANT_CERT, "租户凭证类型已存在");
        }
        var tenantCert = $.bean.copyProperties(tenantCertAddReq, TenantCert.class);
        tenantCert.setRelTenantId(relTenantId);
        return saveEntity(tenantCert);
    }

    @Transactional
    public Resp<Void> modifyTenantCert(Long tenantCertId, TenantCertModifyReq tenantCertModifyReq, Long relTenantId) {
        var qTenantCert = QTenantCert.tenantCert;
        if (tenantCertModifyReq.getCategory() != null) {
            tenantCertModifyReq.setCategory(tenantCertModifyReq.getCategory().toLowerCase());
        }
        if (tenantCertModifyReq.getCategory() != null && sqlBuilder.select(qTenantCert.id)
                .from(qTenantCert)
                .where(qTenantCert.category.eq(tenantCertModifyReq.getCategory()))
                .where(qTenantCert.relTenantId.eq(relTenantId))
                .where(qTenantCert.id.ne(tenantCertId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_TENANT_CERT, "租户凭证类型已存在");
        }
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

}
