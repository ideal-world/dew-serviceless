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

package idealworld.dew.serviceless.iam.scene.common.controller;

import com.ecfront.dew.common.Resp;
import idealworld.dew.serviceless.common.dto.IdentOptInfo;
import idealworld.dew.serviceless.iam.scene.common.dto.account.*;
import idealworld.dew.serviceless.iam.scene.common.dto.tenant.TenantRegisterReq;
import idealworld.dew.serviceless.iam.scene.common.service.CommonAccountService;
import idealworld.dew.serviceless.iam.scene.common.service.CommonTenantService;
import idealworld.dew.serviceless.iam.scene.common.service.OAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 公共操作.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "Common", description = "公共操作")
@RequestMapping(value = "/common")
@Validated
public class CommonController extends IAMBasicController {

    @Autowired
    private CommonTenantService commonTenantService;
    @Autowired
    private CommonAccountService commonAccountService;
    @Autowired
    private OAuthService oAuthService;

    @PostMapping(value = "tenant")
    @Operation(summary = "注册租户")
    public Resp<IdentOptInfo> registerTenant(@Validated @RequestBody TenantRegisterReq tenantRegisterReq) {
        return commonTenantService.register(tenantRegisterReq);
    }

    @PostMapping(value = "account")
    @Operation(summary = "注册账号")
    public Resp<IdentOptInfo> registerAccount(@Validated @RequestBody AccountRegisterReq accountRegisterReq) {
        return commonAccountService.register(accountRegisterReq);
    }

    @PostMapping(value = "login")
    @Operation(summary = "登录")
    public Resp<IdentOptInfo> login(@Validated @RequestBody AccountLoginReq accountLoginReq) {
        return commonAccountService.login(accountLoginReq);
    }

    @PostMapping(value = "oauth/login")
    @Operation(summary = "OAuth登录")
    public Resp<IdentOptInfo> oauthLogin(@Validated @RequestBody AccountOAuthLoginReq accountOAuthLoginReq) {
        return oAuthService.login(accountOAuthLoginReq);
    }

    @PostMapping(value = "logout")
    @Operation(summary = "退出登录")
    public Resp<Void> login() {
        return commonAccountService.logout(getCurrentToken(), getCurrentOpenId());
    }

    @PatchMapping(value = "account")
    @Operation(summary = "修改账号")
    public Resp<Void> changeInfo(@Validated @RequestBody AccountChangeReq accountChangeReq) {
        return commonAccountService.changeInfo(accountChangeReq, getCurrentOpenId());
    }

    @PatchMapping(value = "account/ident")
    @Operation(summary = "修改账号认证")
    // TODO 目前只能修改sk
    public Resp<Void> changeIdent(@Validated @RequestBody AccountIdentChangeReq accountIdentChangeReq) {
        return commonAccountService.changeIdent(accountIdentChangeReq, getCurrentOpenId(), getCurrentAppAndTenantId()._0);
    }

    @DeleteMapping(value = "account")
    @Operation(summary = "注销账号")
    public Resp<Void> unRegister() {
        return commonAccountService.unRegister(getCurrentOpenId());
    }

}
