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

package idealworld.dew.serviceless.cache.exchange;

import com.ecfront.dew.common.$;
import idealworld.dew.serviceless.cache.CacheConfig;
import idealworld.dew.serviceless.common.CommonApplication;
import idealworld.dew.serviceless.common.CommonConfig;
import idealworld.dew.serviceless.common.dto.exchange.ResourceSubjectExchange;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.serviceless.common.funs.cache.RedisClient;
import idealworld.dew.serviceless.common.funs.exchange.ExchangeHelper;
import idealworld.dew.serviceless.common.funs.httpclient.HttpClient;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeProcessor {

    public static Future<Void> init(CacheConfig cacheConfig) {
        var header = HttpClient.getIdentOptHeader(cacheConfig.getIam().getAppId(), cacheConfig.getIam().getTenantId());
        HttpClient.request(HttpMethod.GET, cacheConfig.getIam().getUri() + "/console/app/resource/subject?qKind=" + ResourceKind.CACHE.toString(), null, header)
                .onSuccess(result -> {
                    var resourceSubjects = $.json.toList(
                            $.json.toJson(result.body().toString(StandardCharsets.UTF_8)).get("body"),
                                    ResourceSubjectExchange.class);
                    for (var resourceSubject : resourceSubjects) {
                        RedisClient.init(resourceSubject.getCode(), CommonApplication.VERTX,
                                CommonConfig.RedisConfig.builder()
                                        .uri(resourceSubject.getUri())
                                        .password(resourceSubject.getSk())
                                        .build());
                        log.info("[Exchange]Init [resourceSubject.code={}] data", resourceSubject.getCode());
                    }
                })
                .onFailure(e -> log.error("[Exchange]Init resourceSubjects error: {}", e.getMessage(), e.getCause()));
        return ExchangeHelper.register(new HashSet<>() {
            {
                add("resourcesubject." + ResourceKind.CACHE.toString().toLowerCase());
            }
        }, exchangeData -> {
            if (exchangeData.getActionKind() == OptActionKind.CREATE
                    || exchangeData.getActionKind() == OptActionKind.MODIFY) {
                var resourceSubject = $.json.toObject(exchangeData.getDetailData(), ResourceSubjectExchange.class);
                RedisClient.remove(resourceSubject.getCode());
                RedisClient.init(resourceSubject.getCode(), CommonApplication.VERTX,
                        CommonConfig.RedisConfig.builder()
                                .uri(resourceSubject.getUri())
                                .password(resourceSubject.getSk())
                                .build());
                log.info("[Exchange]Updated [resourceSubject.code={}] data", resourceSubject.getCode());
            } else if (exchangeData.getActionKind() == OptActionKind.DELETE) {
                var code = (String) exchangeData.getDetailData().get("code");
                RedisClient.remove(code);
                log.error("[Exchange]Removed [resourceSubject.code={}]", code);
            }
        });
    }

}
