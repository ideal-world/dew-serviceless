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

package idealworld.dew.serviceless.iam.process.systemconsole;

import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.serviceless.iam.domain.ident.Tenant;
import idealworld.dew.serviceless.iam.process.systemconsole.dto.TenantAddReq;

/**
 * 系统控制台下的租户控制器.
 *
 * @author gudaoxuri
 */
public class SCTenantProcessor {

    {
        // 添加租户
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/system/tenant", addTenant());
    }

    public ProcessFun<Long> addTenant() {
        return context -> {
            var tenantAddReqR = context.helper.parseBody(context.req.body, TenantAddReq.class);
            if (!tenantAddReqR.ok()) {
                return context.helper.error(tenantAddReqR);
            }
            var tenant = Tenant.builder()
                    .name(tenantAddReqR.getBody().getName())
                    .icon(tenantAddReqR.getBody().getIcon())
                    .parameters(tenantAddReqR.getBody().getParameters())
                    .allowAccountRegister(tenantAddReqR.getBody().getAllowAccountRegister())
                    .status(CommonStatus.ENABLED)
                    .build();
            return context.fun.sql.insert(tenant, context);
        };
    }

}
