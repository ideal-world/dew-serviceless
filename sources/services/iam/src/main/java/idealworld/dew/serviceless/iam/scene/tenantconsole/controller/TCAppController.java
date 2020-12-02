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

package idealworld.dew.serviceless.iam.scene.tenantconsole.controller;

import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import idealworld.dew.serviceless.iam.scene.common.controller.IAMBasicController;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.app.AppModifyReq;
import idealworld.dew.serviceless.iam.scene.tenantconsole.dto.app.AppResp;
import idealworld.dew.serviceless.iam.scene.tenantconsole.service.TCAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 租户控制台下的应用控制器.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "Tenant Console APP", description = "租户控制台下的应用控制器")
@RequestMapping(value = "/console/tenant/app")
@Validated
public class TCAppController extends IAMBasicController {

    @Autowired
    private TCAppService tcAppService;

    @PostMapping(value = "")
    @Operation(summary = "添加当前租户的应用")
    public Resp<Long> addApp(@Validated @RequestBody AppAddReq appAddReq) {
        return tcAppService.addApp(appAddReq, getCurrentTenantId());
    }

    @PatchMapping(value = "{appId}")
    @Operation(summary = "修改当前租户的某个应用")
    public Resp<Void> modifyApp(@PathVariable Long appId,
                                @Validated @RequestBody AppModifyReq appModifyReq) {
        return tcAppService.modifyApp(appId, appModifyReq, getCurrentTenantId());
    }

    @GetMapping(value = "{appId}")
    @Operation(summary = "获取当前租户的某个应用信息")
    public Resp<AppResp> getApp(@PathVariable Long appId) {
        return tcAppService.getApp(appId, getCurrentTenantId());
    }

    @GetMapping(value = "")
    @Operation(summary = "获取当前租户的应用列表信息")
    public Resp<Page<AppResp>> pageApps(@RequestParam Long pageNumber, @RequestParam Integer pageSize,
                                        @RequestParam(required = false) String qName) {
        return tcAppService.pageApps(pageNumber, pageSize, qName, getCurrentTenantId());
    }

}
