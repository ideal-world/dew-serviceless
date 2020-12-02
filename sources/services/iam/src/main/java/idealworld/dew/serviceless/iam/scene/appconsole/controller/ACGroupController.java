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

package idealworld.dew.serviceless.iam.scene.appconsole.controller;

import com.ecfront.dew.common.Resp;
import idealworld.dew.serviceless.iam.enumeration.GroupKind;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.group.*;
import idealworld.dew.serviceless.iam.scene.appconsole.service.ACGroupService;
import idealworld.dew.serviceless.iam.scene.common.controller.IAMBasicController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用控制台下的群组控制器.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "App Console Group", description = "应用控制台下的群组控制器")
@RequestMapping(value = "/console/app/group")
@Validated
public class ACGroupController extends IAMBasicController {

    @Autowired
    private ACGroupService acGroupService;

    @PostMapping(value = "")
    @Operation(summary = "添加当前应用的群组")
    public Resp<Long> addGroup(@Validated @RequestBody GroupAddReq groupAddReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acGroupService.addGroup(groupAddReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @PatchMapping(value = "{groupId}")
    @Operation(summary = "修改当前应用的某个群组")
    public Resp<Void> modifyGroup(@PathVariable Long groupId,
                                  @Validated @RequestBody GroupModifyReq groupModifyReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acGroupService.modifyGroup(groupId, groupModifyReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "{groupId}")
    @Operation(summary = "获取当前应用的某个群组信息")
    public Resp<GroupResp> getGroup(@PathVariable Long groupId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acGroupService.getGroup(groupId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "")
    @Operation(summary = "获取当前应用的群组列表信息")
    public Resp<List<GroupResp>> findGroups(@RequestParam(required = false) String qCode,
                                            @RequestParam(required = false) String qName,
                                            @RequestParam(required = false) GroupKind qKind) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acGroupService.findGroups(qCode, qName, qKind, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @DeleteMapping(value = "{groupId}")
    @Operation(summary = "删除当前应用的某个群组")
    public Resp<Void> deleteGroup(@PathVariable Long groupId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acGroupService.deleteGroup(groupId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    // --------------------------------------------------------------------

    @PostMapping(value = "{groupId}/node")
    @Operation(summary = "添加当前应用某个群组的节点")
    public Resp<Long> addGroupNode(@PathVariable Long groupId,
                                   @Validated @RequestBody GroupNodeAddReq groupNodeAddReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acGroupService.addGroupNode(groupNodeAddReq, groupId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @PatchMapping(value = "{groupId}/node/{groupNodeId}")
    @Operation(summary = "修改当前应用某个群组的节点")
    public Resp<Void> modifyGroupNode(@PathVariable Long groupId,
                                      @PathVariable Long groupNodeId,
                                      @Validated @RequestBody GroupNodeModifyReq groupNodeModifyReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acGroupService.modifyGroupNode(groupNodeId, groupNodeModifyReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "{groupId}/node")
    @Operation(summary = "获取当前应用某个群组的节点列表信息")
    public Resp<List<GroupNodeResp>> findGroupNodes(@PathVariable Long groupId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acGroupService.findGroupNodes(groupId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @DeleteMapping(value = "{groupId}/node/{groupNodeId}")
    @Operation(summary = "删除当前应用某个群组的节点")
    public Resp<Void> deleteGroupNode(@PathVariable Long groupId, @PathVariable Long groupNodeId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acGroupService.deleteGroupNode(groupNodeId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

}
