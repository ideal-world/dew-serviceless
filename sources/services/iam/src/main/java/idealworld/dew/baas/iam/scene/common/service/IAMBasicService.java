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

package idealworld.dew.baas.iam.scene.common.service;

import group.idealworld.dew.Dew;
import group.idealworld.dew.core.cluster.ClusterElection;
import idealworld.dew.baas.common.service.CommonService;
import idealworld.dew.baas.iam.IAMConfig;
import idealworld.dew.baas.iam.domain.auth.*;
import idealworld.dew.baas.iam.domain.ident.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

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

    private static final List<String> NOTIFY_BY_MQ_ENTITIES = new ArrayList<>() {
        {
            // 除租户外的资源都通知
            add(AuthPolicy.class.getName());
            add(ResourceSubject.class.getName());
            add(Resource.class.getName());
            add(Role.class.getName());
            add(RoleDef.class.getName());
            add(Group.class.getName());
            add(GroupNode.class.getName());
            add(AccountGroup.class.getName());
            add(AccountRole.class.getName());
            add(Account.class.getName());
            add(AccountBind.class.getName());
            add(AccountApp.class.getName());
            add(AccountIdent.class.getName());
            add(App.class.getName());
            add(AppIdent.class.getName());
        }
    };

    @Autowired
    private IAMConfig iamConfig;

    @Override
    public String topicName() {
        return iamConfig.getEventNotifyTopicName();
    }

    @Override
    public boolean notifyByMQ(String entityName) {
        return NOTIFY_BY_MQ_ENTITIES.contains(entityName);
    }

}
