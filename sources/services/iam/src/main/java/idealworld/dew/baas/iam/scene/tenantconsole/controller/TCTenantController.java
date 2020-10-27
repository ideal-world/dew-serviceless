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

import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import idealworld.dew.baas.iam.scene.common.controller.IAMBasicController;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.tenant.*;
import idealworld.dew.baas.iam.scene.tenantconsole.service.TCTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 租户控制台下的租户控制器.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "Tenant Console Tenant", description = "租户控制台下的租户控制器")
@RequestMapping(value = "/console/tenant/tenant")
@Validated
public class TCTenantController extends IAMBasicController {

    @Autowired
    private TCTenantService tcTenantService;

    @PatchMapping(value = "")
    @Operation(summary = "修改当前租户")
    public Resp<Void> modifyTenant(@Validated @RequestBody TenantModifyReq tenantModifyReq) {
        return tcTenantService.modifyTenant(tenantModifyReq, getCurrentTenantId());
    }

    @GetMapping(value = "")
    @Operation(summary = "获取当前租户信息")
    public Resp<TenantResp> getTenant() {
        return tcTenantService.getTenant(getCurrentTenantId());
    }

    // --------------------------------------------------------------------

    @PostMapping(value = "ident")
    @Operation(summary = "添加当前租户的认证")
    public Resp<Long> addTenantIdent(TenantIdentAddReq tenantIdentAddReq) {
        return tcTenantService.addTenantIdent(tenantIdentAddReq, getCurrentTenantId());
    }

    @PatchMapping(value = "ident/{tenantIdentId}")
    @Operation(summary = "修改当前租户的某个认证")
    public Resp<Void> modifyTenantIdent(@PathVariable Long tenantIdentId,
                                        @Validated @RequestBody TenantIdentModifyReq tenantIdentModifyReq) {
        return tcTenantService.modifyTenantIdent(tenantIdentId, tenantIdentModifyReq, getCurrentTenantId());
    }

    @GetMapping(value = "ident/{tenantIdentId")
    @Operation(summary = "获取当前租户的某个认证信息")
    public Resp<TenantIdentResp> getTenantIdent(@PathVariable Long tenantIdentId) {
        return tcTenantService.getTenantIdent(tenantIdentId, getCurrentTenantId());
    }

    @GetMapping(value = "ident")
    @Operation(summary = "获取当前租户的认证列表信息")
    public Resp<Page<TenantIdentResp>> pageTenantIdents(@RequestParam Long pageNumber, @RequestParam Integer pageSize) {
        return tcTenantService.pageTenantIdents(pageNumber, pageSize, getCurrentTenantId());
    }

    @DeleteMapping(value = "ident/{tenantIdentId}")
    @Operation(summary = "删除当前租户的某个认证")
    public Resp<Void> deleteTenantIdent(@PathVariable Long tenantIdentId) {
        return tcTenantService.deleteTenantIdent(tenantIdentId, getCurrentTenantId());
    }

    // --------------------------------------------------------------------

    @PostMapping(value = "cert")
    @Operation(summary = "添加当前租户的凭证")
    public Resp<Long> addTenantCert(TenantCertAddReq tenantCertAddReq) {
        return tcTenantService.addTenantCert(tenantCertAddReq, getCurrentTenantId());
    }

    @PatchMapping(value = "cert/{tenantCertId}")
    @Operation(summary = "修改当前租户的某个凭证")
    public Resp<Void> modifyTenantCert(@PathVariable Long tenantCertId,
                                       @Validated @RequestBody TenantCertModifyReq tenantCertModifyReq) {
        return tcTenantService.modifyTenantCert(tenantCertId, tenantCertModifyReq, getCurrentTenantId());
    }

    @GetMapping(value = "cert/{tenantCertId")
    @Operation(summary = "获取当前租户的某个凭证信息")
    public Resp<TenantCertResp> getTenantCert(@PathVariable Long tenantCertId) {
        return tcTenantService.getTenantCert(tenantCertId, getCurrentTenantId());
    }


    @GetMapping(value = "cert")
    @Operation(summary = "获取当前租户的认证凭证信息")
    public Resp<Page<TenantCertResp>> pageTenantCerts(@RequestParam Long pageNumber, @RequestParam Integer pageSize) {
        return tcTenantService.pageTenantCerts(pageNumber, pageSize, getCurrentTenantId());
    }

    @DeleteMapping(value = "cert/{tenantCertId}")
    @Operation(summary = "删除当前租户的某个凭证")
    public Resp<Void> deleteTenantCert(@PathVariable Long tenantCertId) {
        return tcTenantService.deleteTenantCert(tenantCertId, getCurrentTenantId());
    }

}
