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

package idealworld.dew.baas.iam.scene.tenantconsole.controller;

import com.ecfront.dew.common.Resp;
import idealworld.dew.baas.iam.scene.common.controller.IAMBasicController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
    private AccountService accountService;

    /**
     * 添加当前租户的账号.
     *
     * @param addAccountReq the add account req
     * @return the resp
     */
    @PostMapping(value = "")
    @Operation(summary = "添加当前租户的账号")
    public Resp<Long> addAccount(@Validated @RequestBody AddAccountReq addAccountReq) {
        return accountService.addAccountExt(addAccountReq, getCurrentTenantId());
    }

    /**
     * 获取当前租户的账号列表信息.
     *
     * @param pageNumber the page number
     * @param pageSize   the page size
     * @return the resp
     */
    @GetMapping(value = "")
    @Operation(summary = "获取当前租户的账号列表信息")
    public Resp<Page<AccountInfoResp>> findAccountInfo(
            @Parameter(name = "pageNumber", description = "当前页码", in = ParameterIn.QUERY, required = true)
            @RequestParam(value = "pageNumber") Long pageNumber,
            @Parameter(name = "pageSize", description = "每页记录数", in = ParameterIn.QUERY, required = true)
            @RequestParam(value = "pageSize") Integer pageSize
    ) {
        return accountService.pageAccountInfo(pageNumber, pageSize, getCurrentTenantId());
    }

    /**
     * 获取当前租户的某个账号信息.
     *
     * @param accountId the account id
     * @return the account info
     */
    @GetMapping(value = "{accountId}")
    @Operation(summary = "获取当前租户的某个账号信息")
    public Resp<AccountInfoResp> getAccountInfo(@PathVariable Long accountId) {
        return accountService.getAccountInfo(accountId, getCurrentTenantId());
    }

    /**
     * 修改当前租户的某个账号.
     *
     * @param accountId        the account id
     * @param modifyAccountReq the modify account req
     * @return the resp
     */
    @PatchMapping(value = "{accountId}")
    @Operation(summary = "修改当前租户的某个账号")
    public Resp<Void> modifyAccount(@PathVariable Long accountId,
                                    @Validated @RequestBody ModifyAccountReq modifyAccountReq) {
        return accountService.modifyAccount(modifyAccountReq, accountId, getCurrentTenantId());
    }

    /**
     * 删除当前租户的某个账号.
     *
     * @param accountId the account id
     * @return the resp
     */
    @DeleteMapping(value = "{accountId}")
    @Operation(summary = "删除当前租户的某个账号、关联的账号认证、账号岗位")
    public Resp<Void> deleteAccount(@PathVariable Long accountId) {
        return accountService.deleteAccount(accountId, getCurrentTenantId());
    }

    // ========================== Ident ==============================

    /**
     * 添加当前租户某个账号的认证.
     *
     * @param accountId          the account id
     * @param addAccountIdentReq the add account ident req
     * @return the resp
     */
    @PostMapping(value = "{accountId}/ident")
    @Operation(summary = "添加当前租户某个账号的认证")
    public Resp<Long> addAccountIdent(@PathVariable Long accountId,
                                      @Validated @RequestBody AddAccountIdentReq addAccountIdentReq) {
        return accountService.addAccountIdent(addAccountIdentReq, accountId, getCurrentTenantId());
    }

    /**
     * 获取当前租户某个账号的认证列表信息.
     *
     * @param accountId the account id
     * @return the resp
     */
    @GetMapping(value = "{accountId}/ident")
    @Operation(summary = "获取当前租户某个账号的认证列表信息")
    public Resp<List<AccountIdentInfoResp>> findAccountIdentInfo(@PathVariable Long accountId) {
        return accountService.findAccountIdentInfo(accountId, getCurrentTenantId());
    }

    /**
     * 修改当前租户某个账号的某个认证.
     *
     * @param accountId             the account id
     * @param accountIdentId        the account ident id
     * @param modifyAccountIdentReq the modify account ident req
     * @return the resp
     */
    @PatchMapping(value = "{accountId}/ident/{accountIdentId}")
    @Operation(summary = "修改当前租户某个账号的某个认证")
    public Resp<Void> modifyAccountIdent(@PathVariable Long accountId,
                                         @PathVariable Long accountIdentId,
                                         @Validated @RequestBody ModifyAccountIdentReq modifyAccountIdentReq) {
        return accountService.modifyAccountIdent(modifyAccountIdentReq, accountIdentId, accountId, getCurrentTenantId());
    }

    /**
     * 删除当前租户某个账号的某个认证.
     *
     * @param accountId      the account id
     * @param accountIdentId the account ident id
     * @return the resp
     */
    @DeleteMapping(value = "{accountId}/ident/{accountIdentId}")
    @Operation(summary = "删除当前租户某个账号的某个认证")
    public Resp<Void> deleteAccountIdent(@PathVariable Long accountId,
                                         @PathVariable Long accountIdentId) {
        return accountService.deleteAccountIdent(accountIdentId, accountId, getCurrentTenantId());
    }

    /**
     * 删除当前租户某个账号的所有认证.
     *
     * @param accountId the account id
     * @return the resp
     */
    @DeleteMapping(value = "{accountId}/ident")
    @Operation(summary = "删除当前租户某个账号的所有认证")
    public Resp<Long> deleteAccountIdents(@PathVariable Long accountId) {
        return accountService.deleteAccountIdents(accountId, getCurrentTenantId());
    }

    // ========================== Post ==============================

    /**
     * 添加当前租户某个账号的岗位.
     *
     * @param accountId         the account id
     * @param addAccountPostReq the add account post req
     * @return the resp
     */
    @PostMapping(value = "{accountId}/post")
    @Operation(summary = "添加当前租户某个账号的岗位")
    public Resp<Long> addAccountPost(@PathVariable Long accountId,
                                     @Validated @RequestBody AddAccountPostReq addAccountPostReq) {
        return accountService.addAccountPost(addAccountPostReq, accountId, getCurrentTenantId());
    }

    /**
     * 获取当前租户某个账号的岗位列表信息.
     *
     * @param accountId the account id
     * @return the resp
     */
    @GetMapping(value = "{accountId}/post")
    @Operation(summary = "获取当前租户某个账号的岗位列表信息")
    public Resp<List<AccountPostInfoResp>> findAccountPostInfo(@PathVariable Long accountId) {
        return accountService.findAccountPostInfo(accountId, getCurrentTenantId());
    }

    /**
     * 删除当前租户某个账号的某个岗位.
     *
     * @param accountId     the account id
     * @param accountPostId the account post id
     * @return the resp
     */
    @DeleteMapping(value = "{accountId}/post/{accountPostId}")
    @Operation(summary = "删除当前租户某个账号的某个岗位")
    public Resp<Void> deleteAccountPost(@PathVariable Long accountId,
                                        @PathVariable Long accountPostId) {
        return accountService.deleteAccountPost(accountPostId, accountId, getCurrentTenantId());
    }

}
