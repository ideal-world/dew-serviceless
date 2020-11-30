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

package idealworld.dew.serviceless.gateway.process;

import com.ecfront.dew.common.StandardCode;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.IdentOptCacheInfo;
import idealworld.dew.serviceless.common.enumeration.AuthResultKind;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.common.funs.httpserver.CommonHttpHandler;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * 鉴权处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public class AuthHandler extends CommonHttpHandler {

    private final GatewayAuthPolicy authPolicy;

    public AuthHandler(GatewayAuthPolicy authPolicy) {
        this.authPolicy = authPolicy;
    }

    @Override
    public void handle(RoutingContext ctx) {
        var resourceUri = (URI) ctx.get(Constant.REQUEST_RESOURCE_URI_FLAG);
        var action = (OptActionKind) ctx.get(Constant.REQUEST_RESOURCE_ACTION_FLAG);
        var identOptInfo = (IdentOptCacheInfo) ctx.get(CONTEXT_INFO);
        var subjectInfo = packageSubjectInfo(identOptInfo);
        authPolicy.authentication(action.toString(), resourceUri, subjectInfo)
                .onSuccess(authResultKind -> {
                    if (authResultKind == AuthResultKind.REJECT) {
                        error(StandardCode.UNAUTHORIZED, "鉴权错误，没有权限访问对应的资源[" + action + "|" + resourceUri.toString() + "]", ctx);
                        return;
                    }
                    ctx.next();
                })
                .onFailure(e -> error(StandardCode.INTERNAL_SERVER_ERROR, "服务错误", ctx, e));
    }


}
