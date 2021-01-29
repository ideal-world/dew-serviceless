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

package idealworld.dew.framework.fun.auth;

import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 登录鉴权处理器.
 *
 * @author gudaoxuri
 * @author gjason
 */
@Slf4j
public class AuthCacheProcessor {

    public static Future<Optional<IdentOptExchangeInfo>> getOptInfo(String token, ProcessContext context) {
        return context.cache.get(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + token)
                .compose(optInfoStr -> {
                    if (optInfoStr != null && !optInfoStr.isEmpty()) {
                        var identOpt = new JsonObject(optInfoStr).mapTo(IdentOptExchangeInfo.class);
                        identOpt.setUnauthorizedAppId(identOpt.getAppId());
                        identOpt.setUnauthorizedTenantId(identOpt.getUnauthorizedTenantId());
                        return context.helper.success(Optional.of(identOpt));
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
                        return context.cache.hset(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + optInfo.getAccountCode(),
                                optInfo.getToken(),
                                optInfo.getTokenKind() + "##" + System.currentTimeMillis())
                                .compose(resp -> removeOldToken(optInfo.getAccountCode(), optInfo.getTokenKind(), context));
                    }
                    return context.cache.expire(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + optInfo.getToken(), expireSec)
                            .compose(resp ->
                                    context.cache.hset(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + optInfo.getAccountCode(),
                                            optInfo.getToken(),
                                            optInfo.getTokenKind() + "##" + System.currentTimeMillis()))
                            .compose(resp -> removeOldToken(optInfo.getAccountCode(), optInfo.getTokenKind(), context));
                });
    }

    private static Future<Void> removeOldToken(Object accountCode, String tokenKind, ProcessContext context) {
        // 当前 token kind 要求保留的历史版本数
        // TODO
        int revisionHistoryLimit = 0;
        // 当前 account code 关联的所有 token
        return context.cache.hgetall(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + accountCode)
                .compose(tokenKinds ->
                        CompositeFuture.all(
                                tokenKinds.entrySet().stream()
                                        .filter(entry -> entry.getValue().split("##")[0].equalsIgnoreCase(tokenKind))
                                        // 按创建时间倒序
                                        .sorted((entry1, entry2) ->
                                                Long.compare(Long.parseLong(entry2.getValue().split("##")[1]),
                                                        Long.parseLong(entry1.getValue().split("##")[1])))
                                        .skip(revisionHistoryLimit + 1)
                                        .map(entry ->
                                                // 删除多余版本
                                                context.cache.del(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + entry.getKey())
                                                        .compose(r ->
                                                                context.cache.hdel(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + accountCode,
                                                                        entry.getKey())))
                                        .collect(Collectors.toList())
                        )
                )
                .compose(rsp -> context.helper.success());
    }

}
