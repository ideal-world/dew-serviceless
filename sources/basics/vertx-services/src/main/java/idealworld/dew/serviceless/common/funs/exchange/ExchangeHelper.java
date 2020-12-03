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

package idealworld.dew.serviceless.common.funs.exchange;

import com.ecfront.dew.common.$;
import idealworld.dew.serviceless.common.CommonConfig;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.exchange.ExchangeData;
import idealworld.dew.serviceless.common.dto.exchange.ResourceSubjectExchange;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.serviceless.common.funs.cache.RedisClient;
import idealworld.dew.serviceless.common.funs.httpclient.HttpClient;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeHelper {

    private static final String QUICK_CHECK_SPLIT = "#";

    public static Future<Void> loadAndWatchResourceSubject(CommonConfig config, ResourceKind kind, Consumer<ResourceSubjectExchange> addFun, Consumer<String> removeFun) {
        var header = HttpClient.getIdentOptHeader(config.getIam().getAppId(), config.getIam().getTenantId());
        HttpClient.choose("").request(HttpMethod.GET, config.getIam().getUri() + "/console/app/resource/subject?qKind=" + kind.toString(), null, header)
                .onSuccess(result -> {
                    var resourceSubjects = $.json.toList(
                            $.json.toJson(result.body().toString(StandardCharsets.UTF_8)).get("body"),
                            ResourceSubjectExchange.class);
                    for (var resourceSubject : resourceSubjects) {
                        addFun.accept(resourceSubject);
                        log.info("[Exchange]Init [resourceSubject.code={}] data", resourceSubject.getCode());
                    }
                })
                .onFailure(e -> log.error("[Exchange]Init resourceSubjects error: {}", e.getMessage(), e.getCause()));
        return watch(new HashSet<>() {
            {
                add("resourcesubject." + kind.toString().toLowerCase());
            }
        }, exchangeData -> {
            if (exchangeData.getActionKind() == OptActionKind.CREATE
                    || exchangeData.getActionKind() == OptActionKind.MODIFY) {
                var resourceSubject = $.json.toObject(exchangeData.getDetailData(), ResourceSubjectExchange.class);
                removeFun.accept(resourceSubject.getCode());
                addFun.accept(resourceSubject);
                log.info("[Exchange]Updated [resourceSubject.code={}] data", resourceSubject.getCode());
            } else if (exchangeData.getActionKind() == OptActionKind.DELETE) {
                var code = (String) exchangeData.getDetailData().get("code");
                removeFun.accept(code);
                log.error("[Exchange]Removed [resourceSubject.code={}]", code);
            }
        });
    }

    public static Future<Void> watch(Set<String> subjectCategories, Consumer<ExchangeData> fun) {
        Promise<Void> promise = Promise.promise();
        RedisClient.choose("").subscribe(Constant.EVENT_NOTIFY_TOPIC_BY_IAM, message -> {
            var quickCheckSplit = message.split(QUICK_CHECK_SPLIT);
            var subjectCategory = quickCheckSplit[0];
            if (subjectCategories.stream().noneMatch(subjectCategory::startsWith)) {
                return;
            }
            log.trace("[Exchange]Received {}", message);
            fun.accept($.json.toObject(quickCheckSplit[1], ExchangeData.class));
        }).onSuccess(response -> promise.complete());
        return promise.future();
    }

}
