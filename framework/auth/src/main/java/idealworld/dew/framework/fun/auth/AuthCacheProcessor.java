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

package idealworld.dew.framework.fun.auth;

import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 登录鉴权处理器.
 *
 * @author gudaoxuri
 * @author gjason
 */
public class AuthCacheProcessor {

    public static Future<Optional<IdentOptCacheInfo>> getOptInfo(String token, ProcessContext context) {
        return context.cache.get(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + token)
                .compose(optInfoStr -> {
                    if (optInfoStr != null && !optInfoStr.isEmpty()) {
                        return context.helper.success(Optional.of(new JsonObject(optInfoStr).mapTo(IdentOptCacheInfo.class)));
                    } else {
                        return context.helper.success(Optional.empty());
                    }
                });
    }

    public static Future<Void> removeOptInfo(String token, ProcessContext context) {
        return getOptInfo(token, context)
                .compose(tokenInfoOpt -> {
                    if (tokenInfoOpt.isPresent()) {
                        return context.cache.del(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + token)
                                .compose(resp ->
                                        removeOldToken(tokenInfoOpt.get().getAccountCode(),
                                                tokenInfoOpt.get().getTokenKind(), context));
                    } else {
                        return context.helper.success();
                    }
                });
    }

    public static Future<Void> setOptInfo(IdentOptCacheInfo optInfo, Long expireSec, ProcessContext context) {
        return context.cache.setnx(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + optInfo.getToken(),
                JsonObject.mapFrom(optInfo).toString())
                .compose(success -> {
                    if (!success) {
                        return context.helper.success();
                    }
                    if (expireSec == DewAuthConstant.OBJECT_UNDEFINED) {
                        return removeOldToken(optInfo.getAccountCode(), optInfo.getTokenKind(), context)
                                .compose(resp -> context.cache.hset(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + optInfo.getAccountCode(),
                                        optInfo.getTokenKind() + "##" + System.currentTimeMillis(),
                                        optInfo.getToken()));
                    }
                    return context.cache.expire(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + optInfo.getToken(), expireSec)
                            .compose(resp -> removeOldToken(optInfo.getAccountCode(), optInfo.getTokenKind(), context))
                            .compose(resp ->
                                    context.cache.hset(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + optInfo.getAccountCode(),
                                            optInfo.getTokenKind() + "##" + System.currentTimeMillis(),
                                            optInfo.getToken()));
                });
    }

    private static Future<Void> removeOldToken(Object accountCode, String tokenKind, ProcessContext context) {
        // 当前 token kind 要求保留的历史版本数
        // TODO
        int revisionHistoryLimit = 0;
        // 当前 account code 关联的所有 token
        return context.cache.hgetall(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + accountCode)
                .compose(tokenKinds ->
                        CompositeFuture.all(tokenKinds.entrySet().stream()
                                .map(entry ->
                                        context.cache.exists(entry.getValue())
                                                .compose(exists -> {
                                                    if (!exists) {
                                                        // 删除过期的关联token
                                                        return context.cache.hdel(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + accountCode, entry.getKey());
                                                    }
                                                    return context.helper.success(null);
                                                }))
                                .collect(Collectors.toList())))
                .compose(resp -> context.cache.hgetall(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + accountCode))
                .compose(tokenKinds ->
                        CompositeFuture.all(tokenKinds.keySet().stream()
                                .filter(key -> key.split("##")[0].equalsIgnoreCase(tokenKind))
                                // 按创建时间倒序
                                .sorted((key1, key2) ->
                                        Long.compare(Long.parseLong(key2.split("##")[1]), Long.parseLong(key1.split("##")[1])))
                                .skip(revisionHistoryLimit)
                                .map(key ->
                                        // 删除多余版本
                                        context.cache.del(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + tokenKinds.get(key))
                                                .compose(r ->
                                                        context.cache.hdel(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + accountCode, key)))
                                .collect(Collectors.toList())))
                .compose(rsp -> context.helper.success());
    }

}
