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

import idealworld.dew.baas.common.dto.IdentOptInfo;
import idealworld.dew.baas.iam.IAMConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private static final String BUSINESS_TENANT_CERT = "TENANT_CERT";

    @Autowired
    private IAMConfig iamConfig;
    /*@Autowired
    private AccountService accountService;
    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private PositionService positionService;
    @Autowired
    private PostService postService;
    @Autowired
    private InterceptService interceptService;*/

    /**
     * Register tenant.
     *
     * @param registerTenantReq the register tenant req
     * @return the resp
     *//*
    @Transactional
    public Resp<IdentOptInfo> registerTenant(RegisterTenantReq registerTenantReq) {
        log.info("Register Tenant : {}", $.json.toJsonString(registerTenantReq));
        if (!iamConfig.isAllowTenantRegister()) {
            return StandardResp.locked(BUSINESS_TENANT, "The current configuration does not allow tenants to self-register");
        }
        var tenantAdminPostId = postService.getTenantAdminPostId();
        var addAccountR = accountService.addAccountExt(AddAccountReq.builder()
                .name(registerTenantReq.getAccountName())
                .identReq(AddAccountIdentReq.builder()
                        .kind(registerTenantReq.getIdentKind())
                        .ak(registerTenantReq.getAk())
                        .sk(registerTenantReq.getSk())
                        .build())
                .postReq(AddAccountPostReq.builder()
                        .relPostId(tenantAdminPostId)
                        .build())
                .build(), Constant.OBJECT_UNDEFINED);
        if (!addAccountR.ok()) {
            return StandardResp.error(addAccountR);
        }
        var openId = accountService.getOpenId(addAccountR.getBody()).getBody();
        var tenant = Tenant.builder()
                .name(registerTenantReq.getTenantName())
                .icon("")
                .parameters("{}")
                .allowAccountRegister(registerTenantReq.getAllowAccountRegister() != null ?
                        registerTenantReq.getAllowAccountRegister() : false)
                .globalAccount(registerTenantReq.getGlobalAccount() != null ?
                        registerTenantReq.getGlobalAccount() : true)
                .allowCrossTenant(registerTenantReq.getAllowCrossTenant() != null ?
                        registerTenantReq.getAllowCrossTenant() : false)
                .status(CommonStatus.ENABLED)
                .createUser(openId)
                .updateUser(openId)
                .build();
        saveEntity(tenant);
        interceptService.changeTenantStatus(tenant.getId(), CommonStatus.ENABLED);
        accountService.updateAccountTenant(addAccountR.getBody(), tenant.getId());
        addTenantIdent(AddTenantIdentReq.builder()
                .kind(registerTenantReq.getIdentKind())
                .build(), tenant.getId());
        // 自动登录
        return accountService.login(
                LoginReq.builder()
                        .identKind(registerTenantReq.getIdentKind())
                        .ak(registerTenantReq.getAk())
                        .sk(registerTenantReq.getSk())
                        .build(), tenant.getId());
    }

    *//**
     * Gets tenant info.
     *
     * @param tenantId the tenant id
     * @return the tenant info
     *//*
    public Resp<TenantInfoResp> getTenantInfo(Long tenantId) {
        var qTenant = QTenant.tenant;
        var qAccountCreateUser = new QAccount("createUser");
        var qAccountUpdateUser = new QAccount("updateUser");
        var tenantQuery = sqlBuilder
                .select(Projections.bean(
                        TenantInfoResp.class,
                        qTenant.id,
                        qTenant.name,
                        qTenant.icon,
                        qTenant.allowAccountRegister,
                        qTenant.globalAccount,
                        qTenant.allowCrossTenant,
                        qTenant.parameters,
                        qTenant.status,
                        qTenant.createTime,
                        qTenant.updateTime,
                        qAccountCreateUser.name.as("createUserName"),
                        qAccountUpdateUser.name.as("updateUserName")))
                .from(qTenant)
                .leftJoin(qAccountCreateUser).on(qTenant.createUser.eq(qAccountCreateUser.openId))
                .leftJoin(qAccountUpdateUser).on(qTenant.updateUser.eq(qAccountUpdateUser.openId))
                .where(qTenant.id.eq(tenantId));
        return getDTO(tenantQuery);
    }

    *//**
     * Modify tenant.
     *
     * @param modifyTenantReq the modify tenant req
     * @param tenantId        the tenant id
     * @return the resp
     *//*
    @Transactional
    public Resp<Void> modifyTenant(ModifyTenantReq modifyTenantReq, Long tenantId) {
        var qTenant = QTenant.tenant;
        var updateClause = sqlBuilder.update(qTenant)
                .where(qTenant.id.eq(tenantId))
                .set(qTenant.name, modifyTenantReq.getName());
        if (modifyTenantReq.getIcon() != null) {
            updateClause.set(qTenant.icon, modifyTenantReq.getIcon());
        }
        if (modifyTenantReq.getAllowAccountRegister() != null) {
            updateClause.set(qTenant.allowAccountRegister, modifyTenantReq.getAllowAccountRegister());
        }
        if (modifyTenantReq.getGlobalAccount() != null) {
            updateClause.set(qTenant.globalAccount, modifyTenantReq.getGlobalAccount());
        }
        if (modifyTenantReq.getAllowCrossTenant() != null) {
            updateClause.set(qTenant.allowCrossTenant, modifyTenantReq.getAllowCrossTenant());
        }
        if (modifyTenantReq.getParameters() != null) {
            updateClause.set(qTenant.parameters, modifyTenantReq.getParameters());
        }
        if (modifyTenantReq.getStatus() != null) {
            updateClause.set(qTenant.status, modifyTenantReq.getStatus());
            interceptService.changeTenantStatus(tenantId, modifyTenantReq.getStatus());
        }
        return updateEntity(updateClause);
    }

    *//**
     * Un register tenant.
     *
     * @param tenantId the tenant id
     * @return the resp
     *//*
    @Transactional
    public Resp<Void> unRegisterTenant(Long tenantId) {
        log.info("Un-Register Tenant : {}", tenantId);
        var qApp = QApp.app;
        var count = sqlBuilder
                .selectFrom(qApp)
                .where(qApp.relTenantId.eq(tenantId))
                .fetchCount();
        if (count != 0) {
            log.warn("Un-Register Tenant error: need to delete the app first");
            return StandardResp.conflict(BUSINESS_TENANT, "请先删除租户下的所有应用");
        }
        // 删除账号
        accountService.deleteAccounts(tenantId);
        // 删除机构
        organizationService.deleteOrganization(Constant.OBJECT_UNDEFINED, tenantId);
        // 删除职位
        positionService.deletePositions(Constant.OBJECT_UNDEFINED, tenantId);
        // 删除租户认证
        deleteTenantIdent(tenantId);
        var qTenant = QTenant.tenant;
        interceptService.changeTenantStatus(tenantId, CommonStatus.DISABLED);
        return softDelEntity(sqlBuilder
                .selectFrom(qTenant)
                .where(qTenant.id.eq(tenantId))
        );
    }

    *//**
     * Allow account register.
     *
     * @param tenantId the tenant id
     * @return the resp
     *//*
    protected Resp<Boolean> allowAccountRegister(Long tenantId) {
        var qTenant = QTenant.tenant;
        var isAllow = sqlBuilder.select(qTenant.allowAccountRegister)
                .from(qTenant)
                .where(qTenant.id.eq(tenantId))
                .fetchOne();
        return StandardResp.success(isAllow);
    }

    // ========================== Ident ==============================

    *//**
     * Add tenant ident.
     *
     * @param addTenantIdentReq the add tenant ident req
     * @param relTenantId       the rel tenant id
     * @return the resp
     *//*
    @Transactional
    public Resp<Long> addTenantIdent(AddTenantIdentReq addTenantIdentReq,
                                     Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        if (sqlBuilder.select(qTenantIdent.id)
                .from(qTenantIdent)
                .where(qTenantIdent.relTenantId.eq(relTenantId))
                .where(qTenantIdent.kind.eq(addTenantIdentReq.getKind()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_TENANT_CERT, "认证已存在");
        }
        log.info("Add Tenant Ident : {}", $.json.toJsonString(addTenantIdentReq));
        var tenantIdent = TenantIdent.builder()
                .kind(addTenantIdentReq.getKind())
                .validRuleNote(addTenantIdentReq.getValidRuleNote() != null ? addTenantIdentReq.getValidRuleNote() : "")
                .validRule(addTenantIdentReq.getValidRule() != null ? addTenantIdentReq.getValidRule() : "")
                .validTimeSec(addTenantIdentReq.getValidTimeSec() != null
                        ? addTenantIdentReq.getValidTimeSec() : Constant.OBJECT_UNDEFINED)
                .oauthAk(addTenantIdentReq.getOauthAk() != null ? addTenantIdentReq.getOauthAk() : "")
                .oauthSk(addTenantIdentReq.getOauthSk() != null ? addTenantIdentReq.getOauthSk() : "")
                .status(CommonStatus.ENABLED)
                .relTenantId(relTenantId)
                .build();
        return saveEntity(tenantIdent);
    }

    *//**
     * Find tenant ident info.
     *
     * @param relTenantId the rel tenant id
     * @return the resp
     *//*
    public Resp<List<TenantIdentInfoResp>> findTenantIdentInfo(Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        var qAccountCreateUser = new QAccount("createUser");
        var qAccountUpdateUser = new QAccount("updateUser");
        var query = sqlBuilder
                .select(Projections.bean(
                        TenantIdentInfoResp.class,
                        qTenantIdent.id,
                        qTenantIdent.kind,
                        qTenantIdent.validRuleNote,
                        qTenantIdent.validRule,
                        qTenantIdent.validTimeSec,
                        qTenantIdent.oauthAk,
                        qTenantIdent.oauthSk,
                        qTenantIdent.status,
                        qTenantIdent.createTime,
                        qTenantIdent.updateTime,
                        qAccountCreateUser.name.as("createUserName"),
                        qAccountUpdateUser.name.as("updateUserName")))
                .from(qTenantIdent)
                .leftJoin(qAccountCreateUser).on(qTenantIdent.createUser.eq(qAccountCreateUser.openId))
                .leftJoin(qAccountUpdateUser).on(qTenantIdent.updateUser.eq(qAccountUpdateUser.openId))
                .where(qTenantIdent.relTenantId.eq(relTenantId));
        return findDTOs(query);
    }

    *//**
     * Modify tenant ident.
     *
     * @param modifyTenantIdentReq the modify tenant ident req
     * @param tenantIdentId        the tenant ident id
     * @param relTenantId          the rel tenant id
     * @return the resp
     *//*
    @Transactional
    public Resp<Void> modifyTenantIdent(ModifyTenantIdentReq modifyTenantIdentReq, Long tenantIdentId,
                                        Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        var updateClause = sqlBuilder.update(qTenantIdent)
                .where(qTenantIdent.id.eq(tenantIdentId))
                .where(qTenantIdent.relTenantId.eq(relTenantId));
        if (modifyTenantIdentReq.getValidRule() != null) {
            updateClause.set(qTenantIdent.validRule, modifyTenantIdentReq.getValidRule());
        }
        if (modifyTenantIdentReq.getValidRuleNote() != null) {
            updateClause.set(qTenantIdent.validRuleNote, modifyTenantIdentReq.getValidRuleNote());
        }
        if (modifyTenantIdentReq.getValidTimeSec() != null) {
            updateClause.set(qTenantIdent.validTimeSec, modifyTenantIdentReq.getValidTimeSec());
        }
        if (modifyTenantIdentReq.getOauthAk() != null) {
            updateClause.set(qTenantIdent.oauthAk, modifyTenantIdentReq.getOauthAk());
        }
        if (modifyTenantIdentReq.getOauthSk() != null) {
            updateClause.set(qTenantIdent.oauthSk, modifyTenantIdentReq.getOauthSk());
        }
        if (modifyTenantIdentReq.getStatus() != null) {
            updateClause.set(qTenantIdent.status, modifyTenantIdentReq.getStatus());
        }
        return updateEntity(updateClause);
    }

    *//**
     * Delete tenant ident.
     *
     * @param relTenantId the rel tenant id
     * @return the resp
     *//*
    @Transactional
    public Resp<Long> deleteTenantIdent(Long relTenantId) {
        log.info("Delete Tenant Ident By TenantId : {}", relTenantId);
        var qTenantIdent = QTenantIdent.tenantIdent;
        return softDelEntities(sqlBuilder
                .selectFrom(qTenantIdent)
                .where(qTenantIdent.relTenantId.eq(relTenantId)));
    }

    *//**
     * Delete tenant ident.
     *
     * @param tenantIdentId the tenant ident id
     * @param relTenantId   the rel tenant id
     * @return the resp
     *//*
    @Transactional
    public Resp<Void> deleteTenantIdent(Long tenantIdentId, Long relTenantId) {
        log.info("Delete Tenant Ident By Id : {}", tenantIdentId);
        var qTenantIdent = QTenantIdent.tenantIdent;
        return deleteEntity(sqlBuilder
                .delete(qTenantIdent)
                .where(qTenantIdent.id.eq(tenantIdentId))
                .where(qTenantIdent.relTenantId.eq(relTenantId)));
    }

    *//**
     * Check valid rule and return valid time.
     *
     * @param kind        the kind
     * @param sk          the sk
     * @param relTenantId the rel tenant id
     * @return the resp
     *//*
    protected Resp<Date> checkValidRuleAndReturnValidTime(AccountIdentKind kind, String sk, Long relTenantId) {
        if (relTenantId.equals(Constant.OBJECT_UNDEFINED)) {
            // 表示租户管理员注册时临时分配的虚拟租户号
            return StandardResp.success(Constant.NEVER_EXPIRE_TIME);
        }
        var qTenantIdent = QTenantIdent.tenantIdent;
        var tenantIdent = sqlBuilder
                .select(qTenantIdent.validRule,
                        qTenantIdent.validTimeSec)
                .from(qTenantIdent)
                .where(qTenantIdent.status.eq(CommonStatus.ENABLED))
                .where(qTenantIdent.kind.eq(kind))
                .where(qTenantIdent.relTenantId.eq(relTenantId))
                .fetchOne();
        if (tenantIdent == null) {
            return StandardResp.badRequest(BUSINESS_TENANT_CERT, "认证不存在或已禁用");
        }
        var validRule = tenantIdent.get(0, String.class);
        var validTimeSec = tenantIdent.get(1, Long.class);
        if (!StringUtils.isEmpty(validRule)) {
            if (!VALID_RULES.containsKey(validRule)) {
                VALID_RULES.put(validRule, Pattern.compile(validRule));
            }
            if (!VALID_RULES.get(validRule).matcher(sk).matches()) {
                return StandardResp.badRequest(BUSINESS_TENANT_CERT, "认证密钥规则不合法");
            }
        }
        return StandardResp.success(validTimeSec == null || validTimeSec.equals(Constant.OBJECT_UNDEFINED)
                ? Constant.NEVER_EXPIRE_TIME
                : new Date(System.currentTimeMillis() + validTimeSec * 1000));
    }

    *//**
     * Check valid rule.
     *
     * @param kind        the kind
     * @param sk          the sk
     * @param relTenantId the rel tenant id
     * @return the resp
     *//*
    protected Resp<Boolean> checkValidRule(AccountIdentKind kind, String sk, Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        var validRule = sqlBuilder
                .select(qTenantIdent.validRule)
                .from(qTenantIdent)
                .where(qTenantIdent.status.eq(CommonStatus.ENABLED))
                .where(qTenantIdent.kind.eq(kind))
                .where(qTenantIdent.relTenantId.eq(relTenantId))
                .fetchOne();
        if (validRule == null) {
            return StandardResp.badRequest(BUSINESS_TENANT_CERT, "认证不存在或已禁用");
        }
        if (!StringUtils.isEmpty(validRule)) {
            if (!VALID_RULES.containsKey(validRule)) {
                VALID_RULES.put(validRule, Pattern.compile(validRule));
            }
            if (!VALID_RULES.get(validRule).matcher(sk).matches()) {
                return StandardResp.badRequest(BUSINESS_TENANT_CERT, "认证密钥规则不合法");
            }
        }
        return StandardResp.success(true);
    }

    *//**
     * Gets tenant ident.
     *
     * @param kind        the kind
     * @param relTenantId the rel tenant id
     * @return the tenant ident
     *//*
    protected Resp<TenantIdent> getTenantIdent(AccountIdentKind kind, Long relTenantId) {
        var qTenantIdent = QTenantIdent.tenantIdent;
        return getDTO(sqlBuilder
                .selectFrom(qTenantIdent)
                .where(qTenantIdent.relTenantId.eq(relTenantId))
                .where(qTenantIdent.status.eq(CommonStatus.ENABLED))
                .where(qTenantIdent.kind.eq(kind)));
    }*/

}
