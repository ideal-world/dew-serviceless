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
import idealworld.dew.baas.iam.scene.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.app.AppIdentModifyReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.app.AppIdentResp;
import idealworld.dew.baas.iam.scene.appconsole.service.ACAppService;
import idealworld.dew.baas.iam.scene.common.controller.IAMBasicController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 应用控制台下的应用控制器.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "App Console APP", description = "应用控制台下的应用控制器")
@RequestMapping(value = "/console/app/app")
@Validated
public class ACAppController extends IAMBasicController {

    @Autowired
    private ACAppService acAppService;

    @PostMapping(value = "ident")
    @Operation(summary = "添加当前应用的认证")
    public Resp<Long> addAppIdent(@Validated @RequestBody AppIdentAddReq appIdentAddReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAppService.addAppIdent(appIdentAddReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @PatchMapping(value = "ident/{appIdentId}")
    @Operation(summary = "修改当前应用的某个认证")
    public Resp<Void> modifyAppIdent(@PathVariable Long appIdentId,
                                     @Validated @RequestBody AppIdentModifyReq appIdentModifyReq) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAppService.modifyAppIdent(appIdentId, appIdentModifyReq, currentAppAndTenantId._0, currentAppAndTenantId._1);
    }

    @GetMapping(value = "ident")
    @Operation(summary = "获取当前应用的认证列表信息")
    public Resp<Page<AppIdentResp>> pageApps(@RequestParam Long pageNumber, @RequestParam Integer pageSize) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAppService.pageAppIdents(pageNumber, pageSize, currentAppAndTenantId._0);
    }

    @DeleteMapping(value = "ident/{appIdentId}")
    @Operation(summary = "删除当前应用的某个认证")
    public Resp<Void> deleteAppIdent(@PathVariable Long appIdentId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAppService.deleteAppIdent(appIdentId, currentAppAndTenantId._0);
    }

    @GetMapping(value = "ident/{appIdentId}/sk")
    @Operation(summary = "获取当前应用的某个认证SK")
    public Resp<String> showSk(@PathVariable Long appIdentId) {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAppService.showSk(appIdentId, currentAppAndTenantId._0);
    }


    @GetMapping(value = "publicKey")
    @Operation(summary = "获取当前应用的公钥")
    public Resp<String> showPublicKey() {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAppService.showPublicKey(currentAppAndTenantId._0);
    }

    @GetMapping(value = "privateKey")
    @Operation(summary = "获取当前应用的私钥")
    public Resp<String> showPrivateKey() {
        var currentAppAndTenantId = getCurrentAppAndTenantId();
        return acAppService.showPrivateKey(currentAppAndTenantId._0);
    }

}
