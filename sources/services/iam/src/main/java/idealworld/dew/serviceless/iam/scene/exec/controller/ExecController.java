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

package idealworld.dew.serviceless.iam.scene.exec.controller;

import com.ecfront.dew.common.Resp;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.IdentOptCacheInfo;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.common.util.URIHelper;
import idealworld.dew.serviceless.iam.scene.common.controller.IAMBasicController;
import idealworld.dew.serviceless.iam.scene.exec.service.ExecService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Optional;

/**
 * 资源操作执行.
 *
 * @author gudaoxuri
 */
@RestController
@Tag(name = "Common", description = "执行操作")
@RequestMapping(value = Constant.REQUEST_PATH_FLAG)
@Validated
public class ExecController extends IAMBasicController {

    protected static final String BUSINESS_EXEC = "EXEC";

    @Autowired
    private ExecService execService;

    @PostMapping(value = "")
    @Operation(summary = "执行接口")
    public Resp<?> exec(@RequestBody(required = false) Object body,
                        @RequestHeader(Constant.REQUEST_RESOURCE_URI_FLAG) String resourceUri,
                        @RequestHeader(Constant.REQUEST_RESOURCE_ACTION_FLAG) String resourceAction) {
        var uri = URIHelper.newURI(resourceUri);
        switch (ResourceKind.parse(uri.getScheme())) {
            case MENU:
                return execMenu(uri, OptActionKind.parse(resourceAction), body, getCurrentAppAndTenantId()._0, getCurrentAppAndTenantId()._1, getIdentOptCacheInfo());
            case ELEMENT:
                return execElement(uri, OptActionKind.parse(resourceAction), body, getCurrentAppAndTenantId()._0, getCurrentAppAndTenantId()._1, getIdentOptCacheInfo());
            default:
                return StandardResp.badRequest(BUSINESS_EXEC, "请求的资源类型[" + uri.getScheme() + "]不合法");
        }
    }

    private Resp<?> execMenu(URI resourceUri, OptActionKind actionKind, Object body, Long relAppId, Long relTenantId, Optional<IdentOptCacheInfo> identOptCacheInfo) {
        switch (actionKind) {
            case FETCH:
                return execService.findResources(ResourceKind.MENU, relAppId, relTenantId, identOptCacheInfo);
            default:
                return StandardResp.badRequest(BUSINESS_EXEC, "请求的资源操作类型[" + actionKind.toString() + "]暂不支持");
        }
    }

    private Resp<?> execElement(URI resourceUri, OptActionKind actionKind, Object body, Long relAppId, Long relTenantId, Optional<IdentOptCacheInfo> identOptCacheInfo) {
        switch (actionKind) {
            case FETCH:
                return execService.findResources(ResourceKind.ELEMENT, relAppId, relTenantId, identOptCacheInfo);
            default:
                return StandardResp.badRequest(BUSINESS_EXEC, "请求的资源操作类型[" + actionKind.toString() + "]暂不支持");
        }
    }


}
