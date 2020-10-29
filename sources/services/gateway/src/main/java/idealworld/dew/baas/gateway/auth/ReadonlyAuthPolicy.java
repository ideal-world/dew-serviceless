package idealworld.dew.baas.gateway.auth;

import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.*;
import idealworld.dew.baas.common.resp.StandardResp;
import io.vertx.core.Future;
import io.vertx.redis.client.RedisAPI;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
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

    public static final String GROUP_CODE_NODE_CODE_SPLIT = "#";

    private static final String BUSINESS_AUTH = "AUTH";
    private static final Map<ResourceKind, Map<AuthActionKind, List<URI>>> LOCAL_RESOURCES = new ConcurrentHashMap<>();
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private Integer groupNodeLength;
    private RedisAPI redisClient;

    @SneakyThrows
    public void init(RedisAPI redisClient, Integer groupNodeLength) {
        this.groupNodeLength = groupNodeLength;
        this.redisClient = redisClient;
        scan(Constant.CACHE_AUTH_POLICY, key -> {
            var keyItems = key.substring(Constant.CACHE_AUTH_POLICY.length()).split(":");
            var resourceKind = ResourceKind.parse(keyItems[0]);
            var resourceUri = keyItems[1];
            var actionKind = AuthActionKind.parse(keyItems[2]);
            if (!LOCAL_RESOURCES.containsKey(resourceKind)) {
                LOCAL_RESOURCES.put(resourceKind, new ConcurrentHashMap<>());
            }
            if (!LOCAL_RESOURCES.get(resourceKind).containsKey(actionKind)) {
                LOCAL_RESOURCES.get(resourceKind).put(actionKind, new CopyOnWriteArrayList<>());
            }
            LOCAL_RESOURCES.get(resourceKind).get(actionKind).add(new URI(resourceUri));
        });
    }

    public Future<AuthResultKind> authentication(
            ResourceKind resourceKind,
            String resUri,
            AuthActionKind actionKind,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        return Future.future(promise -> {
            var resourceUri = formatUri(resUri);
            var matchedResourceUris = matchResourceUris(resourceKind, actionKind, resourceUri);
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
            doAuthentication(resourceKind, matchedResourceUris, actionKind, subjectInfo)
                    .onSuccess(promise::complete);
        });
    }

    public Future<AuthResultKind> doAuthentication(
            ResourceKind resourceKind,
            List<String> matchedResourceUris,
            AuthActionKind actionKind,
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
            ResourceKind resourceKind,
            String resourceUri,
            AuthActionKind actionKind,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        return Future.future(promise -> {
            AtomicReference<AuthResultKind> authResultKind = new AtomicReference<>(null);
            for (var subject : subjectInfo.entrySet()) {
                for (var subjectId : subject.getValue()) {
                    var key = Constant.CACHE_AUTH_POLICY
                            + resourceKind.toString() + ":"
                            + resourceUri + ":"
                            + actionKind.toString();
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
                            var newGroupNodeId = groupNodeId.substring(0, groupNodeId.length() - groupNodeLength);
                            doMatchLike(keyPrefix, subjectKind, newGroupNodeId, authResultOpt.get()._0);
                        }
                    });
        });
    }

    private Future<Optional<Tuple2<AuthResultKind, Boolean>>> parseAuth(String key) {
        return Future.future(promise ->
                redisClient.get(key)
                        .onSuccess(response -> {
                            var cache = response.toString();
                            if (cache == null) {
                                promise.complete(Optional.empty());
                            }
                            var cacheValue = cache.split("\\|");
                            var authResultKind = AuthResultKind.parse(cacheValue[0]);
                            var exclusive = Boolean.parseBoolean(cacheValue[1]);
                            promise.complete(Optional.of(new Tuple2<>(authResultKind, exclusive)));
                        }));
    }


    @SneakyThrows
    private List<String> matchResourceUris(ResourceKind resourceKind, AuthActionKind actionKind, String resUri) {
        var resourceUri = new URI(resUri);
        var matchResourceUris = LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>())
                .getOrDefault(actionKind, new ArrayList<>())
                .stream()
                .filter(uri ->
                        uri.getHost().equalsIgnoreCase(resourceUri.getHost())
                                && uri.getPort() == resourceUri.getPort()
                                && uri.getQuery().equalsIgnoreCase(resourceUri.getQuery())
                                && !uri.getPath().equalsIgnoreCase(resourceUri.getPath())
                                && pathMatcher.match(uri.getPath(), resourceUri.getPath())
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

    public Resp<Void> addLocalResource(ResourceKind resourceKind, AuthActionKind actionKind, String resourceUri) {
        if (!LOCAL_RESOURCES.containsKey(resourceKind)) {
            LOCAL_RESOURCES.put(resourceKind, new ConcurrentHashMap<>());
        }
        if (!LOCAL_RESOURCES.get(resourceKind).containsKey(actionKind)) {
            LOCAL_RESOURCES.get(resourceKind).put(actionKind, new CopyOnWriteArrayList<>());
        }
        try {
            LOCAL_RESOURCES.get(resourceKind).get(actionKind).add(new URI(resourceUri));
        } catch (URISyntaxException ignore) {
            return StandardResp.badRequest(BUSINESS_AUTH, "资源URI[%s]格式不正确", resourceUri);
        }
        return Resp.success(null);
    }

    @SneakyThrows
    public Resp<Void> removeLocalResource(
            ResourceKind resourceKind,
            String resourceUri,
            AuthActionKind actionKind,
            AuthSubjectOperatorKind subjectOperatorKind,
            AuthSubjectKind subjectKind,
            String subjectId
    ) {
        if ((subjectOperatorKind == AuthSubjectOperatorKind.INCLUDE
                || subjectOperatorKind == AuthSubjectOperatorKind.LIKE)
                && subjectKind != AuthSubjectKind.GROUP_NODE) {
            return StandardResp.badRequest(BUSINESS_AUTH, "权限主体运算类型为INCLUDE/LIKE时权限主体只能为GROUP_NODE");
        }
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(actionKind, new ArrayList<>()).remove(new URI(resourceUri));
        return Resp.success(null);
    }

    @SneakyThrows
    public Resp<Void> removeLocalResource(
            ResourceKind resourceKind,
            String resourceUri
    ) {
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(AuthActionKind.CREATE, new ArrayList<>()).remove(new URI(resourceUri));
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(AuthActionKind.DELETE, new ArrayList<>()).remove(new URI(resourceUri));
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(AuthActionKind.FETCH, new ArrayList<>()).remove(new URI(resourceUri));
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(AuthActionKind.MODIFY, new ArrayList<>()).remove(new URI(resourceUri));
        LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>()).getOrDefault(AuthActionKind.PATCH, new ArrayList<>()).remove(new URI(resourceUri));
        return Resp.success(null);
    }

    @SneakyThrows
    public String formatUri(String strUri) {
        var uri = new URI(strUri);
        var query = "";
        if (uri.getQuery() != null) {
            query = Arrays.stream(uri.getQuery().split("&"))
                    .sorted(Comparator.comparing(u -> u.split("=")[0]))
                    .collect(Collectors.joining("&"));
        }
        return uri.getScheme()
                + "//"
                + uri.getHost()
                + (uri.getPort() != -1 ? ":" + uri.getPort() : "")
                + uri.getPath()
                + (uri.getQuery() != null ? "?" + query : "");
    }

    private void scan(String key, Consumer<String> fun) {
        doScan(0, key, fun);
    }

    private void doScan(Integer cursor, String key, Consumer<String> fun) {
        redisClient.scan(new ArrayList<>() {
            {
                add(cursor + "");
                add("MATCH");
                add(key + "*");
            }
        }).onSuccess(response -> {
            response.get(1).forEach(returnKey -> {
                fun.accept(returnKey.toString());
            });
            var newCursor = response.get(0).toInteger();
            if (newCursor != 0) {
                doScan(newCursor, key, fun);
            }
        });
    }

}

