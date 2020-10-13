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

package idealworld.dew.baas.iam;


import group.idealworld.dew.core.DewContext;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.common.dto.IdentOptInfo;
import idealworld.dew.baas.iam.service.IAMBasicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ident initiator.
 *
 * @author gudaoxuri
 */
@Service
public class IAMInitiator extends IAMBasicService implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private IAMConfig iamConfig;
   /* @Autowired
    private InterceptService interceptService;*/

    /**
     * Init.
     */
    @Transactional
    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        StandardResp.setServiceFlag("IAM");
        DewContext.setOptInfoClazz(IdentOptInfo.class);
        initPermissionData();
       /* interceptService.cacheTenantAndAppStatus();
        interceptService.cacheAppIdents();*/
    }

    private void initPermissionData() {
        /*var qPosition = QPosition.position;
        if (existQuery(sqlBuilder.selectFrom(qPosition)
                .where(qPosition.code.eq(identConfig.getSecurity().getSystemAdminPositionCode())))
                .getBody()) {
            return;
        }
        saveEntity(Position.builder()
                .code(identConfig.getSecurity().getSystemAdminPositionCode())
                .name(identConfig.getSecurity().getSystemAdminPositionName())
                .icon("")
                .sort(0)
                .relAppId(Constant.OBJECT_UNDEFINED)
                .relTenantId(Constant.OBJECT_UNDEFINED)
                .build());
        saveEntity(Position.builder()
                .code(identConfig.getSecurity().getTenantAdminPositionCode())
                .name(identConfig.getSecurity().getTenantAdminPositionName())
                .icon("")
                .sort(0)
                .relAppId(Constant.OBJECT_UNDEFINED)
                .relTenantId(Constant.OBJECT_UNDEFINED)
                .build());
        saveEntity(Position.builder()
                .code(identConfig.getSecurity().getDefaultPositionCode())
                .name(identConfig.getSecurity().getDefaultPositionName())
                .icon("")
                .sort(0)
                .relAppId(Constant.OBJECT_UNDEFINED)
                .relTenantId(Constant.OBJECT_UNDEFINED)
                .build());

        saveEntity(Post.builder()
                .relOrganizationCode("")
                .relPositionCode(identConfig.getSecurity().getSystemAdminPositionCode())
                .sort(0)
                .relAppId(Constant.OBJECT_UNDEFINED)
                .relTenantId(Constant.OBJECT_UNDEFINED)
                .build());
        saveEntity(Post.builder()
                .relOrganizationCode("")
                .relPositionCode(identConfig.getSecurity().getTenantAdminPositionCode())
                .sort(0)
                .relAppId(Constant.OBJECT_UNDEFINED)
                .relTenantId(Constant.OBJECT_UNDEFINED)
                .build());
        saveEntity(Post.builder()
                .relOrganizationCode("")
                .relPositionCode(identConfig.getSecurity().getDefaultPositionCode())
                .sort(0)
                .relAppId(Constant.OBJECT_UNDEFINED)
                .relTenantId(Constant.OBJECT_UNDEFINED)
                .build());*/
    }
}
