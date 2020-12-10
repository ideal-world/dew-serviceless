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

package idealworld.dew.framework.fun.eventbus;

import com.ecfront.dew.common.Resp;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.cache.FunRedisClient;
import idealworld.dew.framework.fun.httpclient.FunHttpClient;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import idealworld.dew.framework.util.AntPathMatcher;
import idealworld.dew.framework.util.URIHelper;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ReceiveProcessor {

    private static final Map<OptActionKind, Map<String, ProcessFun>> PROCESSORS = new HashMap<>();
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static void addProcessor(OptActionKind actionKind, String pathPattern, ProcessFun processFun) {
        if (!PROCESSORS.containsKey(actionKind)) {
            PROCESSORS.put(actionKind, new HashMap<>());
        }
        PROCESSORS.get(actionKind).put(pathPattern, processFun);
    }

    public static Future<Resp<?>> chooseProcess(String moduleName, OptActionKind actionKind, String pathRequest, String query, Map<String, String> header, Buffer body) {
        if (!PROCESSORS.containsKey(actionKind)) {
            log.warn("[EventBus]Can't found process by [{}] {}", actionKind.toString(), pathRequest);
            return Future.succeededFuture(Resp.badRequest("找不到对应的处理器"));
        }
        if (PROCESSORS.get(actionKind).containsKey(pathRequest)) {
            return (Future<Resp<?>>) PROCESSORS.get(actionKind).get(pathRequest).process(ProcessContext.builder()
                    .req(ProcessContext.Request.builder()
                            .header(header)
                            .params(new HashMap<>())
                            .body(body)
                            .identOptInfo(
                                    header.containsKey(DewConstant.REQUEST_IDENT_OPT_FLAG)
                                            ? new JsonObject(header.get(DewConstant.REQUEST_IDENT_OPT_FLAG)).mapTo(IdentOptInfo.class)
                                            : new IdentOptInfo())
                            .build())
                    .fun(ProcessContext.Function.builder()
                            .sql(FunSQLClient.contains(moduleName) ? FunSQLClient.choose(moduleName) : null)
                            .redis(FunRedisClient.contains(moduleName) ? FunRedisClient.choose(moduleName) : null)
                            .http(FunHttpClient.contains(moduleName) ? FunHttpClient.choose(moduleName) : null)
                            .build())
                    .build());
        }
        var matchedPathTemplate = PROCESSORS.get(actionKind).keySet()
                .stream()
                .filter(pathPattern -> PATH_MATCHER.match(pathPattern, pathRequest))
                .findAny();
        if (matchedPathTemplate.isEmpty()) {
            log.warn("[EventBus]Can't found process by [{}] {}", actionKind.toString(), pathRequest);
            return Future.succeededFuture(Resp.badRequest("找不到对应的处理器"));
        } else {
            var pathPattern = matchedPathTemplate.get();
            var params = PATH_MATCHER.extractUriTemplateVariables(pathPattern, pathRequest);
            params.putAll(URIHelper.getSingleValueQuery(query));
            return (Future<Resp<?>>) PROCESSORS.get(actionKind).get(pathPattern).process(ProcessContext.builder()
                    .req(ProcessContext.Request.builder()
                            .header(header)
                            .params(params)
                            .body(body)
                            .identOptInfo(
                                    header.containsKey(DewConstant.REQUEST_IDENT_OPT_FLAG)
                                            ? new JsonObject(header.get(DewConstant.REQUEST_IDENT_OPT_FLAG)).mapTo(IdentOptInfo.class)
                                            : new IdentOptInfo())
                            .build())
                    .fun(ProcessContext.Function.builder()
                            .sql(FunSQLClient.contains(moduleName) ? FunSQLClient.choose(moduleName) : null)
                            .redis(FunRedisClient.contains(moduleName) ? FunRedisClient.choose(moduleName) : null)
                            .http(FunHttpClient.contains(moduleName) ? FunHttpClient.choose(moduleName) : null)
                            .build())
                    .build());
        }
    }

    public static Future<Void> watch(String moduleName) {
        FunEventBus.choose(moduleName).consumer(moduleName, (actionKind, uri, header, body) ->
                chooseProcess(moduleName, actionKind, uri.getPath(), uri.getQuery(), header, body));
        return Future.succeededFuture();
    }

}
