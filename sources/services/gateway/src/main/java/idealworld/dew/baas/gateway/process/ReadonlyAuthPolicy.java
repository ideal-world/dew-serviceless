package idealworld.dew.baas.gateway.process;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.exception.RTException;
import com.fasterxml.jackson.databind.JsonNode;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.AuthActionKind;
import idealworld.dew.baas.common.enumeration.AuthResultKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.baas.common.util.AntPathMatcher;
import idealworld.dew.baas.common.util.URIHelper;
import idealworld.dew.baas.gateway.util.RedisClient;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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
public class ReadonlyAuthPolicy {

    // resourceKind -> actionKind -> uris
    private static final Map<String, Map<String, List<URI>>> LOCAL_RESOURCES = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final Integer resourceCacheExpireSec;
    private final Integer groupNodeLength;

    public ReadonlyAuthPolicy(Integer resourceCacheExpireSec, Integer groupNodeLength) {
        this.resourceCacheExpireSec = resourceCacheExpireSec;
        this.groupNodeLength = groupNodeLength;
        RedisClient.scan(Constant.CACHE_AUTH_POLICY, key -> {
            var keyItems = key.substring(Constant.CACHE_AUTH_POLICY.length()).split(":");
            var resourceKind = keyItems[0];
            var resourceUri = resourceKind + "://" + keyItems[1];
            var actionKind = keyItems[2];
            if (!LOCAL_RESOURCES.containsKey(resourceKind)) {
                LOCAL_RESOURCES.put(resourceKind, new ConcurrentHashMap<>());
            }
            if (!LOCAL_RESOURCES.get(resourceKind).containsKey(actionKind)) {
                LOCAL_RESOURCES.get(resourceKind).put(actionKind, new CopyOnWriteArrayList<>());
            }
            try {
                if (!LOCAL_RESOURCES.get(resourceKind).get(actionKind).contains(new URI(resourceUri))) {
                    LOCAL_RESOURCES.get(resourceKind).get(actionKind).add(new URI(resourceUri));
                }
            } catch (URISyntaxException e) {
                log.error("Init Auth policy error:{}", e.getMessage(), e);
                throw new RTException(e);
            }
        });
    }

    @SneakyThrows
    public Future<AuthResultKind> authentication(
            URI resourceUri,
            String actionKind,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        var formattedResourceUri = new URI(URIHelper.formatUri(resourceUri));
        var matchedResourceUris = matchResourceUris(formattedResourceUri, actionKind);
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

    public void doAuthentication(
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
        RedisClient.get(Constant.CACHE_AUTH_POLICY
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
                    matchResult = matchBasic(AuthSubjectOperatorKind.INCLUDE, authInfo, subjectInfo);
                    if (matchResult) {
                        promise.complete(AuthResultKind.ACCEPT);
                        return;
                    }
                    matchResult = matchBasic(AuthSubjectOperatorKind.LIKE, authInfo, subjectInfo);
                    if (matchResult) {
                        promise.complete(AuthResultKind.ACCEPT);
                        return;
                    }
                    matchedResourceUris.remove(0);
                    doAuthentication(matchedResourceUris, actionKind, subjectInfo, promise);
                });
    }

    private Boolean matchBasic(AuthSubjectOperatorKind eqOrNeqKind, JsonNode authInfo, Map<AuthSubjectKind, List<String>> subjectInfo) {
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

    private Boolean matchInclude(JsonNode authInfo, Map<AuthSubjectKind, List<String>> subjectInfo) {
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

    private Boolean matchLike(JsonNode authInfo, Map<AuthSubjectKind, List<String>> subjectInfo) {
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

    private List<String> matchResourceUris(URI resourceUri, String actionKind) {
        var resourceKind = resourceUri.getScheme();
        if (LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>())
                .getOrDefault(actionKind, new ArrayList<>())
                .contains(resourceUri)) {
            // 添加精确匹配
            return new ArrayList<>() {
                {
                    add(resourceUri.toString());
                }
            };
        }
        var comparator = pathMatcher.getPatternComparator(resourceUri.getPath());
        return LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>())
                .getOrDefault(actionKind, new ArrayList<>())
                .stream()
                .filter(uri ->
                        uri.getHost().equalsIgnoreCase(resourceUri.getHost())
                                && uri.getPort() == resourceUri.getPort()
                                && !uri.getPath().equalsIgnoreCase(resourceUri.getPath())
                                && (uri.getQuery() == null && resourceUri.getQuery() == null
                                || uri.getQuery().equalsIgnoreCase(resourceUri.getQuery()))
                                && pathMatcher.matchStart(uri.getPath(), resourceUri.getPath())
                )
                .sorted((uri1, uri2) -> comparator.compare(uri1.getPath(), uri2.getPath()))
                .map(URI::toString)
                .collect(Collectors.toList());
    }

    public Resp<Void> addLocalResource(URI resourceUri, String actionKind) {
        var resourceKind = resourceUri.getScheme();
        if (!LOCAL_RESOURCES.containsKey(resourceKind)) {
            LOCAL_RESOURCES.put(resourceKind, new ConcurrentHashMap<>());
        }
        if (!LOCAL_RESOURCES.get(resourceKind).containsKey(actionKind)) {
            LOCAL_RESOURCES.get(resourceKind).put(actionKind, new CopyOnWriteArrayList<>());
        }
        LOCAL_RESOURCES.get(resourceKind).get(actionKind).add(resourceUri);
        return Resp.success(null);
    }

    public Resp<Void> removeLocalResource(URI resourceUri, String actionKind) {
        LOCAL_RESOURCES.getOrDefault(resourceUri.getScheme(), new HashMap<>()).getOrDefault(actionKind, new ArrayList<>()).remove(resourceUri);
        return Resp.success(null);
    }

    public Resp<Void> removeLocalResource(URI resourceUri) {
        var resourceKind = resourceUri.getScheme();
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(AuthActionKind.CREATE.toString(), new ArrayList<>()).remove(resourceUri);
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(AuthActionKind.DELETE.toString(), new ArrayList<>()).remove(resourceUri);
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(AuthActionKind.FETCH.toString(), new ArrayList<>()).remove(resourceUri);
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(AuthActionKind.MODIFY.toString(), new ArrayList<>()).remove(resourceUri);
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(AuthActionKind.PATCH.toString(), new ArrayList<>()).remove(resourceUri);
        return Resp.success(null);
    }

}

