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

package idealworld.dew.serviceless.http.process;

import com.ecfront.dew.common.StandardCode;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.framework.fun.httpclient.HttpClient;
import idealworld.dew.framework.fun.httpserver.CommonHttpHandler;
import idealworld.dew.framework.util.URIHelper;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;

/**
 * 缓存处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public class HttpHandler extends CommonHttpHandler {

    @Override
    public void handle(RoutingContext ctx) {
        if (!ctx.request().headers().contains(Constant.REQUEST_RESOURCE_URI_FLAG)
                || !ctx.request().headers().contains(Constant.REQUEST_RESOURCE_ACTION_FLAG)) {
            error(StandardCode.BAD_REQUEST, HttpHandler.class, "请求格式不合法", ctx);
            return;
        }
        var strResourceUriWithoutPath = ctx.request().getHeader(Constant.REQUEST_RESOURCE_URI_FLAG);
        var resourceUri = URIHelper.newURI(strResourceUriWithoutPath);
        var resourceSubjectCode = resourceUri.getHost();
        if (!HttpClient.contains(resourceSubjectCode)) {
            error(StandardCode.BAD_REQUEST, HttpHandler.class, "请求的资源主题[" + resourceSubjectCode + "]不存在", ctx);
            return;
        }
        var action = OptActionKind.parse(ctx.request().getHeader(Constant.REQUEST_RESOURCE_ACTION_FLAG));
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
        var header = new HashMap<String, String>();
        ctx.request().headers().forEach(h -> header.put(h.getKey(), h.getValue()));
        header.remove(Constant.REQUEST_IDENT_OPT_FLAG);
        header.remove(Constant.REQUEST_RESOURCE_ACTION_FLAG);
        header.remove(Constant.REQUEST_RESOURCE_URI_FLAG);
        HttpClient.choose(resourceSubjectCode).requestByResourceSubject(httpMethod,
                URIHelper.getPathAndQuery(resourceUri),
                ctx.getBody(),
                header)
                .onSuccess(resp -> {
                    resp.headers().forEach(h -> ctx.response().putHeader(h.getKey(), h.getValue()));
                    ctx.end(resp.body());
                })
                .onFailure(e ->
                        error(StandardCode.INTERNAL_SERVER_ERROR, HttpHandler.class, "Http服务请求错误:" + resourceUri.toString(), ctx));
    }

}
