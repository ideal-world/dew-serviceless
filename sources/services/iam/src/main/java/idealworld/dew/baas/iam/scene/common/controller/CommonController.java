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

package idealworld.dew.baas.iam.scene.common.controller;

import com.ecfront.dew.common.Resp;
import idealworld.dew.baas.common.dto.IdentOptInfo;
import idealworld.dew.baas.iam.scene.common.dto.account.AccountChangeReq;
import idealworld.dew.baas.iam.scene.common.dto.account.AccountIdentChangeReq;
import idealworld.dew.baas.iam.scene.common.dto.account.AccountLoginReq;
import idealworld.dew.baas.iam.scene.common.dto.account.AccountRegisterReq;
import idealworld.dew.baas.iam.scene.common.service.CommonAccountService;
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
    private CommonAccountService commonAccountService;

    @PostMapping(value = "account")
    @Operation(summary = "注册账号")
    public Resp<IdentOptInfo> register(@Validated @RequestBody AccountRegisterReq accountRegisterReq) {
        return commonAccountService.register(accountRegisterReq);
    }

    @PostMapping(value = "login")
    @Operation(summary = "登录")
    public Resp<IdentOptInfo> login(@Validated @RequestBody AccountLoginReq accountLoginReq) {
        return commonAccountService.login(accountLoginReq);
    }

    @PostMapping(value = "logout")
    @Operation(summary = "退出登录")
    public Resp<Void> login(@Validated @RequestBody String token) {
        return commonAccountService.logout(token, getCurrentOpenId());
    }

    @PatchMapping(value = "account")
    @Operation(summary = "修改账号")
    public Resp<Void> changeInfo(@Validated @RequestBody AccountChangeReq accountChangeReq) {
        return commonAccountService.changeInfo(accountChangeReq, getCurrentOpenId());
    }

    @PatchMapping(value = "account/ident")
    @Operation(summary = "修改账号认证")
    public Resp<Void> changeIdent(@Validated @RequestBody AccountIdentChangeReq accountIdentChangeReq) {
        return commonAccountService.changeIdent(accountIdentChangeReq, getCurrentOpenId(), getCurrentAppAndTenantId()._0);
    }

    @DeleteMapping(value = "account")
    @Operation(summary = "注销账号")
    public Resp<Void> unRegister() {
        return commonAccountService.unRegister(getCurrentOpenId());
    }

}
