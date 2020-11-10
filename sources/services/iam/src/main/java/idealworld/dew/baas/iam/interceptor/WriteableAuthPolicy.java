package idealworld.dew.baas.iam.interceptor;

import com.ecfront.dew.common.Resp;
import group.idealworld.dew.Dew;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.*;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.IAMConfig;
import idealworld.dew.baas.iam.IAMConstant;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class WriteableAuthPolicy {

    private static final String BUSINESS_AUTH = "AUTH";

    @Autowired
    private IAMConfig iamConfig;
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public Resp<Void> addPolicy(AuthPolicyAddReq authPolicyAddReq) {
        if ((authPolicyAddReq.getSubjectOperator() == AuthSubjectOperatorKind.INCLUDE
                || authPolicyAddReq.getSubjectOperator() == AuthSubjectOperatorKind.LIKE)
                && authPolicyAddReq.subjectKind != AuthSubjectKind.GROUP_NODE) {
            return StandardResp.badRequest(BUSINESS_AUTH, "权限主体运算类型为INCLUDE/LIKE时权限主体只能为GROUP_NODE");
        }
        var expireSec = (authPolicyAddReq.getExpiredTime().getTime() - System.currentTimeMillis()) / 1000;
        if (expireSec <= 0) {
            return StandardResp.badRequest(BUSINESS_AUTH, "过期时间小于当前时间");
        }
        authPolicyAddReq.setResourceUri(formatUri(authPolicyAddReq.getResourceUri()));
        var key = IAMConstant.CACHE_AUTH_POLICY
                + authPolicyAddReq.getResourceKind().toString() + ":"
                + authPolicyAddReq.getResourceUri() + ":"
                + authPolicyAddReq.getActionKind().toString() + ":"
                + authPolicyAddReq.getSubjectOperator().toString() + ":"
                + authPolicyAddReq.getSubjectKind().toString() + ":";
        var value = authPolicyAddReq.getResultKind().toString() + "|" + authPolicyAddReq.getExclusive();

        switch (authPolicyAddReq.getSubjectOperator()) {
            case INCLUDE:
                var groupNodeId = authPolicyAddReq.getSubjectId();
                value += "|" + groupNodeId;
                // 添加当前及上级Node
                while (groupNodeId.length() > 0) {
                    key += groupNodeId;
                    Dew.cluster.cache.setex(key, value, expireSec);
                    groupNodeId = groupNodeId.substring(0, groupNodeId.length() - iamConfig.getApp().getInitNodeCode().length());
                }
                break;
            default:
                key += authPolicyAddReq.getSubjectId();
                Dew.cluster.cache.setex(key, value, expireSec);
        }
        return Resp.success(null);
    }

    public Resp<Void> removePolicy(
            ResourceKind resourceKind,
            String resourceUri,
            OptActionKind actionKind,
            AuthSubjectOperatorKind subjectOperatorKind,
            AuthSubjectKind subjectKind,
            String subjectId
    ) {
        resourceUri = formatUri(resourceUri);
        var key = Constant.CACHE_AUTH_POLICY
                + resourceKind.toString() + ":"
                + resourceUri + ":"
                + actionKind.toString() + ":"
                + subjectOperatorKind.toString() + ":"
                + subjectKind.toString() + ":";
        switch (subjectOperatorKind) {
            case INCLUDE:
                var groupNodeId = subjectId;
                // 删除当前及上级Node
                while (groupNodeId.length() > 0) {
                    key += groupNodeId;
                    Dew.cluster.cache.del(key);
                    groupNodeId = groupNodeId.substring(0, groupNodeId.length() - iamConfig.getApp().getInitNodeCode().length());
                }
                break;
            default:
                key += subjectId;
                Dew.cluster.cache.del(key);
        }
        return Resp.success(null);
    }

    public Resp<Void> removePolicy(
            ResourceKind resourceKind,
            String resourceUri
    ) {
        resourceUri = formatUri(resourceUri);
        execRedisScan(
                Constant.CACHE_AUTH_POLICY
                        + resourceKind.toString() + ":"
                        + resourceUri + ":")
                .forEach(key -> {
                    Dew.cluster.cache.del(key);
                });
        return Resp.success(null);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthPolicyAddReq implements Serializable {

        private ResourceKind resourceKind;

        private String resourceUri;

        private OptActionKind actionKind;

        private AuthSubjectKind subjectKind;

        private String subjectId;

        private AuthSubjectOperatorKind subjectOperator;

        protected Date expiredTime;

        private AuthResultKind resultKind;

        private Boolean exclusive;

    }

    private Set<String> execRedisScan(String key) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keys = new HashSet<>();
            Cursor<byte[]> cursor = connection.scan(new ScanOptions.ScanOptionsBuilder().match(key + "*").count(iamConfig.getSecurity().getAuthPolicyMaxFetchCount()).build());
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
            return keys;
        });
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

