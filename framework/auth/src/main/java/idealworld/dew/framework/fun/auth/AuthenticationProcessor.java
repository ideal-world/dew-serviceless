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

package idealworld.dew.framework.fun.auth;

import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.fun.auth.dto.AuthResultKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectOperatorKind;
import idealworld.dew.framework.fun.cache.FunRedisClient;
import idealworld.dew.framework.util.AntPathMatcher;
import idealworld.dew.framework.util.URIHelper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 鉴权策略.
 * <p>
 * Redis格式：
 * <p>
 * // TODO 加上过期时间
 * 资源类型:资源URI:资源操作类型 = {权限主体运算类型:{权限主体类型:[权限主体Id]}}
 *
 * @author gudaoxuri
 */
@Slf4j
public class AuthenticationProcessor {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static Integer resourceCacheExpireSec;
    private static Integer groupNodeLength;

    public static void init(String moduleName, Integer resourceCacheExpireSec, Integer groupNodeLength) {
        AuthenticationProcessor.resourceCacheExpireSec = resourceCacheExpireSec;
        AuthenticationProcessor.groupNodeLength = groupNodeLength;
        LocalResourceCache.loadRemoteResources(moduleName, null);
    }

    public static Map<AuthSubjectKind, List<String>> packageSubjectInfo(IdentOptCacheInfo identOptInfo) {
        var subjectInfo = new LinkedHashMap<AuthSubjectKind, List<String>>();
        if (identOptInfo != null) {
            if (identOptInfo.getAccountCode() != null) {
                subjectInfo.put(AuthSubjectKind.ACCOUNT, new ArrayList<>() {
                    {
                        add(identOptInfo.getAccountCode().toString());
                    }
                });
            }
            if (identOptInfo.getGroupInfo() != null && !identOptInfo.getGroupInfo().isEmpty()) {
                subjectInfo.put(AuthSubjectKind.GROUP_NODE, identOptInfo.getGroupInfo().stream()
                        .map(group -> group.getGroupCode() + DewAuthConstant.GROUP_CODE_NODE_CODE_SPLIT + group.getGroupNodeCode())
                        .collect(Collectors.toList()));
            }
            if (identOptInfo.getRoleInfo() != null && !identOptInfo.getRoleInfo().isEmpty()) {
                subjectInfo.put(AuthSubjectKind.ROLE, identOptInfo.getRoleInfo().stream()
                        .map(IdentOptCacheInfo.RoleInfo::getCode)
                        .collect(Collectors.toList()));
            }
            if (identOptInfo.getAppId() != null) {
                subjectInfo.put(AuthSubjectKind.APP, new ArrayList<>() {
                    {
                        add(identOptInfo.getAppId().toString());
                    }
                });
            }
            if (identOptInfo.getTenantId() != null) {
                subjectInfo.put(AuthSubjectKind.TENANT, new ArrayList<>() {
                    {
                        add(identOptInfo.getTenantId().toString());
                    }
                });
            }
        }
        return subjectInfo;
    }

    public static Future<AuthResultKind> authentication(
            String moduleName,
            String actionKind,
            URI resourceUri,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        return authentication(moduleName, actionKind,
                new ArrayList<>() {
                    {
                        add(resourceUri);
                    }
                },
                subjectInfo);
    }

    public static Future<AuthResultKind> authentication(
            String moduleName,
            String actionKind,
            List<URI> resourceUris,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        var matchedResourceUris = resourceUris.stream()
                .flatMap(resourceUri -> matchResourceUris(resourceUri, actionKind))
                .collect(Collectors.toList());
        if (matchedResourceUris.isEmpty()) {
            // 资源不需要鉴权
            return Future.succeededFuture(AuthResultKind.ACCEPT);
        }
        if (subjectInfo.isEmpty()) {
            // 资源需要鉴权但没有对应的权限主体
            return Future.succeededFuture(AuthResultKind.REJECT);
        }
        Promise<AuthResultKind> promise = Promise.promise();
        doAuthentication(moduleName, matchedResourceUris, actionKind, subjectInfo, promise);
        return promise.future();
    }

    private static void doAuthentication(
            String moduleName,
            List<String> matchedResourceUris,
            String actionKind,
            Map<AuthSubjectKind, List<String>> subjectInfo,
            Promise<AuthResultKind> promise
    ) {
        var currentProcessUri = matchedResourceUris.get(0);
        FunRedisClient.choose(moduleName).get(DewAuthConstant.CACHE_AUTH_POLICY
                + currentProcessUri.replace("//", "") + ":"
                + actionKind, resourceCacheExpireSec)
                .onSuccess(value -> {
                    var authInfo = new JsonObject(value);
                    var matchResult = matchBasic(AuthSubjectOperatorKind.EQ, authInfo, subjectInfo);
                    if (matchResult) {
                        promise.complete(AuthResultKind.ACCEPT);
                        return;
                    }
                    matchResult = matchBasic(AuthSubjectOperatorKind.NEQ, authInfo, subjectInfo);
                    if (matchResult) {
                        promise.complete(AuthResultKind.REJECT);
                        return;
                    }
                    matchResult = matchInclude(authInfo, subjectInfo);
                    if (matchResult) {
                        promise.complete(AuthResultKind.ACCEPT);
                        return;
                    }
                    matchResult = matchLike(authInfo, subjectInfo);
                    if (matchResult) {
                        promise.complete(AuthResultKind.ACCEPT);
                        return;
                    }
                    matchedResourceUris.remove(0);
                    if (!matchedResourceUris.isEmpty()) {
                        doAuthentication(moduleName, matchedResourceUris, actionKind, subjectInfo, promise);
                        return;
                    }
                    promise.complete(AuthResultKind.REJECT);
                })
                .onFailure(e -> {
                    log.error("[Auth]Resource fetch error: {}", e.getMessage(), e.getCause());
                    promise.complete(AuthResultKind.REJECT);
                });
    }

    private static Boolean matchBasic(AuthSubjectOperatorKind eqOrNeqKind, JsonObject authInfo, Map<AuthSubjectKind, List<String>> subjectInfo) {
        if (!authInfo.containsKey(eqOrNeqKind.toString().toLowerCase())) {
            return false;
        }
        var json = authInfo.getJsonObject(eqOrNeqKind.toString().toLowerCase());
        if (subjectInfo.containsKey(AuthSubjectKind.ACCOUNT)
                && json.containsKey(AuthSubjectKind.ACCOUNT.toString().toLowerCase())) {
            var subjectJson = json.getJsonArray(AuthSubjectKind.ACCOUNT.toString().toLowerCase());
            if (subjectInfo.get(AuthSubjectKind.ACCOUNT).stream().anyMatch(subjectJson::contains)) {
                return true;
            }
        }
        if (subjectInfo.containsKey(AuthSubjectKind.GROUP_NODE)
                && json.containsKey(AuthSubjectKind.GROUP_NODE.toString().toLowerCase())) {
            var subjectJson = json.getJsonArray(AuthSubjectKind.GROUP_NODE.toString().toLowerCase());
            if (subjectInfo.get(AuthSubjectKind.GROUP_NODE).stream().anyMatch(subjectJson::contains)) {
                return true;
            }
        }
        if (subjectInfo.containsKey(AuthSubjectKind.ROLE)
                && json.containsKey(AuthSubjectKind.ROLE.toString().toLowerCase())) {
            var subjectJson = json.getJsonArray(AuthSubjectKind.ROLE.toString().toLowerCase());
            if (subjectInfo.get(AuthSubjectKind.ROLE).stream().anyMatch(subjectJson::contains)) {
                return true;
            }
        }
        if (subjectInfo.containsKey(AuthSubjectKind.APP)
                && json.containsKey(AuthSubjectKind.APP.toString().toLowerCase())) {
            var subjectJson = json.getJsonArray(AuthSubjectKind.APP.toString().toLowerCase());
            if (subjectInfo.get(AuthSubjectKind.APP).stream().anyMatch(subjectJson::contains)) {
                return true;
            }
        }
        if (subjectInfo.containsKey(AuthSubjectKind.TENANT)
                && json.containsKey(AuthSubjectKind.TENANT.toString().toLowerCase())) {
            var subjectJson = json.getJsonArray(AuthSubjectKind.TENANT.toString().toLowerCase());
            return subjectInfo.get(AuthSubjectKind.TENANT).stream().anyMatch(subjectJson::contains);
        }
        return false;
    }

    private static Boolean matchInclude(JsonObject authInfo, Map<AuthSubjectKind, List<String>> subjectInfo) {
        if (!authInfo.containsKey(AuthSubjectOperatorKind.INCLUDE.toString().toLowerCase())) {
            return false;
        }
        var json = authInfo.getJsonObject(AuthSubjectOperatorKind.INCLUDE.toString().toLowerCase());
        if (!subjectInfo.containsKey(AuthSubjectKind.GROUP_NODE)
                || !json.containsKey(AuthSubjectKind.GROUP_NODE.toString().toLowerCase())) {
            return false;
        }
        var subjectJson = json.getJsonArray(AuthSubjectKind.GROUP_NODE.toString().toLowerCase());
        var subjectIds = subjectInfo.get(AuthSubjectKind.GROUP_NODE);
        for (var item : subjectJson) {
            var currentNodeCode = (String) item;
            while (currentNodeCode.length() > 0) {
                if (subjectIds.contains(currentNodeCode)) {
                    return true;
                }
                currentNodeCode = currentNodeCode.substring(0, currentNodeCode.length() - groupNodeLength);
            }
        }
        return false;
    }

    private static Boolean matchLike(JsonObject authInfo, Map<AuthSubjectKind, List<String>> subjectInfo) {
        if (!authInfo.containsKey(AuthSubjectOperatorKind.LIKE.toString().toLowerCase())) {
            return false;
        }
        var json = authInfo.getJsonObject(AuthSubjectOperatorKind.LIKE.toString().toLowerCase());
        if (!subjectInfo.containsKey(AuthSubjectKind.GROUP_NODE)
                || !json.containsKey(AuthSubjectKind.GROUP_NODE.toString().toLowerCase())) {
            return false;
        }
        var subjectIds = subjectInfo.get(AuthSubjectKind.GROUP_NODE);
        var subjectJson = json.getJsonArray(AuthSubjectKind.GROUP_NODE.toString().toLowerCase());
        for (var item : subjectJson) {
            var nodeCode = (String) item;
            for (var subjectId : subjectIds) {
                if (subjectId.startsWith(nodeCode)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Stream<String> matchResourceUris(URI resUri, String actionKind) {
        var resourceUri = URIHelper.newURI(URIHelper.formatUri(resUri));
        var resourceKind = resourceUri.getScheme();
        if (LocalResourceCache.getResourceInfo(resourceKind)
                .getOrDefault(actionKind, new ArrayList<>())
                .contains(resourceUri)) {
            // 添加精确匹配
            return Stream.of(resourceUri.toString());
        }
        var comparator = PATH_MATCHER.getPatternComparator(resourceUri.getPath());
        return LocalResourceCache.getResourceInfo(resourceKind)
                .getOrDefault(actionKind, new ArrayList<>())
                .stream()
                .filter(uri ->
                        uri.getHost().equalsIgnoreCase(resourceUri.getHost())
                                && uri.getPort() == resourceUri.getPort()
                                && !uri.getPath().equalsIgnoreCase(resourceUri.getPath())
                                && (uri.getQuery() == null && resourceUri.getQuery() == null
                                || uri.getQuery().equalsIgnoreCase(resourceUri.getQuery()))
                                && PATH_MATCHER.match(uri.getPath(), resourceUri.getPath())
                )
                .sorted((uri1, uri2) -> comparator.compare(uri1.getPath(), uri2.getPath()))
                .map(URI::toString);
    }

}

