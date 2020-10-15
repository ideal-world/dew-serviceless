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

package idealworld.dew.baas.iam.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.baas.common.enumeration.CommonStatus;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.ident.App;
import idealworld.dew.baas.iam.domain.ident.AppIdent;
import idealworld.dew.baas.iam.domain.ident.QApp;
import idealworld.dew.baas.iam.domain.ident.QAppIdent;
import idealworld.dew.baas.iam.dto.app.*;
import idealworld.dew.baas.iam.utils.KeyHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AppService extends IAMBasicService {

    private static final String BUSINESS_APP = "APP";
    private static final String BUSINESS_APP_IDENT = "APP_IDENT";

    @Transactional
    public Resp<Long> addApp(AppAddReq appAddReq, Long relTenantId) {
        var qApp = QApp.app;
        if (sqlBuilder.select(qApp.id)
                .from(qApp)
                .where(qApp.relTenantId.eq(relTenantId))
                .where(qApp.name.eq(appAddReq.getName()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_APP, "应用名称已存在");
        }
        var app = $.bean.copyProperties(appAddReq, App.class);
        app.setRelTenantId(relTenantId);
        app.setStatus(CommonStatus.ENABLED);
        return saveEntity(app);
    }

    @Transactional
    public Resp<Void> modifyApp(Long appId, AppModifyReq appModifyReq, Long relTenantId) {
        var qApp = QApp.app;
        if (!checkAppMembership(appId, relTenantId).ok()) {
            return StandardResp.unAuthorized(BUSINESS_APP, "应用Id不合法");
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
        return updateEntity(appUpdate);
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

    @Transactional
    public Resp<Void> deleteApp(Long appId, Long relTenantId) {
        var qApp = QApp.app;
        return softDelEntity(sqlBuilder
                .selectFrom(qApp)
                .where(qApp.id.eq(appId))
                .where(qApp.relTenantId.eq(relTenantId)));
    }

    private Resp<Void> checkAppMembership(Long appId, Long tenantId) {
        var qApp = QApp.app;
        if (sqlBuilder.select(qApp.id)
                .from(qApp)
                .where(qApp.id.eq(appId))
                .where(qApp.relTenantId.eq(tenantId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_APP, "应用不合法");
        }
        return Resp.success(null);
    }

    // --------------------------------------------------------------------


    @Transactional
    public Resp<Long> addAppIdent(AppIdentAddReq appIdentAddReq, Long relAppId) {
        var appIdent = $.bean.copyProperties(appIdentAddReq, AppIdent.class);
        appIdent.setAk(KeyHelper.generateAK());
        appIdent.setSk(KeyHelper.generateSK(appIdent.getAk()));
        appIdent.setRelAppId(relAppId);
        return saveEntity(appIdent);
    }

    @Transactional
    public Resp<Void> modifyAppIdent(Long appIdentId, AppIdentModifyReq appIdentModifyReq, Long relAppId) {
        var qAppIdent = QAppIdent.appIdent;
        var appIdentUpdate = sqlBuilder.update(qAppIdent)
                .where(qAppIdent.id.eq(appIdentId))
                .where(qAppIdent.relAppId.eq(relAppId));
        if (appIdentModifyReq.getNote() != null) {
            appIdentUpdate.set(qAppIdent.note, appIdentModifyReq.getNote());
        }
        if (appIdentModifyReq.getValidTime() != null) {
            appIdentUpdate.set(qAppIdent.validTime, appIdentModifyReq.getValidTime());
        }
        return updateEntity(appIdentUpdate);
    }

    public Resp<Page<AppIdentResp>> pageAppIdents(Long pageNumber, Integer pageSize, Long relAppId) {
        var qAppIdent = QAppIdent.appIdent;
        return pageDTOs(sqlBuilder.select(Projections.bean(AppIdentResp.class,
                qAppIdent.id,
                qAppIdent.note,
                qAppIdent.ak,
                qAppIdent.validTime))
                .from(qAppIdent)
                .where(qAppIdent.relAppId.eq(relAppId)), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteAppIdent(Long appIdent, Long relAppId) {
        var qAppIdent = QAppIdent.appIdent;
        return softDelEntity(sqlBuilder
                .selectFrom(qAppIdent)
                .where(qAppIdent.id.eq(appIdent))
                .where(qAppIdent.relAppId.eq(relAppId)));
    }

    public Resp<String> showSk(String ak, Long relAppId) {
        var qAppIdent = QAppIdent.appIdent;
        return Resp.success(sqlBuilder
                .select(qAppIdent.sk)
                .from(qAppIdent)
                .where(qAppIdent.ak.eq(ak))
                .where(qAppIdent.relAppId.eq(relAppId))
                .fetchOne());
    }

}
