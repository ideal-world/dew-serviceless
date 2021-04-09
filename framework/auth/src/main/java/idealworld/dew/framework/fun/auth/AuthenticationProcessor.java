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
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.fun.auth.dto.AuthResultKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectOperatorKind;
import idealworld.dew.framework.fun.cache.FunCacheClient;
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

    public static void init(Integer resourceCacheExpireSec, Integer groupNodeLength) {
        AuthenticationProcessor.resourceCacheExpireSec = resourceCacheExpireSec;
        AuthenticationProcessor.groupNodeLength = groupNodeLength;
    }

    public static Future<AuthResultKind> authentication(String moduleName, String actionKind, URI resourceUri, IdentOptExchangeInfo identOptInfo) {
        return authentication(moduleName, actionKind,
                new ArrayList<>() {
                    {
                        add(resourceUri);
                    }
                },
                identOptInfo);
    }

    public static Future<AuthResultKind> authentication(String moduleName, String actionKind, List<URI> resourceUris, IdentOptExchangeInfo identOptInfo) {
        var matchedResourceUris = resourceUris.stream()
                .flatMap(resourceUri -> matchResourceUris(resourceUri, actionKind))
                .collect(Collectors.toList());
        if (matchedResourceUris.isEmpty()) {
            // 资源不需要鉴权
            return Future.succeededFuture(AuthResultKind.ACCEPT);
        }
        var subjectInfo = packageSubjectInfo(identOptInfo);
        if (subjectInfo.isEmpty()) {
            // 资源需要鉴权但没有对应的权限主体
            return Future.succeededFuture(AuthResultKind.REJECT);
        }
        Promise<AuthResultKind> promise = Promise.promise();
        doAuthentication(moduleName, matchedResourceUris, actionKind, subjectInfo, promise);
        return promise.future();
    }

    private static Map<AuthSubjectKind, List<String>> packageSubjectInfo(IdentOptExchangeInfo identOptInfo) {
        var subjectInfo = new LinkedHashMap<AuthSubjectKind, List<String>>();
        if (identOptInfo == null) {
            return subjectInfo;
        }
        if (identOptInfo.getAccountId() != null && identOptInfo.getAccountId() != DewConstant.OBJECT_UNDEFINED) {
            subjectInfo.put(AuthSubjectKind.ACCOUNT, new ArrayList<>() {
                {
                    add(identOptInfo.getAccountId() + "");
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
                    .map(r -> r.getId() + "")
                    .collect(Collectors.toList()));
        }
        if (identOptInfo.getUnauthorizedAppId() != null && identOptInfo.getUnauthorizedAppId() != DewConstant.OBJECT_UNDEFINED) {
            subjectInfo.put(AuthSubjectKind.APP, new ArrayList<>() {
                {
                    add(identOptInfo.getUnauthorizedAppId().toString());
                }
            });
        }
        if (identOptInfo.getUnauthorizedTenantId() != null && identOptInfo.getUnauthorizedTenantId() != DewConstant.OBJECT_UNDEFINED) {
            subjectInfo.put(AuthSubjectKind.TENANT, new ArrayList<>() {
                {
                    add(identOptInfo.getUnauthorizedTenantId().toString());
                }
            });
        }
        return subjectInfo;
    }

    private static void doAuthentication(String moduleName, List<String> matchedResourceUris, String actionKind,
                                         Map<AuthSubjectKind, List<String>> subjectInfo, Promise<AuthResultKind> promise) {
        var currentProcessUri = matchedResourceUris.remove(0);
        FunCacheClient.choose(moduleName).get(DewAuthConstant.CACHE_AUTH_POLICY
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
            var currentNodeWithGroup = ((String) item).split(DewAuthConstant.GROUP_CODE_NODE_CODE_SPLIT);
            var currentGroupCode = currentNodeWithGroup[0];
            var currentNodeCode = currentNodeWithGroup[1];
            var subjectIdsWithoutGroupCode = subjectIds.stream()
                    .filter(id -> id.startsWith(currentGroupCode + DewAuthConstant.GROUP_CODE_NODE_CODE_SPLIT))
                    .map(id -> id.split(DewAuthConstant.GROUP_CODE_NODE_CODE_SPLIT)[1])
                    .collect(Collectors.toList());
            if (subjectIdsWithoutGroupCode.isEmpty()) {
                continue;
            }
            while (currentNodeCode.length() > 0) {
                if (subjectIdsWithoutGroupCode.contains(currentNodeCode)) {
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
                                && (uri.getRawQuery() == null
                                || resourceUri.getRawQuery() != null
                                && uri.getRawQuery().equalsIgnoreCase(resourceUri.getRawQuery()))
                                && PATH_MATCHER.match(uri.getPath().toLowerCase(), resourceUri.getPath().toLowerCase())
                )
                .sorted((uri1, uri2) -> comparator.compare(uri1.getPath(), uri2.getPath()))
                .map(URI::toString);
    }

}

