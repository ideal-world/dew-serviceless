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

package idealworld.dew.baas.iam.scene.appconsole.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.baas.iam.domain.ident.AppIdent;
import idealworld.dew.baas.iam.domain.ident.QAppIdent;
import idealworld.dew.baas.iam.scene.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.app.AppIdentModifyReq;
import idealworld.dew.baas.iam.scene.appconsole.dto.app.AppIdentResp;
import idealworld.dew.baas.iam.scene.common.service.IAMBasicService;
import idealworld.dew.baas.iam.util.KeyHelper;
import lombok.extern.slf4j.Slf4j;
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
