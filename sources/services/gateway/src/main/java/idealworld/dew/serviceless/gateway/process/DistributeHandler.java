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
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.serviceless.common.funs.httpclient.HttpClient;
import idealworld.dew.serviceless.common.funs.httpserver.CommonHttpHandler;
import idealworld.dew.serviceless.gateway.GatewayConfig;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

/**
 * 鉴权处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public class DistributeHandler extends CommonHttpHandler {

    private final GatewayConfig.Distribute distribute;

    public DistributeHandler(GatewayConfig.Distribute distribute) {
        this.distribute = distribute;
    }

    @Override
    public void handle(RoutingContext ctx) {
        var identOptInfo = (IdentOptCacheInfo) ctx.get(CONTEXT_INFO);
        var resourceUri = (URI) ctx.get(Constant.REQUEST_RESOURCE_URI_FLAG);
        var action = (OptActionKind) ctx.get(Constant.REQUEST_RESOURCE_ACTION_FLAG);

        Buffer body = ctx.getBody();
        Future<HttpResponse<Buffer>> request;
        var header = HttpClient.getIdentOptHeader(identOptInfo);
        header.put(Constant.REQUEST_RESOURCE_URI_FLAG, resourceUri.toString());
        header.put(Constant.REQUEST_RESOURCE_ACTION_FLAG, action.toString());
        switch (ResourceKind.parse(resourceUri.getScheme().toLowerCase())) {
            case HTTP:
                if (resourceUri.getHost().equalsIgnoreCase(distribute.getIamServiceName())) {
                    HttpMethod httpMethod = HttpMethod.GET;
                    switch (action) {
                        case FETCH:
                            httpMethod = HttpMethod.GET;
                            break;
                        case CREATE:
                            httpMethod = HttpMethod.POST;
                            break;
                        case MODIFY:
                            httpMethod = HttpMethod.PUT;
                            break;
                        case PATCH:
                            httpMethod = HttpMethod.PATCH;
                            break;
                        case DELETE:
                            httpMethod = HttpMethod.DELETE;
                            break;
                    }
                    request = HttpClient.choose("").request(httpMethod,
                            resourceUri.toString(),
                            body,
                            header,
                            distribute.getTimeoutMS());
                } else {
                    ctx.request().headers().forEach(h -> header.put(h.getKey(), h.getValue()));
                    request = HttpClient.choose("").request(HttpMethod.POST,
                            distribute.getHttpServiceName(),
                            distribute.getHttpServicePort(),
                            Constant.REQUEST_PATH_FLAG,
                            null,
                            body,
                            header,
                            distribute.getTimeoutMS());
                }
                break;
            case MENU:
            case ELEMENT:
                request = HttpClient.choose("").request(HttpMethod.POST,
                        distribute.getIamServiceName(),
                        distribute.getIamServicePort(),
                        Constant.REQUEST_PATH_FLAG,
                        null,
                        body,
                        header,
                        distribute.getTimeoutMS());
                break;
            case RELDB:
                request = HttpClient.choose("").request(HttpMethod.POST,
                        distribute.getReldbServiceName(),
                        distribute.getReldbServicePort(),
                        Constant.REQUEST_PATH_FLAG,
                        null,
                        body,
                        header,
                        distribute.getTimeoutMS());
                break;
            case CACHE:
                request = HttpClient.choose("").request(HttpMethod.POST,
                        distribute.getCacheServiceName(),
                        distribute.getCacheServicePort(),
                        Constant.REQUEST_PATH_FLAG,
                        null,
                        body,
                        header,
                        distribute.getTimeoutMS());
                break;
            case MQ:
                request = HttpClient.choose("").request(HttpMethod.POST,
                        distribute.getMqServiceName(),
                        distribute.getMqServicePort(),
                        Constant.REQUEST_PATH_FLAG,
                        null,
                        body,
                        header,
                        distribute.getTimeoutMS());
                break;
            case OBJECT:
                request = HttpClient.choose("").request(HttpMethod.POST,
                        distribute.getObjServiceName(),
                        distribute.getObjServicePort(),
                        Constant.REQUEST_PATH_FLAG,
                        null,
                        body,
                        header,
                        distribute.getTimeoutMS());
                break;
            default:
                error(StandardCode.NOT_FOUND, DistributeHandler.class, "资源类型不存在", ctx);
                return;
        }
        request
                .onSuccess(resp -> {
                    if (ResourceKind.parse(resourceUri.getScheme().toLowerCase()) == ResourceKind.HTTP
                            && !resourceUri.getHost().equalsIgnoreCase(distribute.getIamServiceName())) {
                        // 外部调用带上返回的HTTP Header
                        resp.headers().forEach(h -> ctx.response().putHeader(h.getKey(), h.getValue()));
                    }
                    ctx.end(resp.body());
                })
                .onFailure(e -> error(StandardCode.INTERNAL_SERVER_ERROR, DistributeHandler.class, "转发服务错误", ctx));
    }

}
