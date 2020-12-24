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

import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.fun.cache.FunCacheClient;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.cache.CacheConstant;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;

/**
 * 缓存服务.
 *
 * @author gudaoxuri
 */
@Slf4j
public class CacheProcessor extends EventBusProcessor {

    public CacheProcessor(String moduleName) {
        super(moduleName);
    }

    {
        addProcessor("/**", eventBusContext ->
                exec(
                        OptActionKind.parse(eventBusContext.req.header.get(DewConstant.REQUEST_RESOURCE_ACTION_FLAG)),
                        eventBusContext.req.header.get(DewConstant.REQUEST_RESOURCE_URI_FLAG),
                        eventBusContext.req.body(Buffer.class),
                        eventBusContext.context));
    }


    public static Future<?> exec(OptActionKind actionKind, String strResourceUri, Buffer body, ProcessContext context) {
        var resourceUri = URIHelper.newURI(strResourceUri);
        var resourceSubjectCode = resourceUri.getHost();
        if (!FunCacheClient.contains(resourceSubjectCode)) {
            throw context.helper.error(new BadRequestException("请求的资源主题[" + resourceSubjectCode + "]不存在"));
        }
        var resourcePath = resourceUri.getPath().substring(1).split("/");
        if (resourcePath.length < 1) {
            throw context.helper.error(new BadRequestException("请求的格式不正确"));
        }
        var resourceQuery = URIHelper.getSingleValueQuery(resourceUri.getQuery());
        var key = resourcePath[0];
        var fieldName = resourcePath.length == 2 ? resourcePath[1] : null;
        var cacheClient = FunCacheClient.choose(resourceSubjectCode);
        switch (actionKind) {
            case EXISTS:
                if (fieldName == null) {
                    return cacheClient.exists(key);
                } else {
                    return cacheClient.hexists(key, fieldName);
                }
            case FETCH:
                if (fieldName == null) {
                    return cacheClient.get(key);
                } else if (fieldName.isBlank() || fieldName.equals("*")) {
                    return cacheClient.hgetall(key);
                } else {
                    return cacheClient.hget(key, fieldName);
                }
            case CREATE:
            case MODIFY:
                var strBody = body == null ? null : body.toString("utf-8");
                var attrExpire = resourceQuery.containsKey(CacheConstant.REQUEST_ATTR_EXPIRE_SEC)
                        && !resourceQuery.get(CacheConstant.REQUEST_ATTR_EXPIRE_SEC).isBlank()
                        ? Long.parseLong(resourceQuery.get(CacheConstant.REQUEST_ATTR_EXPIRE_SEC))
                        : null;
                var attrIncr = resourceQuery.containsKey(CacheConstant.REQUEST_ATTR_INCR);
                if (fieldName == null) {
                    if (strBody != null && !strBody.isBlank() && attrIncr) {
                        return cacheClient.incrby(key, Integer.valueOf(strBody));
                    }
                    if (strBody != null && !strBody.isBlank() && attrExpire == null) {
                        return cacheClient.set(key, strBody);
                    }
                    if (strBody != null && !strBody.isBlank() && attrExpire != null) {
                        return cacheClient.setex(key, strBody, attrExpire);
                    }
                    if ((strBody == null || strBody.isBlank()) && attrExpire != null) {
                        return cacheClient.expire(key, attrExpire);
                    }
                }
                if (fieldName != null) {
                    if (strBody != null && !strBody.isBlank() && attrIncr) {
                        return cacheClient.hincrby(key, fieldName, Integer.valueOf(strBody));
                    }
                    if (strBody != null && !strBody.isBlank() && attrExpire == null) {
                        return cacheClient.hset(key, fieldName, strBody);
                    }
                    if (attrExpire != null) {
                        return cacheClient.expire(key, attrExpire);
                    }
                }
                log.warn("[Cache]Unsupported operations: action = {}, key = {}, fieldKey = {}, body = {}, expire = {}", actionKind.toString(), key, fieldName, strBody, attrExpire);
                throw context.helper.error(new BadRequestException("请求的格式不正确"));
            case DELETE:
                if (fieldName == null || fieldName.isBlank() || fieldName.equals("*")) {
                    return cacheClient.del(key);
                } else {
                    return cacheClient.hdel(key, fieldName);
                }
            default:
                throw context.helper.error(new BadRequestException("请求操作不存在"));
        }
    }

}
