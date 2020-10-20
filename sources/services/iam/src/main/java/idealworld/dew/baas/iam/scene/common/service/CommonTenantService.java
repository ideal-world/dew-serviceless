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

package idealworld.dew.baas.iam.scene.common.service;

import com.ecfront.dew.common.Resp;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.IAMConfig;
import idealworld.dew.baas.iam.enumeration.AccountIdentKind;
import idealworld.dew.baas.iam.scene.common.dto.tenant.TenantRegisterReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.account.AccountAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.account.AccountIdentAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.tenant.TenantAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.tenant.TenantCertAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.tenant.TenantIdentAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.service.TCAccountService;
import idealworld.dew.baas.iam.scene.tenantconsole.service.TCTenantService;
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
    private TCTenantService tcTenantService;
    @Autowired
    private TCAccountService tcAccountService;

    @Transactional
    public Resp<Long> register(TenantRegisterReq tenantRegisterReq) {
        if (!iamConfig.getAllowTenantRegister()) {
            return StandardResp.locked(BUSINESS_TENANT, "当前设置不允许自助注册租户");
        }
        // 初始化租户
        var tenantId = tcTenantService.addTenant(TenantAddReq.builder()
                .name(tenantRegisterReq.getName())
                .icon(tenantRegisterReq.getIcon())
                .parameters(tenantRegisterReq.getParameters())
                .allowAccountRegister(tenantRegisterReq.getAllowAccountRegister())
                .build()).getBody();
        // 初始化租户认证
        tcTenantService.addTenantIdent(TenantIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .validTimeSec(Constant.OBJECT_UNDEFINED)
                .build(), tenantId);
        // 初始化租户凭证
        tcTenantService.addTenantCert(TenantCertAddReq.builder()
                .category("")
                .version(1)
                .build(), tenantId);
        // 初始化账号
        var accountId = tcAccountService.addAccount(AccountAddReq.builder()
                .name(iamConfig.getApp().getIamAppName())
                .build(), tenantId).getBody();
        // 初始化账号认证
        tcAccountService.addAccountIdent(AccountIdentAddReq.builder()
                .kind(AccountIdentKind.USERNAME)
                .ak(tenantRegisterReq.getAccountUserName())
                .sk(tenantRegisterReq.getAccountPassword())
                .relAccountId(accountId).build(), tenantId);
        // 初始化账号角色
        // TODO
        return Resp.success(tenantId);
    }

}
