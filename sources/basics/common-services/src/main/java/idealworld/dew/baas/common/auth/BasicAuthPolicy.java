package idealworld.dew.baas.common.auth;

import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.tuple.Tuple2;
import group.idealworld.dew.Dew;
import idealworld.dew.baas.common.CommonConfig;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.*;
import idealworld.dew.baas.common.resp.StandardResp;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.util.AntPathMatcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
public class BasicAuthPolicy {

    public static final String GROUP_CODE_NODE_CODE_SPLIT = "#";

    protected static final String BUSINESS_AUTH = "AUTH";

    protected static final Map<ResourceKind, Map<AuthActionKind, List<URI>>> LOCAL_RESOURCES = new ConcurrentHashMap<>();

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    protected CommonConfig commonConfig;
    @Autowired
    protected RedisTemplate<String, String> redisTemplate;

    protected Integer groupNodeItemLength;

    @SneakyThrows
    public void init(Integer groupNodeItemLength) {
        this.groupNodeItemLength = groupNodeItemLength;
        for (String key : execRedisScan(Constant.CACHE_AUTH_POLICY)) {
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
        }
    }

    public Resp<AuthResultKind> authentication(
            ResourceKind resourceKind,
            String resourceUri,
            AuthActionKind actionKind,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        resourceUri = formatUri(resourceUri);
        var matchedResourceUris = matchResourceUris(resourceKind, actionKind, resourceUri);
        if (matchedResourceUris.isEmpty()) {
            // 资源不需要鉴权
            return Resp.success(AuthResultKind.ACCEPT);
        }
        if (subjectInfo.isEmpty()) {
            // 资源需要鉴权但没有对应的权限主体
            return Resp.success(AuthResultKind.ACCEPT);
        }
        AuthResultKind authResultKind = AuthResultKind.REJECT;
        for (var matchedResourceUri : matchedResourceUris) {
            var authResultOpt = doAuthentication(resourceKind, matchedResourceUri, actionKind, subjectInfo);
            if (authResultOpt.isPresent()) {
                if (authResultOpt.get()._1) {
                    return Resp.success(authResultOpt.get()._0);
                }
                authResultKind = authResultOpt.get()._0;
            }
        }
        return Resp.success(authResultKind);
    }

    private Optional<Tuple2<AuthResultKind, Boolean>> doAuthentication(
            ResourceKind resourceKind,
            String resourceUri,
            AuthActionKind actionKind,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        AuthResultKind authResultKind = null;
        for (var subject : subjectInfo.entrySet()) {
            for (var subjectId : subject.getValue()) {
                var key = Constant.CACHE_AUTH_POLICY
                        + resourceKind.toString() + ":"
                        + resourceUri + ":"
                        + actionKind.toString();
                var authResultOpt = matchEQ(key, subject.getKey(), subjectId);
                if (authResultOpt.isPresent()) {
                    if (authResultOpt.get()._1) {
                        return authResultOpt;
                    }
                    authResultKind = authResultOpt.get()._0;
                }
                authResultOpt = matchInclude(key, subject.getKey(), subjectId);
                if (authResultOpt.isPresent()) {
                    if (authResultOpt.get()._1) {
                        return authResultOpt;
                    }
                    authResultKind = authResultOpt.get()._0;
                }
                authResultOpt = matchLike(key, subject.getKey(), subjectId);
                if (authResultOpt.isPresent()) {
                    if (authResultOpt.get()._1) {
                        return authResultOpt;
                    }
                    authResultKind = authResultOpt.get()._0;
                }
            }
        }
        return authResultKind == null ? Optional.empty() : Optional.of(new Tuple2<>(authResultKind, false));
    }

    private Optional<Tuple2<AuthResultKind, Boolean>> matchEQ(String keyPrefix, AuthSubjectKind subjectKind, String subjectId) {
        return parseAuth(keyPrefix + AuthSubjectOperatorKind.EQ.toString() + ":" + subjectKind.toString() + ":" + subjectId);
    }

    private Optional<Tuple2<AuthResultKind, Boolean>> matchInclude(String keyPrefix, AuthSubjectKind subjectKind, String subjectId) {
        // 在添加时已经为上级节点添加了key，所以这里只要发起一次查询即可
        return parseAuth(keyPrefix + AuthSubjectOperatorKind.INCLUDE.toString() + ":" + subjectKind.toString() + ":" + subjectId);
    }


    private Optional<Tuple2<AuthResultKind, Boolean>> matchLike(String keyPrefix, AuthSubjectKind subjectKind, String subjectId) {
        AuthResultKind authResultKind = null;
        var groupNodeId = subjectId;
        while (groupNodeId.length() > 0) {
            var authResultOpt = parseAuth(keyPrefix + AuthSubjectOperatorKind.LIKE.toString() + ":" + subjectKind.toString() + ":" + groupNodeId);
            if (authResultOpt.isPresent()) {
                if (authResultOpt.get()._1) {
                    return authResultOpt;
                }
                authResultKind = authResultOpt.get()._0;
            }
            groupNodeId = groupNodeId.substring(0, groupNodeId.length() - groupNodeItemLength);
        }
        return authResultKind == null ? Optional.empty() : Optional.of(new Tuple2<>(authResultKind, false));
    }

    private Optional<Tuple2<AuthResultKind, Boolean>> parseAuth(String key) {
        var cache = Dew.cluster.cache.get(key);
        if (cache == null) {
            return Optional.empty();
        }
        var cacheValue = cache.split("\\|");
        var authResultKind = AuthResultKind.parse(cacheValue[0]);
        var exclusive = Boolean.parseBoolean(cacheValue[1]);
        return Optional.of(new Tuple2<>(authResultKind, exclusive));
    }

    protected Set<String> execRedisScan(String key) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keys = new HashSet<>();
            Cursor<byte[]> cursor = connection.scan(new ScanOptions.ScanOptionsBuilder().match(key + "*").count(commonConfig.getAuthPolicyMaxFetchCount()).build());
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
            return keys;
        });
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

}

