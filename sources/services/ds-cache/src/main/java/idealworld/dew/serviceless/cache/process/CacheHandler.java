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

package idealworld.dew.serviceless.cache.process;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.StandardCode;
import idealworld.dew.serviceless.cache.CacheConstant;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.common.funs.cache.RedisClient;
import idealworld.dew.serviceless.common.funs.httpserver.CommonHttpHandler;
import idealworld.dew.serviceless.common.util.URIHelper;
import io.vertx.core.AsyncResult;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public class CacheHandler extends CommonHttpHandler {

    @Override
    public void handle(RoutingContext ctx) {
        if (!ctx.request().headers().contains(Constant.REQUEST_RESOURCE_URI_FLAG)
                || !ctx.request().headers().contains(Constant.REQUEST_RESOURCE_ACTION_FLAG)) {
            error(StandardCode.BAD_REQUEST, CacheHandler.class, "请求格式不合法", ctx);
            return;
        }
        var strResourceUriWithoutPath = ctx.request().getHeader(Constant.REQUEST_RESOURCE_URI_FLAG);
        var resourceUri = URIHelper.newURI(strResourceUriWithoutPath);
        var resourceSubjectCode = resourceUri.getHost();
        var resourcePath = resourceUri.getPath().substring(1).split("/");
        var resourceQuery = URIHelper.getSingleValueQuery(resourceUri.getQuery());
        if (!RedisClient.contains(resourceSubjectCode)) {
            error(StandardCode.BAD_REQUEST, CacheHandler.class, "请求的资源主题不存在", ctx);
            return;
        }
        if (resourcePath.length < 1) {
            error(StandardCode.BAD_REQUEST, CacheHandler.class, "请求的格式不正确", ctx);
            return;
        }
        var key = resourcePath[0];
        var fieldName = resourcePath.length == 2 ? resourcePath[1] : null;
        var action = OptActionKind.parse(ctx.request().getHeader(Constant.REQUEST_RESOURCE_ACTION_FLAG));
        var redisClient = RedisClient.choose(resourceSubjectCode);
        switch (action) {
            case EXISTS:
                if (fieldName == null) {
                    redisClient.exists(key).onComplete(handler -> resultProcess(handler, ctx));
                } else {
                    redisClient.hexists(key, fieldName).onComplete(handler -> resultProcess(handler, ctx));
                }
                break;
            case FETCH:
                if (fieldName == null) {
                    redisClient.get(key).onComplete(handler -> resultProcess(handler, ctx));
                } else if (fieldName.isBlank() || fieldName.equals("*")) {
                    redisClient.hgetall(key).onComplete(handler -> resultProcess(handler, ctx));
                } else {
                    redisClient.hget(key, fieldName).onComplete(handler -> resultProcess(handler, ctx));
                }
                break;
            case CREATE:
            case MODIFY:
                var body = ctx.getBodyAsString();
                var attrExpire = resourceQuery.containsKey(CacheConstant.REQUEST_ATTR_EXPIRE)
                        && !resourceQuery.get(CacheConstant.REQUEST_ATTR_EXPIRE).isBlank()
                        ? Long.parseLong(resourceQuery.get(CacheConstant.REQUEST_ATTR_EXPIRE))
                        : null;
                var attrIncr = resourceQuery.containsKey(CacheConstant.REQUEST_ATTR_INCR);

                if (fieldName == null) {
                    if (body != null && !body.isBlank() && attrIncr) {
                        redisClient.incrby(key, Integer.valueOf(body)).onComplete(handler -> resultProcess(handler, ctx));
                        break;
                    }
                    if (body != null && !body.isBlank() && attrExpire == null) {
                        redisClient.set(key, body).onComplete(handler -> resultProcess(handler, ctx));
                        break;
                    }
                    if (body != null && !body.isBlank() && attrExpire != null) {
                        redisClient.setex(key, body, attrExpire).onComplete(handler -> resultProcess(handler, ctx));
                        break;
                    }
                    if ((body == null || body.isBlank()) && attrExpire != null) {
                        redisClient.expire(key, attrExpire).onComplete(handler -> resultProcess(handler, ctx));
                        break;
                    }
                }
                if (fieldName != null) {
                    if (body != null && !body.isBlank() && attrIncr) {
                        redisClient.hincrby(key, fieldName, Integer.valueOf(body)).onComplete(handler -> resultProcess(handler, ctx));
                        break;
                    }
                    if (body != null && !body.isBlank() && attrExpire == null) {
                        redisClient.hset(key, fieldName, body).onComplete(handler -> resultProcess(handler, ctx));
                        break;
                    }
                    if (attrExpire != null) {
                        redisClient.expire(key, attrExpire).onComplete(handler -> resultProcess(handler, ctx));
                        break;
                    }
                }
                log.warn("[Cache]Unsupported operations: action = {}, key = {}, fieldKey = {}, body = {}, expire = {}", action, key, fieldName, body, attrExpire);
                error(StandardCode.BAD_REQUEST, CacheHandler.class, "请求的格式不正确", ctx);
                break;
            case DELETE:
                if (fieldName == null || fieldName.isBlank() || fieldName.equals("*")) {
                    redisClient.del(key).onComplete(handler -> resultProcess(handler, ctx));
                } else {
                    redisClient.hdel(key, fieldName).onComplete(handler -> resultProcess(handler, ctx));
                }
                break;
        }

    }

    private <E> void resultProcess(AsyncResult<E> handler, RoutingContext ctx) {
        if (handler.failed()) {
            error(StandardCode.INTERNAL_SERVER_ERROR, CacheHandler.class, "缓存服务错误", ctx, handler.cause());
            return;
        }
        ctx.end($.json.toJsonString(Resp.success(handler.result())));
    }


}
