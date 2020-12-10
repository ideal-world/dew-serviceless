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

package idealworld.dew.serviceless.iam.exchange;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import com.fasterxml.jackson.databind.node.ObjectNode;
import group.idealworld.dew.Dew;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.framework.dto.exchange.ExchangeData;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.serviceless.common.enumeration.CommonStatus;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.domain.auth.*;
import idealworld.dew.serviceless.iam.domain.ident.*;
import idealworld.dew.serviceless.iam.scene.common.service.IAMBasicService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * @author gudaoxuri
 */
@Slf4j
@Component
public class ExchangeProcessor extends IAMBasicService {

    private static final String QUICK_CHECK_SPLIT = "#";
    private static final String BUSINESS_AUTH = "AUTH";

    public static void publish(String subjectCategory, OptActionKind actionKind, Object subjectIds, Map<String, Object> detailData) {
        if (subjectIds instanceof Collection<?>) {
            for (var subjectId : (Collection<?>) subjectIds) {
                Dew.cluster.mq.publish(Constant.EVENT_NOTIFY_TOPIC_BY_IAM,
                        subjectCategory + QUICK_CHECK_SPLIT + $.json.toJsonString(ExchangeData.builder()
                                .actionKind(actionKind)
                                .subjectCategory(subjectCategory)
                                .subjectId(subjectId.toString())
                                .fetchUrl(fetchUrl(actionKind, subjectCategory, subjectId.toString()))
                                .detailData(detailData)
                                .build()));
            }
            return;
        }
        Dew.cluster.mq.publish(Constant.EVENT_NOTIFY_TOPIC_BY_IAM,
                subjectCategory + QUICK_CHECK_SPLIT + $.json.toJsonString(ExchangeData.builder()
                        .actionKind(actionKind)
                        .subjectCategory(subjectCategory)
                        .subjectId(subjectIds.toString())
                        .fetchUrl(fetchUrl(actionKind, subjectCategory, subjectIds.toString()))
                        .detailData(detailData)
                        .build()));
    }

    private static String fetchUrl(OptActionKind actionKind, String subjectCategory, Object subjectId) {
        if (actionKind == OptActionKind.DELETE) {
            return "";
        }
        String url = "http://" + Dew.Info.name + "/";
        if (subjectCategory.equals(App.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/app/" + subjectId;
        } else if (subjectCategory.equals(AppIdent.class.getSimpleName().toLowerCase())) {
            return url + "console/app/app/ident/" + subjectId;
        } else if (subjectCategory.equals(Account.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/account/" + subjectId;
        } else if (subjectCategory.equals(AccountIdent.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/account/ident/" + subjectId;
        } else if (subjectCategory.equals(AccountApp.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/account/app/" + subjectId;
        } else if (subjectCategory.equals(AccountGroup.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/account/group/" + subjectId;
        } else if (subjectCategory.equals(AccountRole.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/account/role/" + subjectId;
        } else if (subjectCategory.equals(Group.class.getSimpleName().toLowerCase())) {
            return url + "console/app/group/" + subjectId;
        } else if (subjectCategory.equals(GroupNode.class.getSimpleName().toLowerCase())) {
            return url + "console/app/group/node/" + subjectId;
        } else if (subjectCategory.equals(RoleDef.class.getSimpleName().toLowerCase())) {
            return url + "console/app/role/def/" + subjectId;
        } else if (subjectCategory.equals(Role.class.getSimpleName().toLowerCase())) {
            return url + "console/app/role/" + subjectId;
        } else if (subjectCategory.startsWith(ResourceSubject.class.getSimpleName().toLowerCase())) {
            return url + "console/app/resource/subject/" + subjectId;
        } else if (subjectCategory.equals(Resource.class.getSimpleName().toLowerCase())) {
            return url + "console/app/resource/" + subjectId;
        } else if (subjectCategory.equals(AuthPolicy.class.getSimpleName().toLowerCase())) {
            return url + "console/app/authpolicy/" + subjectId;
        }
        log.error("[MQ]URL for resources [{}] not found", subjectCategory);
        return "";
    }


    public void cacheAppIdents() {
        if (!ELECTION.isLeader()) {
            return;
        }
        var qAppIdent = QAppIdent.appIdent;
        var qTenant = QTenant.tenant;
        var qApp = QApp.app;
        sqlBuilder
                .select(qAppIdent.ak, qAppIdent.sk, qAppIdent.relAppId, qAppIdent.validTime, qApp.relTenantId)
                .from(qAppIdent)
                .innerJoin(qApp)
                .on(qApp.id.eq(qAppIdent.relAppId)
                        .and(qApp.status.eq(CommonStatus.ENABLED)))
                .innerJoin(qTenant)
                .on(qTenant.id.eq(qApp.relTenantId)
                        .and(qTenant.status.eq(CommonStatus.ENABLED)))
                .where(qAppIdent.validTime.gt(new Date()))
                .fetch()
                .forEach(info -> {
                    var ak = info.get(0, String.class);
                    var sk = info.get(1, String.class);
                    var appId = info.get(2, Long.class);
                    var validTime = info.get(3, Date.class);
                    var tenantId = info.get(4, Long.class);
                    changeAppIdent(ak, sk, validTime, appId, tenantId);
                });
    }

    public void enableTenant(Long tenantId) {
        var qAppIdent = QAppIdent.appIdent;
        var qApp = QApp.app;
        sqlBuilder
                .select(qAppIdent.ak, qAppIdent.sk, qAppIdent.relAppId, qAppIdent.validTime)
                .from(qAppIdent)
                .innerJoin(qApp)
                .on(qApp.id.eq(qAppIdent.relAppId).and(qApp.status.eq(CommonStatus.ENABLED)))
                .where(qApp.relTenantId.eq(tenantId))
                .where(qAppIdent.validTime.gt(new Date()))
                .fetch()
                .forEach(info -> {
                    var ak = info.get(0, String.class);
                    var sk = info.get(1, String.class);
                    var appId = info.get(2, Long.class);
                    var validTime = info.get(3, Date.class);
                    changeAppIdent(ak, sk, validTime, appId, tenantId);
                });
    }

    public void disableTenant(Long tenantId) {
        var qAppIdent = QAppIdent.appIdent;
        var qApp = QApp.app;
        sqlBuilder
                .select(qAppIdent.ak)
                .from(qAppIdent)
                .innerJoin(qApp)
                .on(qApp.id.eq(qAppIdent.relAppId))
                .where(qApp.relTenantId.eq(tenantId))
                .fetch()
                .forEach(this::deleteAppIdent);
    }

    public void enableApp(Long appId, Long tenantId) {
        var qApp = QApp.app;
        sqlBuilder
                .select(qApp.pubKey, qApp.priKey)
                .from(qApp)
                .where(qApp.id.eq(appId))
                .fetch()
                .forEach(info -> {
                    var publicKey = info.get(0, String.class);
                    var privateKey = info.get(1, String.class);
                    Dew.cluster.cache.set(IAMConstant.CACHE_APP_INFO + appId, tenantId + "\n" + publicKey + "\n" + privateKey);
                });
        var qAppIdent = QAppIdent.appIdent;
        sqlBuilder
                .select(qAppIdent.ak, qAppIdent.sk, qAppIdent.validTime)
                .from(qAppIdent)
                .where(qAppIdent.relAppId.eq(appId))
                .where(qAppIdent.validTime.gt(new Date()))
                .fetch()
                .forEach(info -> {
                    var ak = info.get(0, String.class);
                    var sk = info.get(1, String.class);
                    var validTime = info.get(2, Date.class);
                    changeAppIdent(ak, sk, validTime, appId, tenantId);
                });
    }

    public void disableApp(Long appId, Long tenantId) {
        var qAppIdent = QAppIdent.appIdent;
        sqlBuilder
                .select(qAppIdent.ak)
                .from(qAppIdent)
                .where(qAppIdent.relAppId.eq(appId))
                .fetch()
                .forEach(this::deleteAppIdent);
        Dew.cluster.cache.del(IAMConstant.CACHE_APP_INFO + appId);
    }

    public void changeAppIdent(AppIdent appIdent, Long appId, Long tenantId) {
        changeAppIdent(appIdent.getAk(), appIdent.getSk(), appIdent.getValidTime(), appId, tenantId);
    }

    public void deleteAppIdent(String ak) {
        Dew.cluster.cache.del(IAMConstant.CACHE_APP_AK + ak);
    }

    private void changeAppIdent(String ak, String sk, Date validTime, Long appId, Long tenantId) {
        Dew.cluster.cache.del(IAMConstant.CACHE_APP_AK + ak);
        if (validTime == null) {
            Dew.cluster.cache.set(IAMConstant.CACHE_APP_AK + ak, sk + ":" + tenantId + ":" + appId);
        } else {
            Dew.cluster.cache.setex(IAMConstant.CACHE_APP_AK + ak, sk + ":" + tenantId + ":" + appId,
                    (validTime.getTime() - System.currentTimeMillis()) / 1000);
        }
    }

    public Resp<Void> addPolicy(AuthPolicyInfo policyInfo) {
        if ((policyInfo.getSubjectOperator() == AuthSubjectOperatorKind.INCLUDE
                || policyInfo.getSubjectOperator() == AuthSubjectOperatorKind.LIKE)
                && policyInfo.subjectKind != AuthSubjectKind.GROUP_NODE) {
            return StandardResp.badRequest(BUSINESS_AUTH, "权限主体运算类型为INCLUDE/LIKE时权限主体只能为GROUP_NODE");
        }
        policyInfo.setResourceUri(URIHelper.formatUri(policyInfo.getResourceUri()));
        var key = IAMConstant.CACHE_AUTH_POLICY
                + policyInfo.getResourceUri().replace("//", "") + ":"
                + policyInfo.getActionKind().toString().toLowerCase();
        var policyValue = Dew.cluster.cache.exists(key) ? (ObjectNode) $.json.toJson(Dew.cluster.cache.get(key)) : $.json.createObjectNode();
        if (!policyValue.has(policyInfo.subjectOperator.toString().toLowerCase())) {
            policyValue.set(policyInfo.subjectOperator.toString().toLowerCase(), $.json.createObjectNode());
        }
        if (!policyValue.get(policyInfo.subjectOperator.toString().toLowerCase()).has(policyInfo.subjectKind.toString().toLowerCase())) {
            ((ObjectNode) policyValue.get(policyInfo.subjectOperator.toString().toLowerCase())).set(policyInfo.subjectKind.toString().toLowerCase(), $.json.createObjectNode());
        }
        ((ObjectNode) policyValue.get(policyInfo.subjectOperator.toString().toLowerCase()).get(policyInfo.subjectKind.toString().toLowerCase())).putArray(policyInfo.getSubjectId());
        Dew.cluster.cache.set(key, $.json.toJsonString(policyValue));
        return Resp.success(null);
    }

    public Resp<Void> removePolicy(AuthPolicyInfo policyInfo) {
        policyInfo.setResourceUri(URIHelper.formatUri(policyInfo.getResourceUri()));
        var key = IAMConstant.CACHE_AUTH_POLICY
                + policyInfo.getResourceUri().replace("//", "") + ":"
                + policyInfo.getActionKind().toString().toLowerCase();
        if (!Dew.cluster.cache.exists(key)) {
            return Resp.success(null);
        }
        var policyValue = (ObjectNode) $.json.toJson(Dew.cluster.cache.get(key));
        if (!policyValue.has(policyInfo.subjectOperator.toString().toLowerCase())
                || !policyValue.get(policyInfo.subjectOperator.toString().toLowerCase()).has(policyInfo.subjectKind.toString().toLowerCase())) {
            return Resp.success(null);
        }
        ((ObjectNode) policyValue.get(policyInfo.subjectOperator.toString().toLowerCase()).get(policyInfo.subjectKind.toString().toLowerCase())).remove(policyInfo.getSubjectId());
        Dew.cluster.cache.set(key, $.json.toJsonString(policyValue));
        return Resp.success(null);
    }

    public Resp<Void> removePolicy(String resourceUri, OptActionKind actionKind) {
        resourceUri = URIHelper.formatUri(resourceUri);
        var key = IAMConstant.CACHE_AUTH_POLICY
                + resourceUri.replace("//", "") + ":"
                + actionKind.toString().toLowerCase();
        Dew.cluster.cache.del(key);
        return Resp.success(null);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthPolicyInfo implements Serializable {

        private String resourceUri;

        private OptActionKind actionKind;

        private AuthSubjectKind subjectKind;

        private String subjectId;

        private AuthSubjectOperatorKind subjectOperator;

    }

}
