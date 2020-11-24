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

package idealworld.dew.baas.iam.scene.tenantconsole.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.baas.common.enumeration.CommonStatus;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.ident.App;
import idealworld.dew.baas.iam.domain.ident.QApp;
import idealworld.dew.baas.iam.exchange.ExchangeProcessor;
import idealworld.dew.baas.iam.scene.common.service.CommonFunctionService;
import idealworld.dew.baas.iam.scene.common.service.IAMBasicService;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.app.AppAddReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.app.AppModifyReq;
import idealworld.dew.baas.iam.scene.tenantconsole.dto.app.AppResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 租户控制台下的应用服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class TCAppService extends IAMBasicService {

    @Autowired
    private CommonFunctionService commonFunctionService;
    @Autowired
    private ExchangeProcessor exchangeProcessor;

    @Transactional
    public Resp<Long> addApp(AppAddReq appAddReq, Long relTenantId) {
        var qApp = QApp.app;
        appAddReq.setName(appAddReq.getName().toLowerCase());
        if (sqlBuilder.select(qApp.id)
                .from(qApp)
                .where(qApp.relTenantId.eq(relTenantId))
                .where(qApp.name.eq(appAddReq.getName()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_APP, "应用名称已存在");
        }
        var app = $.bean.copyProperties(appAddReq, App.class);
        var keys = $.security.asymmetric.generateKeys("RSA", 1024);
        app.setPubKey(keys.get("PublicKey"));
        app.setPriKey(keys.get("PrivateKey"));
        app.setRelTenantId(relTenantId);
        app.setStatus(CommonStatus.ENABLED);
        return sendMQBySave(
                saveEntity(app),
                App.class
        );
    }

    @Transactional
    public Resp<Void> modifyApp(Long appId, AppModifyReq appModifyReq, Long relTenantId) {
        var qApp = QApp.app;
        if (!commonFunctionService.checkAppMembership(appId, relTenantId).ok()) {
            return StandardResp.unAuthorized(BUSINESS_APP, "应用Id不合法");
        }
        if (appModifyReq.getName() != null) {
            appModifyReq.setName(appModifyReq.getName().toLowerCase());
        }
        if (appModifyReq.getName() != null && sqlBuilder.select(qApp.id)
                .from(qApp)
                .where(qApp.relTenantId.eq(relTenantId))
                .where(qApp.name.eq(appModifyReq.getName()))
                .where(qApp.id.ne(appId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_APP, "应用名称已存在");
        }
        var appUpdate = sqlBuilder.update(qApp)
                .where(qApp.id.eq(appId))
                .where(qApp.relTenantId.eq(relTenantId));
        if (appModifyReq.getName() != null) {
            appUpdate.set(qApp.name, appModifyReq.getName());
        }
        if (appModifyReq.getIcon() != null) {
            appUpdate.set(qApp.icon, appModifyReq.getIcon());
        }
        if (appModifyReq.getParameters() != null) {
            appUpdate.set(qApp.parameters, appModifyReq.getParameters());
        }
        if (appModifyReq.getStatus() != null) {
            appUpdate.set(qApp.status, appModifyReq.getStatus());
        }
        var updateR = sendMQByUpdate(
                updateEntity(appUpdate),
                App.class,
                appId
        );
        if (!updateR.ok()) {
            return updateR;
        }
        if (appModifyReq.getStatus() != null) {
            if (appModifyReq.getStatus() == CommonStatus.ENABLED) {
                exchangeProcessor.enableApp(appId, relTenantId);
            } else {
                exchangeProcessor.disableApp(appId, relTenantId);
            }
        }
        return updateR;
    }

    public Resp<AppResp> getApp(Long appId, Long relTenantId) {
        var qApp = QApp.app;
        return getDTO(sqlBuilder.select(Projections.bean(AppResp.class,
                qApp.id,
                qApp.name,
                qApp.icon,
                qApp.parameters,
                qApp.status,
                qApp.relTenantId))
                .from(qApp)
                .where(qApp.id.eq(appId))
                .where(qApp.relTenantId.eq(relTenantId)));
    }

    public Resp<Page<AppResp>> pageApps(Long pageNumber, Integer pageSize, Long relTenantId) {
        var qApp = QApp.app;
        return pageDTOs(sqlBuilder.select(Projections.bean(AppResp.class,
                qApp.id,
                qApp.name,
                qApp.icon,
                qApp.parameters,
                qApp.status,
                qApp.relTenantId))
                .from(qApp)
                .where(qApp.relTenantId.eq(relTenantId)), pageNumber, pageSize);
    }

}
