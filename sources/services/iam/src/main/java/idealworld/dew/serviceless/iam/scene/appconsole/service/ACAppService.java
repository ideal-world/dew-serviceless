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

package idealworld.dew.serviceless.iam.scene.appconsole.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.serviceless.iam.domain.ident.AppIdent;
import idealworld.dew.serviceless.iam.domain.ident.QApp;
import idealworld.dew.serviceless.iam.domain.ident.QAppIdent;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.app.AppIdentModifyReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.app.AppIdentResp;
import idealworld.dew.serviceless.iam.scene.common.service.IAMBasicService;
import idealworld.dew.serviceless.iam.util.KeyHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 应用控制台下的应用服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class ACAppService extends IAMBasicService {

    @Autowired
    private ExchangeProcessor exchangeProcessor;

    @Transactional
    public Resp<Long> addAppIdent(AppIdentAddReq appIdentAddReq, Long relAppId, Long relTenantId) {
        var appIdent = $.bean.copyProperties(appIdentAddReq, AppIdent.class);
        appIdent.setAk(KeyHelper.generateAK());
        appIdent.setSk(KeyHelper.generateSK(appIdent.getAk()));
        appIdent.setRelAppId(relAppId);
        var saveR = sendMQBySave(
                saveEntity(appIdent),
                AppIdent.class
        );
        if (!saveR.ok()) {
            return saveR;
        }
        exchangeProcessor.changeAppIdent(appIdent, relAppId, relTenantId);
        return saveR;
    }

    @Transactional
    public Resp<Void> modifyAppIdent(Long appIdentId, AppIdentModifyReq appIdentModifyReq, Long relAppId, Long relTenantId) {
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
        var updateR = sendMQByUpdate(
                updateEntity(appIdentUpdate),
                AppIdent.class,
                appIdentId
        );
        if (!updateR.ok()) {
            return updateR;
        }
        var appIdent = sqlBuilder.selectFrom(qAppIdent)
                .where(qAppIdent.id.eq(appIdentId))
                .where(qAppIdent.relAppId.eq(relAppId))
                .fetchOne();
        exchangeProcessor.changeAppIdent(appIdent, relAppId, relTenantId);
        return updateR;

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
    public Resp<Void> deleteAppIdent(Long appIdentId, Long relAppId) {
        var qAppIdent = QAppIdent.appIdent;
        var appIdent = sqlBuilder
                .selectFrom(qAppIdent)
                .where(qAppIdent.id.eq(appIdentId))
                .where(qAppIdent.relAppId.eq(relAppId))
                .fetchOne();
        var deleteR = sendMQByDelete(
                softDelEntity(sqlBuilder
                        .selectFrom(qAppIdent)
                        .where(qAppIdent.id.eq(appIdentId))
                        .where(qAppIdent.relAppId.eq(relAppId))),
                AppIdent.class,
                appIdentId
        );
        if (!deleteR.ok()) {
            return deleteR;
        }
        exchangeProcessor.deleteAppIdent(appIdent.getAk());
        return deleteR;
    }

    public Resp<String> showSk(Long appIdentId, Long relAppId) {
        var qAppIdent = QAppIdent.appIdent;
        return Resp.success(sqlBuilder
                .select(qAppIdent.sk)
                .from(qAppIdent)
                .where(qAppIdent.id.eq(appIdentId))
                .where(qAppIdent.relAppId.eq(relAppId))
                .fetchOne());
    }

    public Resp<String> showPublicKey(Long relAppId) {
        var qApp = QApp.app;
        return Resp.success(sqlBuilder
                .select(qApp.pubKey)
                .from(qApp)
                .where(qApp.id.eq(relAppId))
                .fetchOne());
    }

    public Resp<String> showPrivateKey(Long relAppId) {
        var qApp = QApp.app;
        return Resp.success(sqlBuilder
                .select(qApp.priKey)
                .from(qApp)
                .where(qApp.id.eq(relAppId))
                .fetchOne());
    }

}
