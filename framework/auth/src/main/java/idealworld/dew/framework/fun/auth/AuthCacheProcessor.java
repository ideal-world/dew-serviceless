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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * 登录鉴权处理器.
 *
 * @author gudaoxuri
 */
@Slf4j
public class AuthCacheProcessor {

    private static final String TOKEN_ID_REL_SPLIT = "##";

    // tenantId -> Token kind -> 保留的版本数
    private static final Map<Long, Map<String, Integer>> REVISION_HISTORY_VERSION = new ConcurrentHashMap<>();

    public static void addRevisionHistoryLimit(Long tenantId, String tokenKind, Integer revisionHistoryLimit) {
        if (!REVISION_HISTORY_VERSION.containsKey(tenantId)) {
            REVISION_HISTORY_VERSION.put(tenantId, new ConcurrentHashMap<>());
        }
        REVISION_HISTORY_VERSION.get(tenantId).put(tokenKind, revisionHistoryLimit);
    }

    public static Future<Optional<IdentOptExchangeInfo>> getOptInfo(String token, ProcessContext context) {
        return context.cache.get(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + token)
                .compose(optInfoStr -> {
                    if (optInfoStr != null && !optInfoStr.isEmpty()) {
                        var identOpt = new JsonObject(optInfoStr).mapTo(IdentOptExchangeInfo.class);
                        identOpt.setUnauthorizedAppId(identOpt.getAppId());
                        identOpt.setUnauthorizedTenantId(identOpt.getTenantId());
                        return context.helper.success(Optional.of(identOpt));
                    } else {
                        return context.helper.success(Optional.empty());
                    }
                });
    }

    public static Future<Void> removeOptInfo(String token, ProcessContext context) {
        var optInfoF = getOptInfo(token, context);
        return optInfoF.compose(tokenInfoOpt -> {
            if (tokenInfoOpt.isPresent()) {
                var delF = context.cache.del(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + token);
                return delF.compose(resp ->
                        removeOldToken(tokenInfoOpt.get().getAccountCode(),
                                tokenInfoOpt.get().getAppId(), tokenInfoOpt.get().getTokenKind(), context));
            } else {
                return context.helper.success();
            }
        });
    }

    public static Future<Void> setOptInfo(IdentOptCacheInfo optInfo, Long expireSec, ProcessContext context) {
        var setTokenInfoF = context.cache.setnx(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + optInfo.getToken(),
                JsonObject.mapFrom(optInfo).toString());
        return setTokenInfoF.compose(success -> {
            if (!success) {
                // 已存在
                return context.helper.success();
            }
            if (expireSec == DewAuthConstant.OBJECT_UNDEFINED) {
                var setRelF = context.cache.hset(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + optInfo.getAccountCode(),
                        optInfo.getToken(),
                        optInfo.getTokenKind() + TOKEN_ID_REL_SPLIT + System.currentTimeMillis());
                return setRelF.compose(resp -> removeOldToken(optInfo.getAccountCode(), optInfo.getTenantId(), optInfo.getTokenKind(), context));
            }
            var setExpireF = context.cache.expire(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + optInfo.getToken(), expireSec);
            return setExpireF.compose(resp ->
                    context.cache.hset(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + optInfo.getAccountCode(),
                            optInfo.getToken(),
                            optInfo.getTokenKind() + TOKEN_ID_REL_SPLIT + System.currentTimeMillis()))
                    .compose(resp -> removeOldToken(optInfo.getAccountCode(), optInfo.getTenantId(), optInfo.getTokenKind(), context));
        });
    }

    private static Future<Void> removeOldToken(Object accountCode, Long tenantId, String tokenKind, ProcessContext context) {
        // 当前 token kind 要求保留的历史版本数
        int revisionHistoryLimit = REVISION_HISTORY_VERSION.getOrDefault(tenantId, new HashMap<>()).getOrDefault(tokenKind, 1);
        // 当前 account code 关联的所有 token
        var getAllRelsF = context.cache.hgetall(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + accountCode);
        return getAllRelsF.compose(tokenKinds ->
                CompositeFuture.all(
                        tokenKinds.entrySet().stream()
                                .filter(entry -> entry.getValue().split(TOKEN_ID_REL_SPLIT)[0].equalsIgnoreCase(tokenKind))
                                // 按创建时间倒序
                                .sorted((entry1, entry2) ->
                                        Long.compare(Long.parseLong(entry2.getValue().split(TOKEN_ID_REL_SPLIT)[1]),
                                                Long.parseLong(entry1.getValue().split(TOKEN_ID_REL_SPLIT)[1])))
                                .skip(revisionHistoryLimit + 1)
                                .map(entry -> {
                                    // 删除多余版本
                                    var delF = context.cache.del(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + entry.getKey());
                                    return delF.compose(r ->
                                            context.cache.hdel(DewAuthConstant.CACHE_TOKEN_ID_REL_FLAG + accountCode,
                                                    entry.getKey()));
                                })
                                .collect(Collectors.toList())
                )
        )
                .compose(rsp -> context.helper.success());
    }

}
