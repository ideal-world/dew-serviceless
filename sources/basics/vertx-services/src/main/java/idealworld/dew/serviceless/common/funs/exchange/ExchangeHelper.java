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
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.exchange.ExchangeData;
import idealworld.dew.serviceless.common.funs.cache.RedisClient;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.function.Consumer;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeHelper {

    private static final String QUICK_CHECK_SPLIT = "#";

    public static Future<Void> register(Set<String> subjectCategories, Consumer<ExchangeData> fun) {
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