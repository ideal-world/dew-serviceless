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

package idealworld.dew.serviceless.iam.scene.appconsole.controller;

import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.authpolicy.AuthPolicyModifyReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.authpolicy.AuthPolicyResp;
import idealworld.dew.serviceless.iam.scene.appconsole.service.ACAuthPolicyService;
import idealworld.dew.serviceless.iam.scene.common.controller.IAMBasicController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用控制台下的权限策略控制器.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "App Console Auth Policy", description = "应用控制台下的权限策略控制器")
@RequestMapping(value = "/console/app/authpolicy")
@Validated
public class ACAuthPolicyController extends IAMBasicController {

    @Autowired
    private ACAuthPolicyService acAuthPolicyService;

    @PostMapping(value = "")
    @Operation(summary = "添加当前应用的权限策略")
    public Resp<List<Long>> addAuthPolicy(@Validated @RequestBody AuthPolicyAddReq authPolicyAddReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAuthPolicyService.addAuthPolicy(authPolicyAddReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @PatchMapping(value = "{authPolicyId}")
    @Operation(summary = "修改当前应用的某个权限策略")
    public Resp<Void> modifyAuthPolicy(@PathVariable Long authPolicyId,
                                       @Validated @RequestBody AuthPolicyModifyReq authPolicyModifyReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAuthPolicyService.modifyAuthPolicy(authPolicyId, authPolicyModifyReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "{authPolicyId}")
    @Operation(summary = "获取当前应用的某个权限策略信息")
    public Resp<AuthPolicyResp> getAuthPolicy(@PathVariable Long authPolicyId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAuthPolicyService.getAuthPolicy(authPolicyId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "")
    @Operation(summary = "获取当前应用的权限策略列表信息")
    public Resp<Page<AuthPolicyResp>> pageAuthPolicies(@RequestParam Long pageNumber, @RequestParam Integer pageSize) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAuthPolicyService.pageAuthPolicies(pageNumber, pageSize, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @DeleteMapping(value = "{authPolicyId}")
    @Operation(summary = "删除当前应用的某个权限策略")
    public Resp<Void> deleteAuthPolicy(@PathVariable Long authPolicyId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAuthPolicyService.deleteAuthPolicy(authPolicyId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

}
