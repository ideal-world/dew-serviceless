package idealworld.dew.baas.iam.interceptor;

import com.ecfront.dew.common.Resp;
import group.idealworld.dew.Dew;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.auth.BasicAuthPolicy;
import idealworld.dew.baas.common.enumeration.*;
import idealworld.dew.baas.common.resp.StandardResp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
public class WriteableAuthPolicy extends BasicAuthPolicy {

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
        var key = Constant.CACHE_AUTH_POLICY
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
                    groupNodeId = groupNodeId.substring(0, groupNodeId.length() - groupNodeItemLength);
                }
                break;
            default:
                key += authPolicyAddReq.getSubjectId();
                Dew.cluster.cache.setex(key, value, expireSec);
        }
        return addLocalResource(authPolicyAddReq.getResourceKind(), authPolicyAddReq.getActionKind(), authPolicyAddReq.getResourceUri());
    }

    public Resp<Void> removePolicy(
            ResourceKind resourceKind,
            String resourceUri,
            AuthActionKind actionKind,
            AuthSubjectOperatorKind subjectOperatorKind,
            AuthSubjectKind subjectKind,
            String subjectId
    ) {
        var removeR = removeLocalResource(resourceKind, resourceUri, actionKind, subjectOperatorKind, subjectKind, subjectId);
        if (!removeR.ok()) {
            return removeR;
        }
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
                    groupNodeId = groupNodeId.substring(0, groupNodeId.length() - groupNodeItemLength);
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
        var removeR = removeLocalResource(resourceKind, resourceUri);
        if (!removeR.ok()) {
            return removeR;
        }
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

        private AuthActionKind actionKind;

        private AuthSubjectKind subjectKind;

        private String subjectId;

        private AuthSubjectOperatorKind subjectOperator;

        protected Date expiredTime;

        private AuthResultKind resultKind;

        private Boolean exclusive;

    }

}

