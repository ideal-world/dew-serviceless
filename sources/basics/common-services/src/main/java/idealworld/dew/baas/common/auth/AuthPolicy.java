package idealworld.dew.baas.common.auth;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import group.idealworld.dew.Dew;
import idealworld.dew.baas.common.CommonConfig;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.*;

/**
 * @author gudaoxuri
 */
@Component
public class AuthPolicy {

    private static final Map<ResourceKind, Map<AuthActionKind, List<String>>> LOCAL_RESOURCES = new HashMap<>();

    @Autowired
    private CommonConfig commonConfig;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @SneakyThrows
    @PostConstruct
    public void init() {
        var cursor = redisTemplate.opsForHash()
                .scan(Constant.CACHE_AUTH_POLICY,
                        ScanOptions.scanOptions().match("*").count(commonConfig.getAuthPolicyMaxFetchCount()).build());
        while (cursor.hasNext()) {
            var key = ((String) cursor.next().getKey());
            var keyItems = key.substring(Constant.CACHE_AUTH_POLICY.length()).split(":");
            var resourceKind = ResourceKind.parse(keyItems[0]);
            var resourceUri = keyItems[1];
            var actionKind = AuthActionKind.parse(keyItems[2]);
            if (!LOCAL_RESOURCES.containsKey(resourceKind)) {
                LOCAL_RESOURCES.put(resourceKind, new HashMap<>());
            }
            if (!LOCAL_RESOURCES.get(resourceKind).containsKey(actionKind)) {
                LOCAL_RESOURCES.get(resourceKind).put(actionKind, new ArrayList<>());
            }
            LOCAL_RESOURCES.get(resourceKind).get(actionKind).add(resourceUri);
        }
        cursor.close();
        $.timer.periodic(commonConfig.getAuthPolicyExpireCleanIntervalSec(), true, () -> {
            var cleanCursor = redisTemplate.opsForHash()
                    .scan(Constant.CACHE_AUTH_POLICY,
                            ScanOptions.scanOptions().match("*").count(commonConfig.getAuthPolicyMaxFetchCount()).build());
            while (cleanCursor.hasNext()) {
                var current = cursor.next();
                var key = ((String) current.getKey());
                var value = ((String) current.getValue());
                if (Long.parseLong(value.split("\\|")[2]) < System.currentTimeMillis()) {
                    Dew.cluster.cache.del(key);
                }
            }
            cursor.close();
        });
    }

    public void addPolicy(AuthPolicyAddReq authPolicyAddReq) {
        var key = Constant.CACHE_AUTH_POLICY
                + authPolicyAddReq.getResourceKind().toString() + ":"
                + authPolicyAddReq.getResourceUri() + ":"
                + authPolicyAddReq.getActionKind().toString();
        var field = authPolicyAddReq.getSubjectKind().toString() + ":"
                + authPolicyAddReq.getSubjectId();
        // TODO 只支持了eq
        var value = authPolicyAddReq.getResultKind().toString() + "|" + authPolicyAddReq.getExclusive() + "|" + authPolicyAddReq.getExpiredTime().getTime()
        Dew.cluster.cache.hset(key, field, value);
        if (!LOCAL_RESOURCES.containsKey(authPolicyAddReq.getResourceKind())) {
            LOCAL_RESOURCES.put(authPolicyAddReq.getResourceKind(), new HashMap<>());
        }
        if (!LOCAL_RESOURCES.get(authPolicyAddReq.getResourceKind()).containsKey(authPolicyAddReq.getActionKind())) {
            LOCAL_RESOURCES.get(authPolicyAddReq.getResourceKind()).put(authPolicyAddReq.getActionKind(), new ArrayList<>());
        }
        LOCAL_RESOURCES.get(authPolicyAddReq.getResourceKind()).get(authPolicyAddReq.getActionKind()).add(authPolicyAddReq.getResourceUri());
    }

    public void removePolicy(
            ResourceKind resourceKind,
            String resourceUri,
            AuthActionKind actionKind,
            AuthSubjectKind subjectKind,
            String subjectId
    ) {
        Dew.cluster.cache.hdel(Constant.CACHE_AUTH_POLICY
                        + resourceKind.toString() + ":"
                        + resourceUri + ":"
                        + actionKind.toString(),
                subjectKind.toString() + ":"
                        + subjectId);
        LOCAL_RESOURCES.get(resourceKind).get(actionKind).remove(resourceUri);
    }

    public void removePolicy(
            ResourceKind resourceKind,
            String resourceUri
    ) {
        Dew.cluster.cache.del(Constant.CACHE_AUTH_POLICY
                + resourceKind.toString() + ":"
                + resourceUri + ":"
                + AuthActionKind.ALL.toString());
        LOCAL_RESOURCES.get(resourceKind).get(AuthActionKind.ALL).remove(resourceUri);
        Dew.cluster.cache.del(Constant.CACHE_AUTH_POLICY
                + resourceKind.toString() + ":"
                + resourceUri + ":"
                + AuthActionKind.CREATE.toString());
        LOCAL_RESOURCES.get(resourceKind).get(AuthActionKind.CREATE).remove(resourceUri);
        Dew.cluster.cache.del(Constant.CACHE_AUTH_POLICY
                + resourceKind.toString() + ":"
                + resourceUri + ":"
                + AuthActionKind.DELETE.toString());
        LOCAL_RESOURCES.get(resourceKind).get(AuthActionKind.DELETE).remove(resourceUri);
        Dew.cluster.cache.del(Constant.CACHE_AUTH_POLICY
                + resourceKind.toString() + ":"
                + resourceUri + ":"
                + AuthActionKind.FETCH.toString());
        LOCAL_RESOURCES.get(resourceKind).get(AuthActionKind.FETCH).remove(resourceUri);
        Dew.cluster.cache.del(Constant.CACHE_AUTH_POLICY
                + resourceKind.toString() + ":"
                + resourceUri + ":"
                + AuthActionKind.MODIFY.toString());
        LOCAL_RESOURCES.get(resourceKind).get(AuthActionKind.MODIFY).remove(resourceUri);
        Dew.cluster.cache.del(Constant.CACHE_AUTH_POLICY
                + resourceKind.toString() + ":"
                + resourceUri + ":"
                + AuthActionKind.PATCH.toString());
        LOCAL_RESOURCES.get(resourceKind).get(AuthActionKind.PATCH).remove(resourceUri);
    }

    public Resp<AuthResultKind> authentication(
            ResourceKind resourceKind,
            String resourceUri,
            AuthActionKind actionKind,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        var authResultKind = AuthResultKind.ACCEPT;
        var matchedResourceUris = matchResourceUris(resourceKind, resourceUri);
        for (var matchedResourceUri : matchedResourceUris) {
            for (var subject : subjectInfo.entrySet()) {
                for (var subjectId : subject.getValue()) {
                    var key = Constant.CACHE_AUTH_POLICY
                            + resourceKind.toString() + ":"
                            + matchedResourceUri + ":"
                            + actionKind.toString();
                    var field = subject.getKey().toString() + ":"
                            + subjectId;
                    var cache = Dew.cluster.cache.hget(key, field);
                    if (cache != null) {
                        var cacheValue = cache.split("\\|");
                        authResultKind = AuthResultKind.parse(cacheValue[0]);
                        var exclusive = Boolean.parseBoolean(cacheValue[1]);
                        var expiredTime = Long.parseLong(cacheValue[2]);
                        if (expiredTime < System.currentTimeMillis()) {
                            Dew.cluster.cache.del(key);
                        }
                        if (exclusive) {
                            return Resp.success(authResultKind);
                        }
                    }
                }
            }
        }
        return Resp.success(authResultKind);
    }

    private List<String> matchResourceUris(ResourceKind resourceKind, String resourceUri) {

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthPolicyAddReq implements Serializable {

        private ResourceKind resourceKind;

        private String resourceUri;

        private AuthActionKind actionKind;

        private AuthSubjectKind subjectKind;

        private String subjectId;

        private AuthSubjectOperatorKind subjectOperator;

        protected Date expiredTime;

        private AuthResultKind resultKind;

        private Boolean exclusive;

    }

}

