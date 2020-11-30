/*
 * Copyright 2020. the original author or authors.
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

package idealworld.dew.serviceless.iam.scene.common.service;

import com.ecfront.dew.common.Resp;
import group.idealworld.dew.Dew;
import group.idealworld.dew.core.cluster.ClusterElection;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.common.service.CommonService;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;

import java.util.HashMap;
import java.util.Map;

/**
 * IAM basic service.
 *
 * @author gudaoxuri
 */
public abstract class IAMBasicService extends CommonService<Long> {

    protected static final ClusterElection ELECTION = Dew.cluster.election.instance("iam");

    // TODO 重新整理
    protected static final String BUSINESS_PUBLIC = "PUBLIC";
    protected static final String BUSINESS_TENANT = "TENANT";
    protected static final String BUSINESS_TENANT_IDENT = "TENANT_IDENT";
    protected static final String BUSINESS_TENANT_CERT = "TENANT_CERT";
    protected static final String BUSINESS_ACCOUNT = "ACCOUNT";
    protected static final String BUSINESS_ACCOUNT_IDENT = "ACCOUNT_IDENT";
    protected static final String BUSINESS_ACCOUNT_CERT = "ACCOUNT_CERT";
    protected static final String BUSINESS_ACCOUNT_APP = "ACCOUNT_APP";
    protected static final String BUSINESS_ACCOUNT_ROLE = "ACCOUNT_ROLE";
    protected static final String BUSINESS_ACCOUNT_GROUP = "ACCOUNT_GROUP";
    protected static final String BUSINESS_ACCOUNT_BIND = "ACCOUNT_BIND";
    protected static final String BUSINESS_APP = "APP";
    protected static final String BUSINESS_APP_IDENT = "APP_IDENT";
    protected static final String BUSINESS_ROLE = "ROLE";
    protected static final String BUSINESS_ROLE_DEF = "ROLE_DEF";
    protected static final String BUSINESS_GROUP = "GROUP";
    protected static final String BUSINESS_GROUP_NODE = "GROUP_NODE";
    protected static final String BUSINESS_RESOURCE = "RESOURCE";
    protected static final String BUSINESS_RESOURCE_SUBJECT = "RESOURCE_SUBJECT";
    protected static final String BUSINESS_AUTH_POLICY = "AUTH_POLICY";
    protected static final String BUSINESS_OAUTH = "OAUTH";

    protected <E> Resp<E> sendMQBySave(Resp<E> resp, Class<?> subjectCategoryClazz) {
        return sendMQBySave(resp, subjectCategoryClazz, new HashMap<>());
    }

    protected <E> Resp<E> sendMQBySave(Resp<E> resp, Class<?> subjectCategoryClazz, Map<String, Object> detailData) {
        return sendMQBySave(resp, subjectCategoryClazz.getSimpleName().toLowerCase(), detailData);
    }

    protected <E> Resp<E> sendMQBySave(Resp<E> resp, String subjectCategory, Map<String, Object> detailData) {
        if (!resp.ok()) {
            return resp;
        }
        ExchangeProcessor.publish(subjectCategory, OptActionKind.CREATE, resp.getBody(), detailData);
        return resp;
    }

    protected <E> Resp<E> sendMQByUpdate(Resp<E> resp, Class<?> subjectCategoryClazz, Object subjectId) {
        return sendMQByUpdate(resp, subjectCategoryClazz, subjectId, new HashMap<>());
    }

    protected <E> Resp<E> sendMQByUpdate(Resp<E> resp, Class<?> subjectCategoryClazz, Object subjectId, Map<String, Object> detailData) {
        return sendMQByUpdate(resp, subjectCategoryClazz.getSimpleName().toLowerCase(), subjectId, detailData);
    }

    protected <E> Resp<E> sendMQByUpdate(Resp<E> resp, String subjectCategory, Object subjectId, Map<String, Object> detailData) {
        if (!resp.ok()) {
            return resp;
        }
        ExchangeProcessor.publish(subjectCategory, OptActionKind.MODIFY, subjectId, detailData);
        return resp;
    }

    protected <E> Resp<E> sendMQByDelete(Resp<E> resp, Class<?> subjectCategoryClazz, Object subjectId) {
        return sendMQByDelete(resp, subjectCategoryClazz, subjectId, new HashMap<>());
    }

    protected <E> Resp<E> sendMQByDelete(Resp<E> resp, Class<?> subjectCategoryClazz, Object subjectId, Map<String, Object> detailData) {
        return sendMQByDelete(resp, subjectCategoryClazz.getSimpleName().toLowerCase(), subjectId, detailData);
    }

    protected <E> Resp<E> sendMQByDelete(Resp<E> resp, String subjectCategory, Object subjectId, Map<String, Object> detailData) {
        if (!resp.ok()) {
            return resp;
        }
        ExchangeProcessor.publish(subjectCategory, OptActionKind.DELETE, subjectId, detailData);
        return resp;
    }

}
