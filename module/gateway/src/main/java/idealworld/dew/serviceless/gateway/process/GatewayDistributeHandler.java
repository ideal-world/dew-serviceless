/*
 * Copyright 2021. gudaoxuri
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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.StandardCode;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.httpserver.AuthHttpHandler;
import idealworld.dew.framework.util.JsonHelper;
import idealworld.dew.serviceless.gateway.GatewayConfig;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * 鉴权处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public class GatewayDistributeHandler extends AuthHttpHandler {

    private final GatewayConfig.Distribute distribute;

    public GatewayDistributeHandler(String moduleName, GatewayConfig.Distribute distribute) {
        super(moduleName);
        this.distribute = distribute;
    }

    @Override
    public void handle(RoutingContext ctx) {
        var identOptInfo = (IdentOptExchangeInfo) ctx.get(CONTEXT_INFO);
        var actionKind = (OptActionKind) ctx.get(DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG);
        var uri = (URI) ctx.get(DewAuthConstant.REQUEST_RESOURCE_URI_FLAG);
        Buffer body = ctx.getBody();
        var header = new HashMap<String, String>();
        header.put(DewAuthConstant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64(JsonObject.mapFrom(identOptInfo).toString(), StandardCharsets.UTF_8));
        String distributeModuleName;
        switch (ResourceKind.parse(uri.getScheme().toLowerCase())) {
            case HTTP:
                if (uri.getHost().equalsIgnoreCase(distribute.getIamModuleName())) {
                    distributeModuleName = distribute.getIamModuleName();
                } else {
                    ctx.request().headers().forEach(h -> header.put(h.getKey(), h.getValue()));
                    distributeModuleName = distribute.getHttpModuleName();
                }
                break;
            case MENU:
            case ELEMENT:
                distributeModuleName = distribute.getIamModuleName();
                break;
            case RELDB:
                distributeModuleName = distribute.getReldbModuleName();
                break;
            case CACHE:
                distributeModuleName = distribute.getCacheModuleName();
                break;
            case MQ:
                distributeModuleName = distribute.getMqModuleName();
                break;
            case OBJECT:
                distributeModuleName = distribute.getObjModuleName();
                break;
            case TASK:
                distributeModuleName = distribute.getTaskModuleName();
                break;
            default:
                error(StandardCode.NOT_FOUND, GatewayDistributeHandler.class, "资源类型不存在", ctx);
                return;
        }
        FunEventBus.choose(getModuleName()).request(
                distributeModuleName,
                actionKind,
                uri.toString(),
                body,
                header)
                .onSuccess(resp -> {
                    if (ResourceKind.parse(uri.getScheme().toLowerCase()) == ResourceKind.HTTP
                            && !uri.getHost().equalsIgnoreCase(distribute.getIamModuleName())) {
                        // 外部调用带上返回的HTTP Header
                        resp._1.forEach((k, v) -> ctx.response().putHeader(k, v));
                    }
                    ctx.end(JsonHelper.appendBuffer(new JsonObject().put("code", "200"), "body", resp._0).toBuffer());
                })
                .onFailure(e -> error(ctx, e));
    }

}
