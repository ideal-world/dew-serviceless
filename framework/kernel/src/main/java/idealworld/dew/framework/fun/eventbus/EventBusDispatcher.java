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

import com.ecfront.dew.common.$;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.util.AntPathMatcher;
import idealworld.dew.framework.util.URIHelper;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gudaoxuri
 */
@Slf4j
public class EventBusDispatcher {

    private static final Map<String, Map<OptActionKind, Map<String, ProcessFun>>> PROCESSORS = new ConcurrentHashMap<>();
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static synchronized void initModule(String moduleName) {
        PROCESSORS.put(moduleName, new HashMap<>());
        PROCESSORS.get(moduleName).put(OptActionKind.CREATE, new ConcurrentHashMap<>());
        PROCESSORS.get(moduleName).put(OptActionKind.MODIFY, new ConcurrentHashMap<>());
        PROCESSORS.get(moduleName).put(OptActionKind.FETCH, new ConcurrentHashMap<>());
        PROCESSORS.get(moduleName).put(OptActionKind.EXISTS, new ConcurrentHashMap<>());
        PROCESSORS.get(moduleName).put(OptActionKind.PATCH, new ConcurrentHashMap<>());
        PROCESSORS.get(moduleName).put(OptActionKind.DELETE, new ConcurrentHashMap<>());
    }

    public static void addProcessor(String moduleName, String pathPattern, ProcessFun processFun) {
        PROCESSORS.get(moduleName).get(OptActionKind.CREATE).put(pathPattern, processFun);
        PROCESSORS.get(moduleName).get(OptActionKind.MODIFY).put(pathPattern, processFun);
        PROCESSORS.get(moduleName).get(OptActionKind.FETCH).put(pathPattern, processFun);
        PROCESSORS.get(moduleName).get(OptActionKind.EXISTS).put(pathPattern, processFun);
        PROCESSORS.get(moduleName).get(OptActionKind.PATCH).put(pathPattern, processFun);
        PROCESSORS.get(moduleName).get(OptActionKind.DELETE).put(pathPattern, processFun);
    }

    public static void addProcessor(String moduleName, OptActionKind actionKind, String pathPattern, ProcessFun processFun) {
        PROCESSORS.get(moduleName).get(actionKind).put(pathPattern, processFun);
    }

    public static <E> Future<E> chooseProcess(String moduleName, Object config, Map<String, Boolean> funStatus, OptActionKind actionKind, String pathRequest, String query, Map<String, String> header, Buffer body) {
        if (PROCESSORS.get(moduleName).get(actionKind).containsKey(pathRequest)) {
            return PROCESSORS.get(moduleName).get(actionKind).get(pathRequest).process(EventBusContext.builder()
                    .req(EventBusContext.Request.builder()
                            .header(header)
                            .params(URIHelper.getSingleValueQuery(query, false))
                            .body(body)
                            .identOptInfo(
                                    header.containsKey(DewConstant.REQUEST_IDENT_OPT_FLAG)
                                            ? new JsonObject($.security.decodeBase64ToString(header.get(DewConstant.REQUEST_IDENT_OPT_FLAG), StandardCharsets.UTF_8)).mapTo(IdentOptCacheInfo.class)
                                            : new IdentOptCacheInfo())
                            .build())
                    .context(ProcessContext.builder()
                            .conf(config)
                            .funStatus(funStatus)
                            .moduleName(moduleName)
                            .build())
                    .build()
                    .init());
        }
        var matchedPathTemplate = PROCESSORS.get(moduleName).get(actionKind).keySet()
                .stream()
                .filter(pathPattern -> PATH_MATCHER.match(pathPattern, pathRequest))
                .findAny();
        if (matchedPathTemplate.isEmpty()) {
            log.warn("[EventBus]Can't found process by [{}] {}", actionKind.toString(), pathRequest);
            throw new BadRequestException("请求[" + actionKind.toString() + ":" + pathRequest + "]找不到对应的处理器");
        } else {
            var pathPattern = matchedPathTemplate.get();
            var params = PATH_MATCHER.extractUriTemplateVariables(pathPattern, pathRequest);
            params.putAll(URIHelper.getSingleValueQuery(query, false));
            return PROCESSORS.get(moduleName).get(actionKind).get(pathPattern).process(EventBusContext.builder()
                    .req(EventBusContext.Request.builder()
                            .header(header)
                            .params(params)
                            .body(body)
                            .identOptInfo(
                                    header.containsKey(DewConstant.REQUEST_IDENT_OPT_FLAG)
                                            ? new JsonObject($.security.decodeBase64ToString(header.get(DewConstant.REQUEST_IDENT_OPT_FLAG), StandardCharsets.UTF_8)).mapTo(IdentOptCacheInfo.class)
                                            : new IdentOptCacheInfo())
                            .build())
                    .context(ProcessContext.builder()
                            .conf(config)
                            .funStatus(funStatus)
                            .moduleName(moduleName)
                            .build())
                    .build()
                    .init());
        }
    }

    public static Future<Void> watch(String moduleName, Object config, Map<String, Boolean> funStatus) {
        FunEventBus.choose(moduleName).consumer(moduleName, (actionKind, uri, header, body) ->
                chooseProcess(moduleName, config, funStatus, actionKind, uri.getPath(), uri.getQuery() != null ? URLDecoder.decode(uri.getQuery(), StandardCharsets.UTF_8) : null, header, body));
        return Future.succeededFuture();
    }

}
