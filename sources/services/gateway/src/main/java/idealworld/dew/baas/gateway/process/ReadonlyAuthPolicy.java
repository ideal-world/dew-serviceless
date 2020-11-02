package idealworld.dew.baas.gateway.process;

import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.exception.RTException;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.AuthActionKind;
import idealworld.dew.baas.common.enumeration.AuthResultKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.baas.common.util.AntPathMatcher;
import idealworld.dew.baas.common.util.URIHelper;
import idealworld.dew.baas.gateway.GatewayConfig;
import idealworld.dew.baas.gateway.util.CachedRedisClient;
import io.vertx.core.Future;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 鉴权策略.
 * <p>
 * Redis格式：
 * <p>
 * 资源类型:资源URI:资源操作类型:权限主体运算类型:权限主体类型:权限主体Id = 权限结果类型|是否排他|<INCLUDE时加上来源权限主体Id>
 *
 * @author gudaoxuri
 */
@Slf4j
public class ReadonlyAuthPolicy {

    // resourceKind -> actionKind -> uris
    private static final Map<String, Map<String, List<URI>>> LOCAL_RESOURCES = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final GatewayConfig.Security security;

    public ReadonlyAuthPolicy(GatewayConfig.Security security) {
        this.security = security;
        CachedRedisClient.scan(Constant.CACHE_AUTH_POLICY, key -> {
            var keyItems = key.substring(Constant.CACHE_AUTH_POLICY.length()).split(":");
            var resourceKind = keyItems[0];
            var resourceUri = keyItems[1];
            var actionKind = keyItems[2];
            if (!LOCAL_RESOURCES.containsKey(resourceKind)) {
                LOCAL_RESOURCES.put(resourceKind, new ConcurrentHashMap<>());
            }
            if (!LOCAL_RESOURCES.get(resourceKind).containsKey(actionKind)) {
                LOCAL_RESOURCES.get(resourceKind).put(actionKind, new CopyOnWriteArrayList<>());
            }
            try {
                LOCAL_RESOURCES.get(resourceKind).get(actionKind).add(new URI(resourceUri));
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
        return Future.future(promise -> {
            var matchedResourceUris = matchResourceUris(formattedResourceUri, actionKind);
            if (matchedResourceUris.isEmpty()) {
                // 资源不需要鉴权
                promise.complete(AuthResultKind.ACCEPT);
                return;
            }
            if (subjectInfo.isEmpty()) {
                // 资源需要鉴权但没有对应的权限主体
                promise.complete(AuthResultKind.ACCEPT);
                return;
            }
            doAuthentication(formattedResourceUri.getScheme().toUpperCase(), matchedResourceUris, actionKind, subjectInfo)
                    .onSuccess(promise::complete);
        });
    }

    private Future<AuthResultKind> doAuthentication(
            String resourceKind,
            List<String> matchedResourceUris,
            String actionKind,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        return Future.future(promise -> {
            if (matchedResourceUris.isEmpty()) {
                promise.complete(AuthResultKind.REJECT);
            }
            AtomicReference<AuthResultKind> authResultKind = new AtomicReference<>(AuthResultKind.REJECT);
            var resourceUri = matchedResourceUris.remove(0);
            doAuthentication(resourceKind, resourceUri, actionKind, subjectInfo)
                    .onSuccess(authResultOpt -> {
                        if (authResultOpt.isPresent()) {
                            if (authResultOpt.get()._1) {
                                promise.complete(authResultOpt.get()._0);
                                return;
                            }
                            authResultKind.set(authResultOpt.get()._0);
                            doAuthentication(resourceKind, matchedResourceUris, actionKind, subjectInfo)
                                    .onSuccess(promise::complete);
                        }
                    });
        });
    }

    private Future<Optional<Tuple2<AuthResultKind, Boolean>>> doAuthentication(
            String resourceKind,
            String resourceUri,
            String actionKind,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        return Future.future(promise -> {
            AtomicReference<AuthResultKind> authResultKind = new AtomicReference<>(null);
            for (var subject : subjectInfo.entrySet()) {
                for (var subjectId : subject.getValue()) {
                    var key = Constant.CACHE_AUTH_POLICY
                            + resourceKind + ":"
                            + resourceUri + ":"
                            + actionKind;
                    matchEQ(key, subject.getKey(), subjectId)
                            .onSuccess(matchEQResultOpt -> {
                                if (matchEQResultOpt.isPresent()) {
                                    if (matchEQResultOpt.get()._1) {
                                        promise.complete(matchEQResultOpt);
                                        return;
                                    }
                                    authResultKind.set(matchEQResultOpt.get()._0);
                                }
                                matchInclude(key, subject.getKey(), subjectId)
                                        .onSuccess(matchIncludeResultOpt -> {
                                            if (matchIncludeResultOpt.isPresent()) {
                                                if (matchIncludeResultOpt.get()._1) {
                                                    promise.complete(matchIncludeResultOpt);
                                                    return;
                                                }
                                                authResultKind.set(matchIncludeResultOpt.get()._0);
                                            }
                                            matchLike(key, subject.getKey(), subjectId)
                                                    .onSuccess(matchLikeResultOpt -> {
                                                        if (matchLikeResultOpt.isPresent()) {
                                                            if (matchLikeResultOpt.get()._1) {
                                                                promise.complete(matchLikeResultOpt);
                                                                return;
                                                            }
                                                            authResultKind.set(matchLikeResultOpt.get()._0);
                                                            promise.complete(
                                                                    authResultKind.get() == null
                                                                            ? Optional.empty()
                                                                            : Optional.of(new Tuple2<>(authResultKind.get(), false)));
                                                        }
                                                    });
                                        });


                            });
                }
            }
        });
    }

    private Future<Optional<Tuple2<AuthResultKind, Boolean>>> matchEQ(String keyPrefix, AuthSubjectKind subjectKind, String subjectId) {
        return parseAuth(keyPrefix + AuthSubjectOperatorKind.EQ.toString() + ":" + subjectKind.toString() + ":" + subjectId);
    }

    private Future<Optional<Tuple2<AuthResultKind, Boolean>>> matchInclude(String keyPrefix, AuthSubjectKind subjectKind, String subjectId) {
        // 在添加时已经为上级节点添加了key，所以这里只要发起一次查询即可
        return parseAuth(keyPrefix + AuthSubjectOperatorKind.INCLUDE.toString() + ":" + subjectKind.toString() + ":" + subjectId);
    }


    private Future<Optional<Tuple2<AuthResultKind, Boolean>>> matchLike(String keyPrefix, AuthSubjectKind subjectKind, String subjectId) {
        return doMatchLike(keyPrefix, subjectKind, subjectId, null);
    }

    private Future<Optional<Tuple2<AuthResultKind, Boolean>>> doMatchLike(String keyPrefix, AuthSubjectKind subjectKind,
                                                                          String groupNodeId, AuthResultKind authResultKind) {
        return Future.future(promise -> {
            if (groupNodeId.length() <= 0) {
                promise.complete(authResultKind == null ? Optional.empty() : Optional.of(new Tuple2<>(authResultKind, false)));
                return;
            }
            parseAuth(keyPrefix + AuthSubjectOperatorKind.LIKE.toString() + ":" + subjectKind.toString() + ":" + groupNodeId)
                    .onSuccess(authResultOpt -> {
                        if (authResultOpt.isPresent()) {
                            if (authResultOpt.get()._1) {
                                promise.complete(authResultOpt);
                                return;
                            }
                            var newGroupNodeId = groupNodeId.substring(0, groupNodeId.length() - security.getGroupNodeLength());
                            doMatchLike(keyPrefix, subjectKind, newGroupNodeId, authResultOpt.get()._0);
                        }
                    });
        });
    }

    private Future<Optional<Tuple2<AuthResultKind, Boolean>>> parseAuth(String key) {
        return Future.future(promise ->
                CachedRedisClient.get(key, security.getResourceCacheExpireSec())
                        .onSuccess(cache -> {
                            if (cache == null) {
                                promise.complete(Optional.empty());
                                return;
                            }
                            var cacheValue = cache.split("\\|");
                            var authResultKind = AuthResultKind.parse(cacheValue[0]);
                            var exclusive = Boolean.parseBoolean(cacheValue[1]);
                            promise.complete(Optional.of(new Tuple2<>(authResultKind, exclusive)));
                        }));
    }

    private List<String> matchResourceUris(URI resourceUri, String actionKind) {
        var resourceKind = resourceUri.getScheme();
        var matchResourceUris = LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>())
                .getOrDefault(actionKind, new ArrayList<>())
                .stream()
                .filter(uri ->
                        uri.getHost().equalsIgnoreCase(resourceUri.getHost())
                                && uri.getPort() == resourceUri.getPort()
                                && uri.getQuery().equalsIgnoreCase(resourceUri.getQuery())
                                && !uri.getPath().equalsIgnoreCase(resourceUri.getPath())
                                && pathMatcher.matchStart(uri.getPath(), resourceUri.getPath())
                )
                .map(URI::toString)
                .collect(Collectors.toList());
        if (LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>())
                .getOrDefault(actionKind, new ArrayList<>())
                .contains(resourceUri)) {
            matchResourceUris.add(0, resourceUri.toString());
        }
        return matchResourceUris;
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

