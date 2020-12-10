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

package idealworld.dew.framework.fun.httpserver;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.StandardCode;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 处理器
 *
 * @author gudaoxuri
 */
@Slf4j
@Data
public abstract class KernelHttpHandler implements Handler<RoutingContext> {

    private String moduleName;

    public KernelHttpHandler(String moduleName) {
        this.moduleName = moduleName;
    }

    protected void error(StandardCode statusCode, Class<?> clazz, String msg, RoutingContext ctx) {
        log.warn("[" + clazz.getSimpleName() + "]Request error [{}]: {}", statusCode.toString(), msg);
        ctx.response().setStatusCode(200).end($.json.toJsonString(new Resp<Void>(statusCode.toString(), msg, null)));
    }

    protected void error(StandardCode statusCode, Class<?> clazz, String msg, RoutingContext ctx, Throwable e) {
        log.warn("[" + clazz.getSimpleName() + "]Request error [{}]{}", statusCode.toString(), e.getMessage(), e);
        ctx.response().setStatusCode(200).end($.json.toJsonString(new Resp<Void>(statusCode.toString(), msg, null)));
    }

}
