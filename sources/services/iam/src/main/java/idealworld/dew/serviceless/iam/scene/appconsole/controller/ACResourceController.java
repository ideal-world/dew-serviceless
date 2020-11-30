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
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.*;
import idealworld.dew.serviceless.iam.scene.appconsole.service.ACResourceService;
import idealworld.dew.serviceless.iam.scene.common.controller.IAMBasicController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用控制台下的资源控制器.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "App Console Resource", description = "应用控制台下的资源控制器")
@RequestMapping(value = "/console/app/resource")
@Validated
public class ACResourceController extends IAMBasicController {

    @Autowired
    private ACResourceService acResourceService;

    @PostMapping(value = "subject")
    @Operation(summary = "添加当前应用的资源主体")
    public Resp<Long> addResourceSubject(@Validated @RequestBody ResourceSubjectAddReq resourceSubjectAddReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acResourceService.addResourceSubject(resourceSubjectAddReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @PatchMapping(value = "subject/{resourceSubjectId}")
    @Operation(summary = "修改当前应用的某个资源主体")
    public Resp<Void> modifyResourceSubject(@PathVariable Long resourceSubjectId,
                                            @Validated @RequestBody ResourceSubjectModifyReq resourceSubjectModifyReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acResourceService.modifyResourceSubject(resourceSubjectId, resourceSubjectModifyReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "subject/{resourceSubjectId}")
    @Operation(summary = "获取当前应用的某个资源主体信息")
    public Resp<ResourceSubjectResp> getResourceSubject(@PathVariable Long resourceSubjectId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acResourceService.getResourceSubject(resourceSubjectId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "subject")
    @Operation(summary = "获取当前应用的资源主体列表信息")
    public Resp<List<ResourceSubjectResp>> findResourceSubjects() {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acResourceService.findResourceSubjects(currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @DeleteMapping(value = "subject/{resourceSubjectId}")
    @Operation(summary = "删除当前应用的某个资源主体")
    public Resp<Void> deleteResourceSubject(@PathVariable Long resourceSubjectId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acResourceService.deleteResourceSubject(resourceSubjectId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    // --------------------------------------------------------------------

    @PostMapping(value = "")
    @Operation(summary = "添加当前应用的资源")
    public Resp<Long> addResource(@Validated @RequestBody ResourceAddReq resourceAddReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acResourceService.addResource(resourceAddReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @PatchMapping(value = "{resourceId}")
    @Operation(summary = "修改当前应用的某个资源")
    public Resp<Void> modifyResource(@PathVariable Long resourceId,
                                     @Validated @RequestBody ResourceModifyReq resourceModifyReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acResourceService.modifyResource(resourceId, resourceModifyReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "{resourceId}")
    @Operation(summary = "获取当前应用的某个资源信息")
    public Resp<ResourceResp> getResource(@PathVariable Long resourceId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acResourceService.getResource(resourceId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "")
    @Operation(summary = "获取当前应用的资源列表信息")
    public Resp<List<ResourceResp>> findResources(@RequestParam(required = false, defaultValue = "false") Boolean expose) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return !expose
                ? acResourceService.findResources(currentAppAndTenantId._0, currentAppAndTenantId._1)
                : acResourceService.findExposeResources(currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @DeleteMapping(value = "{resourceId}")
    @Operation(summary = "删除当前应用的某个资源")
    public Resp<Void> deleteResource(@PathVariable Long resourceId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acResourceService.deleteResource(resourceId, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

}
