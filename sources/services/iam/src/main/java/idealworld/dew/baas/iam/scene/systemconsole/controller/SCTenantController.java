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

package idealworld.dew.baas.iam.scene.systemconsole.controller;

import com.ecfront.dew.common.Resp;
import idealworld.dew.baas.iam.scene.common.controller.IAMBasicController;
import idealworld.dew.baas.iam.scene.systemconsole.dto.TenantAddReq;
import idealworld.dew.baas.iam.scene.systemconsole.service.SCTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统控制台下的租户控制器.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "System Console Tenant", description = "系统控制台下的租户控制器")
@RequestMapping(value = "/console/system/tenant")
@Validated
public class SCTenantController extends IAMBasicController {

    @Autowired
    private SCTenantService scTenantService;

    @PostMapping(value = "")
    @Operation(summary = "添加租户")
    public Resp<Long> addTenant(TenantAddReq tenantAddReq) {
        return scTenantService.addTenant(tenantAddReq);
    }

}
