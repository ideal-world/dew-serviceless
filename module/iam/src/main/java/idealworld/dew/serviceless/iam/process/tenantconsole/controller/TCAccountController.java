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

package idealworld.dew.serviceless.iam.process.tenantconsole.controller;

import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import idealworld.dew.serviceless.iam.scene.common.controller.IAMBasicController;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.account.*;
import idealworld.dew.serviceless.iam.scene.tenantconsole.service.TCAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户控制台下的账号控制器.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "Tenant Console Account", description = "租户控制台下的账号控制器")
@RequestMapping(value = "/console/tenant/account")
@Validated
public class TCAccountController extends IAMBasicController {

    @Autowired
    private TCAccountService tcAccountService;

    @PostMapping(value = "")
    @Operation(summary = "添加当前租户的账号")
    public Resp<Long> addAccount(@Validated @RequestBody AccountAddReq accountAddReq) {
        return tcAccountService.addAccount(accountAddReq, getCurrentTenantId());
    }

    @PatchMapping(value = "{accountId}")
    @Operation(summary = "修改当前租户的某个账号")
    public Resp<Void> modifyAccount(@PathVariable Long accountId,
                                    @Validated @RequestBody AccountModifyReq accountModifyReq) {
        return tcAccountService.modifyAccount(accountId, accountModifyReq, getCurrentTenantId());
    }

    @GetMapping(value = "{accountId}")
    @Operation(summary = "获取当前租户的某个账号信息")
    public Resp<AccountResp> getAccount(@PathVariable Long accountId) {
        return tcAccountService.getAccount(accountId, getCurrentTenantId());
    }

    @GetMapping(value = "")
    @Operation(summary = "获取当前租户的账号列表信息")
    public Resp<Page<AccountResp>> pageAccounts(@RequestParam Long pageNumber, @RequestParam Integer pageSize,
                                                @RequestParam(required = false) String qOpenId,
                                                @RequestParam(required = false) String qName) {
        return tcAccountService.pageAccounts(pageNumber, pageSize, qOpenId, qName, getCurrentTenantId());
    }

    @DeleteMapping(value = "{accountId}")
    @Operation(summary = "删除当前租户的某个账号、关联的账号认证、账号群组、账号角色、账号应用、账号绑定")
    public Resp<Void> deleteAccount(@PathVariable Long accountId) {
        return tcAccountService.deleteAccount(accountId, getCurrentTenantId());
    }

    // --------------------------------------------------------------------

    @PostMapping(value = "{accountId}/ident")
    @Operation(summary = "添加当前租户某个账号的认证")
    public Resp<Long> addAccountIdent(@PathVariable Long accountId,
                                      @Validated @RequestBody AccountIdentAddReq accountIdentAddReq) {
        return tcAccountService.addAccountIdent(accountIdentAddReq, accountId, getCurrentTenantId());
    }

    @PatchMapping(value = "{accountId}/ident/{accountIdentId}")
    @Operation(summary = "修改当前租户某个账号的某个认证")
    public Resp<Void> modifyAccountIdent(@PathVariable Long accountId,
                                         @PathVariable Long accountIdentId,
                                         @Validated @RequestBody AccountIdentModifyReq accountIdentModifyReq) {
        return tcAccountService.modifyAccountIdent(accountIdentId, accountIdentModifyReq, getCurrentTenantId());
    }

    @GetMapping(value = "{accountId}/ident")
    @Operation(summary = "获取当前租户某个账号的认证列表信息")
    public Resp<List<AccountIdentResp>> findAccountIdents(@PathVariable Long accountId) {
        return tcAccountService.findAccountIdents(accountId, getCurrentTenantId());
    }

    @DeleteMapping(value = "{accountId}/ident/{accountIdentId}")
    @Operation(summary = "删除当前租户某个账号的某个认证")
    public Resp<Void> deleteAccountIdent(@PathVariable Long accountId,
                                         @PathVariable Long accountIdentId) {
        return tcAccountService.deleteAccountIdent(accountIdentId, getCurrentTenantId());
    }

    // --------------------------------------------------------------------

    @PostMapping(value = "{accountId}/app/{appId}")
    @Operation(summary = "添加当前租户某个账号的关联应用")
    public Resp<Long> addAccountApp(@PathVariable Long accountId, @PathVariable Long appId) {
        return tcAccountService.addAccountApp(accountId, appId, getCurrentTenantId());
    }

    @DeleteMapping(value = "{accountId}/app/{accountAppId}")
    @Operation(summary = "删除当前租户某个账号的某个关联应用")
    public Resp<Void> deleteAccountApp(@PathVariable Long accountAppId) {
        return tcAccountService.deleteAccountApp(accountAppId, getCurrentTenantId());
    }

    // --------------------------------------------------------------------

    @PostMapping(value = "{accountId}/group/{groupNodeId}")
    @Operation(summary = "添加当前租户某个账号的关联群组")
    public Resp<Long> addAccountGroup(@PathVariable Long accountId, @PathVariable Long groupNodeId) {
        return tcAccountService.addAccountGroup(accountId, groupNodeId, getCurrentTenantId());
    }

    @DeleteMapping(value = "{accountId}/group/{accountGroupId}")
    @Operation(summary = "删除当前租户某个账号的某个关联群组")
    public Resp<Void> deleteAccountGroup(@PathVariable Long accountGroupId) {
        return tcAccountService.deleteAccountGroup(accountGroupId, getCurrentTenantId());
    }

    // --------------------------------------------------------------------

    @PostMapping(value = "{accountId}/role/{roleId}")
    @Operation(summary = "添加当前租户某个账号的关联角色")
    public Resp<Long> addAccountRole(@PathVariable Long accountId, @PathVariable Long roleId) {
        return tcAccountService.addAccountRole(accountId, roleId, getCurrentTenantId());
    }

    @DeleteMapping(value = "{accountId}/role/{accountRoleId}")
    @Operation(summary = "删除当前租户某个账号的某个关联角色")
    public Resp<Void> deleteAccountRole(@PathVariable Long accountRoleId) {
        return tcAccountService.deleteAccountRole(accountRoleId, getCurrentTenantId());
    }

}
