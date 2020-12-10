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

package idealworld.dew.serviceless.iam.scene.exec.service;

import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.iam.domain.auth.QResource;
import idealworld.dew.serviceless.iam.domain.auth.QResourceSubject;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.ResourceResp;
import idealworld.dew.serviceless.iam.scene.common.service.IAMBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 资源操作执行服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class ExecService extends IAMBasicService {

    public Resp<List<ResourceResp>> findResources(ResourceKind kind, Long relAppId, Long relTenantId, Optional<IdentOptCacheInfo> identOptCacheInfo) {
        var qResource = QResource.resource;
        var qResourceSubject = QResourceSubject.resourceSubject;
        // TODO 根据 identOptCacheInfo 过滤
        var resourceQuery = sqlBuilder.select(Projections.bean(ResourceResp.class,
                qResource.id,
                qResource.name,
                qResource.uri.as("pathAndQuery"),
                qResource.icon,
                qResource.action,
                qResource.sort,
                qResource.resGroup,
                qResource.parentId,
                qResource.relResourceSubjectId,
                qResource.exposeKind,
                qResource.relAppId,
                qResource.relTenantId))
                .from(qResource)
                .innerJoin(qResourceSubject).on(qResourceSubject.id.eq(qResource.relResourceSubjectId))
                .where(
                        (qResource.relTenantId.eq(relTenantId).and(qResource.relAppId.eq(relAppId)))
                                .or(qResource.exposeKind.eq(ExposeKind.TENANT).and(qResource.relTenantId.eq(relTenantId)))
                                .or(qResource.exposeKind.eq(ExposeKind.GLOBAL)))
                .where(qResourceSubject.kind.eq(kind));
        return findDTOs(resourceQuery.orderBy(qResource.sort.asc()), obj -> {
            obj.setPathAndQuery(URIHelper.getPathAndQuery(obj.getPathAndQuery()));
            return obj;
        });
    }

}
