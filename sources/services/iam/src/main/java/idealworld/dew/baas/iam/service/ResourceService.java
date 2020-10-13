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
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.auth.QResource;
import idealworld.dew.baas.iam.domain.auth.QResourceSubject;
import idealworld.dew.baas.iam.domain.auth.Resource;
import idealworld.dew.baas.iam.domain.auth.ResourceSubject;
import idealworld.dew.baas.iam.dto.resouce.ResourceAddOrModifyReq;
import idealworld.dew.baas.iam.dto.resouce.ResourceResp;
import idealworld.dew.baas.iam.dto.resouce.ResourceSubjectAddOrModifyReq;
import idealworld.dew.baas.iam.dto.resouce.ResourceSubjectResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resource service.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class ResourceService extends IAMBasicService {

    private static final String BUSINESS_RESOURCE = "RESOURCE";
    private static final String BUSINESS_RESOURCE_SUBJECT = "RESOURCE_SUBJECT";

    @Transactional
    public Resp<Long> addResourceSubject(ResourceSubjectAddOrModifyReq resourceSubjectAddReq, Long relAppId, Long relTenantId) {
        var qResourceSubject = QResourceSubject.resourceSubject;
        if (sqlBuilder.select(qResourceSubject.id)
                .from(qResourceSubject)
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId))
                .where(qResourceSubject.code.eq(resourceSubjectAddReq.getCode()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE_SUBJECT, "资源主体编码已存在");
        }
        if (sqlBuilder.select(qResourceSubject.id)
                .from(qResourceSubject)
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId))
                .where(qResourceSubject.uri.eq(resourceSubjectAddReq.getUri()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE_SUBJECT, "资源主体URI已存在");
        }
        var resourceSubject = $.bean.copyProperties(resourceSubjectAddReq, ResourceSubject.class);
        resourceSubject.setRelTenantId(relTenantId);
        resourceSubject.setRelAppId(relAppId);
        return saveEntity(resourceSubject);
    }

    @Transactional
    public Resp<Long> modifyResourceSubject(Long resourceSubjectId, ResourceSubjectAddOrModifyReq resourceSubjectModifyReq, Long relAppId, Long relTenantId) {
        var qResourceSubject = QResourceSubject.resourceSubject;
        if (sqlBuilder.select(qResourceSubject.id)
                .from(qResourceSubject)
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId))
                .where(qResourceSubject.code.eq(resourceSubjectModifyReq.getCode()))
                .where(qResourceSubject.id.ne(resourceSubjectId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE_SUBJECT, "资源主体编码已存在");
        }
        if (sqlBuilder.select(qResourceSubject.id)
                .from(qResourceSubject)
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId))
                .where(qResourceSubject.uri.eq(resourceSubjectModifyReq.getUri()))
                .where(qResourceSubject.id.ne(resourceSubjectId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE_SUBJECT, "资源主体URI已存在");
        }
        var resourceSubject = $.bean.copyProperties(resourceSubjectModifyReq, ResourceSubject.class);
        resourceSubject.setId(resourceSubjectId);
        resourceSubject.setRelTenantId(relTenantId);
        resourceSubject.setRelAppId(relAppId);
        return updateEntity(resourceSubject);
    }

    public Resp<ResourceSubjectResp> getResourceSubject(Long resourceSubjectId, Long relAppId, Long relTenantId) {
        var qResourceSubject = QResourceSubject.resourceSubject;
        return getDTO(sqlBuilder.select(Projections.bean(ResourceSubjectResp.class,
                qResourceSubject.id,
                qResourceSubject.code,
                qResourceSubject.name,
                qResourceSubject.sort,
                qResourceSubject.defaultByApp,
                qResourceSubject.kind,
                qResourceSubject.uri,
                qResourceSubject.ak,
                qResourceSubject.sk,
                qResourceSubject.platformAccount,
                qResourceSubject.platformProjectId,
                qResourceSubject.exposeKind,
                qResourceSubject.relAppId,
                qResourceSubject.relTenantId))
                .from(qResourceSubject)
                .where(qResourceSubject.id.eq(resourceSubjectId))
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId)));
    }

    public Resp<Page<ResourceSubjectResp>> pageResourceSubject(Long pageNumber, Integer pageSize, Long relAppId, Long relTenantId) {
        var qResourceSubject = QResourceSubject.resourceSubject;
        return pageDTOs(sqlBuilder.select(Projections.bean(ResourceSubjectResp.class,
                qResourceSubject.id,
                qResourceSubject.code,
                qResourceSubject.name,
                qResourceSubject.sort,
                qResourceSubject.defaultByApp,
                qResourceSubject.kind,
                qResourceSubject.uri,
                qResourceSubject.ak,
                qResourceSubject.sk,
                qResourceSubject.platformAccount,
                qResourceSubject.platformProjectId,
                qResourceSubject.exposeKind,
                qResourceSubject.relAppId,
                qResourceSubject.relTenantId))
                .from(qResourceSubject)
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId)), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteResourceSubject(Long resourceSubjectId, Long relAppId, Long relTenantId) {
        var qResourceSubject = QResourceSubject.resourceSubject;
        // TODO 删除前检查
        return softDelEntity(sqlBuilder
                .selectFrom(qResourceSubject)
                .where(qResourceSubject.id.eq(resourceSubjectId))
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId)));
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addResource(ResourceAddOrModifyReq resourceAddReq, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        if (sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .where(qResource.uri.eq(resourceAddReq.getUri()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE, "资源URI已存在");
        }
        var resource = $.bean.copyProperties(resourceAddReq, Resource.class);
        resource.setRelTenantId(relTenantId);
        resource.setRelAppId(relAppId);
        return saveEntity(resource);
    }

    @Transactional
    public Resp<Long> modifyResource(Long resourceId, ResourceAddOrModifyReq resourceModifyReq, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        if (sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .where(qResource.uri.eq(resourceModifyReq.getUri()))
                .where(qResource.id.ne(resourceId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE, "资源URI已存在");
        }
        var resource = $.bean.copyProperties(resourceModifyReq, Resource.class);
        resource.setId(resourceId);
        resource.setRelTenantId(relTenantId);
        resource.setRelAppId(relAppId);
        return updateEntity(resource);
    }

    public Resp<ResourceResp> getResource(Long resourceId, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        return getDTO(sqlBuilder.select(Projections.bean(ResourceResp.class,
                qResource.id,
                qResource.name,
                qResource.uri,
                qResource.icon,
                qResource.name,
                qResource.action,
                qResource.sort,
                qResource.group,
                qResource.parentId,
                qResource.relResourceSubjectId,
                qResource.exposeKind,
                qResource.relAppId,
                qResource.relTenantId))
                .from(qResource)
                .where(qResource.id.eq(resourceId))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId)));
    }

    public Resp<Page<ResourceResp>> pageResource(Long pageNumber, Integer pageSize, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        return pageDTOs(sqlBuilder.select(Projections.bean(ResourceResp.class,
                qResource.id,
                qResource.name,
                qResource.uri,
                qResource.icon,
                qResource.name,
                qResource.action,
                qResource.sort,
                qResource.group,
                qResource.parentId,
                qResource.relResourceSubjectId,
                qResource.exposeKind,
                qResource.relAppId,
                qResource.relTenantId))
                .from(qResource)
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId)), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteResource(Long resourceId, Long relAppId, Long relTenantId) {
        var deleteResourceIds = findResourceAndGroup(resourceId, relAppId, relTenantId);
        deleteResourceIds.add(resourceId);
        var qResource = QResource.resource;
        // TODO 删除前检查
        return softDelEntity(sqlBuilder
                .selectFrom(qResource)
                .where(qResource.id.in(deleteResourceIds))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId)));
    }

    private List<Long> findResourceAndGroup(Long resourceParentId, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        return sqlBuilder
                .select(qResource.id)
                .from(qResource)
                .where(qResource.parentId.eq(resourceParentId))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .fetch()
                .stream()
                .flatMap(resId -> findResourceAndGroup(resId, relAppId, relTenantId).stream())
                .collect(Collectors.toList());
    }

    protected List<ResourceResp> findResourceByGroup(Long resourceGroupId, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        return sqlBuilder
                .select(Projections.bean(ResourceResp.class,
                        qResource.id,
                        qResource.name,
                        qResource.uri,
                        qResource.icon,
                        qResource.name,
                        qResource.action,
                        qResource.sort,
                        qResource.group,
                        qResource.parentId,
                        qResource.relResourceSubjectId,
                        qResource.exposeKind,
                        qResource.relAppId,
                        qResource.relTenantId))
                .from(qResource)
                .where(qResource.parentId.eq(resourceGroupId))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .fetch()
                .stream()
                .flatMap(res -> {
                    if (res.getGroup()) {
                        return findResourceByGroup(res.getId(), relAppId, relTenantId).stream();
                    } else {
                        return new ArrayList<ResourceResp>() {
                            {
                                add(res);
                            }
                        }.stream();
                    }
                })
                .collect(Collectors.toList());
    }

}
