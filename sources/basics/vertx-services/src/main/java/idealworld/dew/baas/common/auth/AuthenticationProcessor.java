package idealworld.dew.baas.common.auth;

import com.ecfront.dew.common.$;
import com.fasterxml.jackson.databind.JsonNode;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.AuthResultKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.baas.common.funs.cache.RedisClient;
import idealworld.dew.baas.common.util.AntPathMatcher;
import idealworld.dew.baas.common.util.URIHelper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 鉴权策略.
 * <p>
 * Redis格式：
 * <p>
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
        LocalResourceCache.loadRemoteResources(null);
    }

    public static Future<AuthResultKind> authentication(
            String actionKind,
            URI resourceUri,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        return authentication(actionKind,
                new ArrayList<>() {
                    {
                        add(resourceUri);
                    }
                },
                subjectInfo);
    }

    public static Future<AuthResultKind> authentication(
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
        doAuthentication(matchedResourceUris, actionKind, subjectInfo, promise);
        return promise.future();
    }

    private static void doAuthentication(
            List<String> matchedResourceUris,
            String actionKind,
            Map<AuthSubjectKind, List<String>> subjectInfo,
            Promise<AuthResultKind> promise
    ) {
        if (matchedResourceUris.isEmpty()) {
            promise.complete(AuthResultKind.REJECT);
            return;
        }
        var currentProcessUri = matchedResourceUris.get(0);
        RedisClient.choose("").get(Constant.CACHE_AUTH_POLICY
                + currentProcessUri.replace("//", "") + ":"
                + actionKind, resourceCacheExpireSec)
                .onSuccess(value -> {
                    var authInfo = $.json.toJson(value);
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
                    doAuthentication(matchedResourceUris, actionKind, subjectInfo, promise);
                })
                .onFailure(e -> {
                    log.error("[Auth]Resource fetch error: {}", e.getMessage(), e.getCause());
                    promise.complete(AuthResultKind.REJECT);
                });
    }

    private static Boolean matchBasic(AuthSubjectOperatorKind eqOrNeqKind, JsonNode authInfo, Map<AuthSubjectKind, List<String>> subjectInfo) {
        if (!authInfo.has(eqOrNeqKind.toString().toLowerCase())) {
            return false;
        }
        var json = authInfo.get(eqOrNeqKind.toString().toLowerCase());
        if (subjectInfo.containsKey(AuthSubjectKind.ACCOUNT)
                && json.has(AuthSubjectKind.ACCOUNT.toString().toLowerCase())) {
            var subjectJson = json.get(AuthSubjectKind.ACCOUNT.toString().toLowerCase());
            if (subjectInfo.get(AuthSubjectKind.ACCOUNT).stream()
                    .anyMatch(id -> $.fun.stream(subjectJson.iterator()).anyMatch(node -> node.asText().equals(id)))) {
                return true;
            }
        }
        if (subjectInfo.containsKey(AuthSubjectKind.GROUP_NODE)
                && json.has(AuthSubjectKind.GROUP_NODE.toString().toLowerCase())) {
            var subjectJson = json.get(AuthSubjectKind.GROUP_NODE.toString().toLowerCase());
            if (subjectInfo.get(AuthSubjectKind.GROUP_NODE).stream()
                    .anyMatch(id -> $.fun.stream(subjectJson.iterator()).anyMatch(node -> node.asText().equals(id)))) {
                return true;
            }
        }
        if (subjectInfo.containsKey(AuthSubjectKind.ROLE)
                && json.has(AuthSubjectKind.ROLE.toString().toLowerCase())) {
            var subjectJson = json.get(AuthSubjectKind.ROLE.toString().toLowerCase());
            if (subjectInfo.get(AuthSubjectKind.ROLE).stream()
                    .anyMatch(id -> $.fun.stream(subjectJson.iterator()).anyMatch(node -> node.asText().equals(id)))) {
                return true;
            }
        }
        if (subjectInfo.containsKey(AuthSubjectKind.APP)
                && json.has(AuthSubjectKind.APP.toString().toLowerCase())) {
            var subjectJson = json.get(AuthSubjectKind.APP.toString().toLowerCase());
            if (subjectInfo.get(AuthSubjectKind.APP).stream()
                    .anyMatch(id -> $.fun.stream(subjectJson.iterator()).anyMatch(node -> node.asText().equals(id)))) {
                return true;
            }
        }
        if (subjectInfo.containsKey(AuthSubjectKind.TENANT)
                && json.has(AuthSubjectKind.TENANT.toString().toLowerCase())) {
            var subjectJson = json.get(AuthSubjectKind.TENANT.toString().toLowerCase());
            return subjectInfo.get(AuthSubjectKind.TENANT).stream()
                    .anyMatch(id -> $.fun.stream(subjectJson.iterator()).anyMatch(node -> node.asText().equals(id)));
        }
        return false;
    }

    private static Boolean matchInclude(JsonNode authInfo, Map<AuthSubjectKind, List<String>> subjectInfo) {
        if (!authInfo.has(AuthSubjectOperatorKind.INCLUDE.toString().toLowerCase())) {
            return false;
        }
        var json = authInfo.get(AuthSubjectOperatorKind.INCLUDE.toString().toLowerCase());
        if (!subjectInfo.containsKey(AuthSubjectKind.GROUP_NODE)
                || !json.has(AuthSubjectKind.GROUP_NODE.toString().toLowerCase())) {
            return false;
        }
        var subjectJson = json.get(AuthSubjectKind.GROUP_NODE.toString().toLowerCase());
        var subjectIds = subjectInfo.get(AuthSubjectKind.GROUP_NODE);
        var jsonIterator = subjectJson.elements();
        while (jsonIterator.hasNext()) {
            var currentNodeCode = jsonIterator.next().asText();
            while (currentNodeCode.length() > 0) {
                if (subjectIds.contains(currentNodeCode)) {
                    return true;
                }
                currentNodeCode = currentNodeCode.substring(0, currentNodeCode.length() - groupNodeLength);
            }
        }
        return false;
    }

    private static Boolean matchLike(JsonNode authInfo, Map<AuthSubjectKind, List<String>> subjectInfo) {
        if (!authInfo.has(AuthSubjectOperatorKind.LIKE.toString().toLowerCase())) {
            return false;
        }
        var json = authInfo.get(AuthSubjectOperatorKind.LIKE.toString().toLowerCase());
        if (!subjectInfo.containsKey(AuthSubjectKind.GROUP_NODE)
                || !json.has(AuthSubjectKind.GROUP_NODE.toString().toLowerCase())) {
            return false;
        }
        var subjectIds = subjectInfo.get(AuthSubjectKind.GROUP_NODE);
        var subjectJson = json.get(AuthSubjectKind.GROUP_NODE.toString().toLowerCase());
        var jsonIterator = subjectJson.elements();
        while (jsonIterator.hasNext()) {
            var nodeCode = jsonIterator.next().asText();
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
                                && PATH_MATCHER.matchStart(uri.getPath(), resourceUri.getPath())
                )
                .sorted((uri1, uri2) -> comparator.compare(uri1.getPath(), uri2.getPath()))
                .map(URI::toString);
    }

}

