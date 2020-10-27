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

package idealworld.dew.baas.iam.scene.appconsole.controller;

import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import idealworld.dew.baas.iam.scene.appconsole.dto.role.*;
import idealworld.dew.baas.iam.scene.appconsole.service.ACRoleService;
import idealworld.dew.baas.iam.scene.common.controller.IAMBasicController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 应用控制台下的角色控制器.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "App Console Role", description = "应用控制台下的角色控制器")
@RequestMapping(value = "/console/app/role")
@Validated
public class ACRoleController extends IAMBasicController {

    @Autowired
    private ACRoleService acRoleService;

    @PostMapping(value = "def")
    @Operation(summary = "添加当前应用的角色定义")
    public Resp<Long> addRoleDef(@Validated @RequestBody RoleDefAddReq roleDefAddReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acRoleService.addRoleDef(roleDefAddReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @PatchMapping(value = "def/{roleDefId}")
    @Operation(summary = "修改当前应用的某个角色定义")
    public Resp<Void> modifyRoleDef(@PathVariable Long roleDefId,
                                    @Validated @RequestBody RoleDefModifyReq roleDefModifyReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acRoleService.modifyRoleDef(roleDefId, roleDefModifyReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "def/{roleDefId}")
    @Operation(summary = "获取当前应用的某个角色定义信息")
    public Resp<RoleDefResp> getRoleDef(@PathVariable Long roleDefId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acRoleService.getRoleDef(roleDefId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "def")
    @Operation(summary = "获取当前应用的角色定义列表信息")
    public Resp<Page<RoleDefResp>> pageRoleDef(@RequestParam Long pageNumber, @RequestParam Integer pageSize) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acRoleService.pageRoleDef(pageNumber, pageSize, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @DeleteMapping(value = "def/{roleDefId}")
    @Operation(summary = "删除当前应用的某个角色定义")
    public Resp<Void> deleteRoleDef(@PathVariable Long roleDefId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acRoleService.deleteRoleDef(roleDefId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    // --------------------------------------------------------------------

    @PostMapping(value = "")
    @Operation(summary = "添加当前应用的角色")
    public Resp<Long> addRole(@Validated @RequestBody RoleAddReq roleAddReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acRoleService.addRole(roleAddReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @PatchMapping(value = "{roleId}")
    @Operation(summary = "修改当前应用的某个角色")
    public Resp<Void> modifyRole(@PathVariable Long roleId,
                                 @Validated @RequestBody RoleModifyReq roleModifyReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acRoleService.modifyRole(roleId, roleModifyReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "{roleId}")
    @Operation(summary = "获取当前应用的某个角色信息")
    public Resp<RoleResp> getRole(@PathVariable Long roleId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acRoleService.getRole(roleId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "")
    @Operation(summary = "获取当前应用的角色列表信息")
    public Resp<Page<RoleResp>> pageRoles(@RequestParam Long pageNumber, @RequestParam Integer pageSize) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acRoleService.pageRoles(pageNumber, pageSize, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @DeleteMapping(value = "{roleId}")
    @Operation(summary = "删除当前应用的某个角色")
    public Resp<Void> deleteRole(@PathVariable Long roleId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acRoleService.deleteRole(roleId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

}
