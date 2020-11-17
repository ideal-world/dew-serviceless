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

package idealworld.dew.baas.iam.interceptor;

import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.tuple.Tuple3;
import group.idealworld.dew.Dew;
import idealworld.dew.baas.common.enumeration.CommonStatus;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.IAMConstant;
import idealworld.dew.baas.iam.domain.ident.AppIdent;
import idealworld.dew.baas.iam.domain.ident.QApp;
import idealworld.dew.baas.iam.domain.ident.QAppIdent;
import idealworld.dew.baas.iam.domain.ident.QTenant;
import idealworld.dew.baas.iam.scene.common.service.IAMBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * Intercept service.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class InterceptService extends IAMBasicService {

    public void cacheTenantAndAppStatus() {
        if (!ELECTION.isLeader()) {
            return;
        }
        var qTenant = QTenant.tenant;
        sqlBuilder.select(qTenant.id)
                .from(qTenant)
                .where(qTenant.status.eq(CommonStatus.ENABLED))
                .fetch()
                .forEach(id -> Dew.cluster.cache.set(IAMConstant.CACHE_TENANT_STATUS_ENABLE + id, ""));
        var qApp = QApp.app;
        sqlBuilder.select(qApp.id)
                .from(qApp)
                .where(qApp.status.eq(CommonStatus.ENABLED))
                .fetch()
                .forEach(id -> Dew.cluster.cache.set(IAMConstant.CACHE_APP_STATUS_ENABLE + id, ""));
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
                    var relAppId = info.get(2, Long.class);
                    var validTime = info.get(3, Date.class);
                    var relTenantId = info.get(4, Long.class);
                    if (validTime.getTime() == IAMConstant.MAX_TIME.getTime()) {
                        Dew.cluster.cache.set(IAMConstant.CACHE_APP_AK + ak, sk + ":" + relTenantId + ":" + relAppId);
                    } else {
                        Dew.cluster.cache.setex(IAMConstant.CACHE_APP_AK + ak, sk + ":" + relTenantId + ":" + relAppId,
                                (validTime.getTime() - System.currentTimeMillis()) / 1000);
                    }
                });
    }

    public boolean isTenantLegal(Long appId) {
        return Dew.cluster.cache.exists(IAMConstant.CACHE_TENANT_STATUS_ENABLE + appId);
    }

    public boolean isAppLegal(Long appId) {
        return Dew.cluster.cache.exists(IAMConstant.CACHE_APP_STATUS_ENABLE + appId);
    }

    public void changeTenantStatus(Long tenantId, CommonStatus status) {
        if (status == CommonStatus.ENABLED) {
            Dew.cluster.cache.set(IAMConstant.CACHE_TENANT_STATUS_ENABLE + tenantId, "");
        } else {
            Dew.cluster.cache.del(IAMConstant.CACHE_TENANT_STATUS_ENABLE + tenantId);
        }
    }

    public void changeAppStatus(Long appId, CommonStatus status) {
        if (status == CommonStatus.ENABLED) {
            Dew.cluster.cache.set(IAMConstant.CACHE_APP_STATUS_ENABLE + appId, "");
        } else {
            Dew.cluster.cache.del(IAMConstant.CACHE_APP_STATUS_ENABLE + appId);
        }
    }

    public void changeAppIdent(AppIdent appIdent, Long relAppId, Long relTenantId) {
        Dew.cluster.cache.del(IAMConstant.CACHE_APP_AK + appIdent.getAk());
        if (appIdent.getValidTime() == null) {
            Dew.cluster.cache.set(IAMConstant.CACHE_APP_AK + appIdent.getAk(), appIdent.getSk() + ":" + relTenantId + ":" + relAppId);
        } else {
            Dew.cluster.cache.setex(IAMConstant.CACHE_APP_AK + appIdent.getAk(), appIdent.getSk() + ":" + relTenantId + ":" + relAppId,
                    (appIdent.getValidTime().getTime() - System.currentTimeMillis()) / 1000);
        }
    }

    public void deleteAppIdent(String ak) {
        Dew.cluster.cache.del(IAMConstant.CACHE_APP_AK + ak);
    }

}
