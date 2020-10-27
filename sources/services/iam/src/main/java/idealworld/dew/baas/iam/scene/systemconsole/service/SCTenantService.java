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

package idealworld.dew.baas.iam.scene.systemconsole.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import idealworld.dew.baas.common.enumeration.CommonStatus;
import idealworld.dew.baas.iam.domain.ident.Tenant;
import idealworld.dew.baas.iam.scene.common.service.IAMBasicService;
import idealworld.dew.baas.iam.scene.systemconsole.dto.TenantAddReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统控制台下的租户服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class SCTenantService extends IAMBasicService {

    @Transactional
    public Resp<Long> addTenant(TenantAddReq tenantAddReq) {
        var tenant = $.bean.copyProperties(tenantAddReq, Tenant.class);
        tenant.setStatus(CommonStatus.ENABLED);
        return saveEntity(tenant);
    }

}
