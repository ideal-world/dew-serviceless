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

package idealworld.dew.serviceless.iam.process.tenantconsole;

import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.serviceless.iam.domain.ident.Tenant;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant.TenantModifyReq;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant.TenantResp;

/**
 * 租户控制台下的租户控制器.
 *
 * @author gudaoxuri
 */
public class TCTenantProcessor {

    {
        // 修改当前租户
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/tenant", modifyTenant());
        // 获取当前租户信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/tenant", getTenant());
    }

    public ProcessFun<Long> modifyTenant() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var tenantModifyReqR = context.helper.parseBody(context.req.body, TenantModifyReq.class);
            if (!tenantModifyReqR.ok()) {
                return context.helper.error(tenantModifyReqR);
            }
            var tenant = context.helper.convert(tenantModifyReqR.getBody(),Tenant.class);
            return context.fun.sql.updateById(relTenantId,tenant,context)
                    .compose(resp -> {
                        if (tenantModifyReqR.getBody().getStatus() != null) {
                            if (tenantModifyReqR.getBody().getStatus() == CommonStatus.ENABLED) {
                                exchangeProcessor.enableTenant(relTenantId);
                            } else {
                                exchangeProcessor.disableTenant(relTenantId);
                            }
                        }
                        return context.helper.success(resp);
                    });
        };
    }

    public ProcessFun<TenantResp> getTenant() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            return context.helper.convertFuture(context.fun.sql.fetchById(relTenantId,Tenant.class,context), TenantResp.class);
        };
    }

}
