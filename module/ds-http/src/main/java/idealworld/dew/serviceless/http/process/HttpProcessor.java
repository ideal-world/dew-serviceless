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

import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.httpclient.FunHttpClient;
import idealworld.dew.framework.util.URIHelper;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Http服务.
 *
 * @author gudaoxuri
 */
@Slf4j
public class HttpProcessor extends EventBusProcessor {

    public HttpProcessor(String moduleName) {
        super(moduleName);
    }

    {
        addProcessor("/**", eventBusContext ->
                exec(
                        OptActionKind.parse(eventBusContext.req.header.get(DewConstant.REQUEST_RESOURCE_ACTION_FLAG)),
                        eventBusContext.req.header.get(DewConstant.REQUEST_RESOURCE_URI_FLAG),
                        eventBusContext.req.body(Buffer.class),
                        eventBusContext.req.header,
                        eventBusContext.context));
    }

    public static Future<Buffer> exec(OptActionKind actionKind, String strResourceUri, Buffer body, Map<String, String> header, ProcessContext context) {
        var resourceUri = URIHelper.newURI(strResourceUri);
        var resourceSubjectCode = resourceUri.getHost();
        if (!FunHttpClient.contains(resourceSubjectCode)) {
            throw context.helper.error(new BadRequestException("请求的资源主题[" + resourceSubjectCode + "]不存在"));
        }
        HttpMethod httpMethod = HttpMethod.GET;
        switch (actionKind) {
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
        header.remove(DewConstant.REQUEST_IDENT_OPT_FLAG);
        header.remove(DewConstant.REQUEST_RESOURCE_ACTION_FLAG);
        header.remove(DewConstant.REQUEST_RESOURCE_URI_FLAG);
        return FunHttpClient.choose(resourceSubjectCode).requestByResourceSubject(httpMethod,
                URIHelper.getPathAndQuery(resourceUri),
                body,
                header)
                .compose(resp -> context.helper.success(resp.body()));
    }

}
