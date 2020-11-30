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

package idealworld.dew.serviceless.iam.scene.common.service;

import com.ecfront.dew.common.Resp;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.IdentOptInfo;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.enumeration.AccountIdentKind;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.service.*;
import idealworld.dew.serviceless.iam.scene.common.dto.tenant.TenantRegisterReq;
import idealworld.dew.serviceless.iam.scene.systemconsole.dto.TenantAddReq;
import idealworld.dew.serviceless.iam.scene.systemconsole.service.SCTenantService;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.account.AccountAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.account.AccountIdentAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.tenant.TenantCertAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.tenant.TenantIdentAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.service.TCAccountService;
import idealworld.dew.serviceless.iam.scene.tenantconsole.service.TCAppService;
import idealworld.dew.serviceless.iam.scene.tenantconsole.service.TCTenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 公共租户服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class CommonTenantService extends IAMBasicService {

    @Autowired
    private IAMConfig iamConfig;
    @Autowired
    private SCTenantService scTenantService;
    @Autowired
    private TCTenantService tcTenantService;
    @Autowired
    private TCAppService tcAppService;
    @Autowired
    private ACAppService acAppService;
    @Autowired
    private ACRoleService acRoleService;
    @Autowired
    private TCAccountService tcAccountService;
    @Autowired
    private ACResourceService acResourceService;
    @Autowired
    private CommonAccountService commonAccountService;

    @Transactional
    public Resp<IdentOptInfo> register(TenantRegisterReq tenantRegisterReq) {
        if (!iamConfig.getAllowTenantRegister()) {
            return StandardResp.locked(BUSINESS_TENANT, "当前设置不允许自助注册租户");
        }
        // 初始化租户
        var tenantId = scTenantService.addTenant(TenantAddReq.builder()
                .name(tenantRegisterReq.getTenantName())
                .icon(tenantRegisterReq.getIcon())
                .parameters(tenantRegisterReq.getParameters())
                .allowAccountRegister(tenantRegisterReq.getAllowAccountRegister())
                .build()).getBody();
        // 初始化租户认证
        tcTenantService.addTenantIdent(TenantIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .validAKRuleNote(iamConfig.getSecurity().getDefaultValidAKRuleNote())
                .validAKRule(iamConfig.getSecurity().getDefaultValidAKRule())
                .validSKRuleNote(iamConfig.getSecurity().getDefaultValidSKRuleNote())
                .validSKRule(iamConfig.getSecurity().getDefaultValidSKRule())
                .validTimeSec(Constant.OBJECT_UNDEFINED)
                .build(), tenantId);
        // 初始化租户凭证
        tcTenantService.addTenantCert(TenantCertAddReq.builder()
                .category("")
                .version(1)
                .build(), tenantId);
        // 初始化应用
        var appId = tcAppService.addApp(AppAddReq.builder()
                .name(tenantRegisterReq.getAppName())
                .build(), tenantId).getBody();
        // 初始化应用认证
        acAppService.addAppIdent(AppIdentAddReq.builder()
                .note("")
                .build(), appId,tenantId);
        // 初始化账号
        var account = tcAccountService.innerAddAccount(AccountAddReq.builder()
                .name(tenantRegisterReq.getTenantName() + " 管理员")
                .build(), tenantId).getBody();
        // 初始化账号认证
        var addAccountIdentR = tcAccountService.addAccountIdent(AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak(tenantRegisterReq.getAccountUserName())
                .sk(tenantRegisterReq.getAccountPassword())
                .build(), account.getId(), tenantId);
        if (!addAccountIdentR.ok()) {
            return Resp.error(addAccountIdentR);
        }
        // 初始化账号应用
        tcAccountService.addAccountApp(account.getId(), appId, tenantId);
        // 初始化账号角色
        tcAccountService.addAccountRole(account.getId(), acRoleService.getTenantAdminRoleId().getBody(), tenantId);
        tcAccountService.addAccountRole(account.getId(), acResourceService.getAppAdminRoleResourceId().getBody(), tenantId);
        // 登录
        return commonAccountService.loginWithoutAuth(account.getId(), account.getOpenId(), tenantRegisterReq.getAccountUserName(), appId, tenantId);
    }

}
