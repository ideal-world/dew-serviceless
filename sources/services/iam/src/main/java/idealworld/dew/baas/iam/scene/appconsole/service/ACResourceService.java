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
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.auth.*;
import idealworld.dew.baas.iam.scene.appconsole.dto.resource.*;
import idealworld.dew.baas.iam.scene.common.service.IAMBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用控制台下的资源服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class ACResourceService extends IAMBasicService {

    @Transactional
    public Resp<Long> addResourceSubject(ResourceSubjectAddReq resourceSubjectAddReq, Long relAppId, Long relTenantId) {
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
    public Resp<Void> modifyResourceSubject(Long resourceSubjectId, ResourceSubjectModifyReq resourceSubjectModifyReq, Long relAppId, Long relTenantId) {
        var qResourceSubject = QResourceSubject.resourceSubject;
        if (resourceSubjectModifyReq.getCode() != null && sqlBuilder.select(qResourceSubject.id)
                .from(qResourceSubject)
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId))
                .where(qResourceSubject.code.eq(resourceSubjectModifyReq.getCode()))
                .where(qResourceSubject.id.ne(resourceSubjectId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE_SUBJECT, "资源主体编码已存在");
        }
        if (resourceSubjectModifyReq.getUri() != null && sqlBuilder.select(qResourceSubject.id)
                .from(qResourceSubject)
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId))
                .where(qResourceSubject.uri.eq(resourceSubjectModifyReq.getUri()))
                .where(qResourceSubject.id.ne(resourceSubjectId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE_SUBJECT, "资源主体URI已存在");
        }
        var resourceSubjectUpdate = sqlBuilder.update(qResourceSubject)
                .where(qResourceSubject.id.eq(resourceSubjectId))
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId));
        if (resourceSubjectModifyReq.getCode() != null) {
            resourceSubjectUpdate.set(qResourceSubject.code, resourceSubjectModifyReq.getCode());
        }
        if (resourceSubjectModifyReq.getName() != null) {
            resourceSubjectUpdate.set(qResourceSubject.name, resourceSubjectModifyReq.getName());
        }
        if (resourceSubjectModifyReq.getName() != null) {
            resourceSubjectUpdate.set(qResourceSubject.name, resourceSubjectModifyReq.getName());
        }
        if (resourceSubjectModifyReq.getSort() != null) {
            resourceSubjectUpdate.set(qResourceSubject.sort, resourceSubjectModifyReq.getSort());
        }
        if (resourceSubjectModifyReq.getDefaultByApp() != null) {
            resourceSubjectUpdate.set(qResourceSubject.defaultByApp, resourceSubjectModifyReq.getDefaultByApp());
        }
        if (resourceSubjectModifyReq.getKind() != null) {
            resourceSubjectUpdate.set(qResourceSubject.kind, resourceSubjectModifyReq.getKind());
        }
        if (resourceSubjectModifyReq.getAk() != null) {
            resourceSubjectUpdate.set(qResourceSubject.ak, resourceSubjectModifyReq.getAk());
        }
        if (resourceSubjectModifyReq.getSk() != null) {
            resourceSubjectUpdate.set(qResourceSubject.sk, resourceSubjectModifyReq.getSk());
        }
        if (resourceSubjectModifyReq.getPlatformAccount() != null) {
            resourceSubjectUpdate.set(qResourceSubject.platformAccount, resourceSubjectModifyReq.getPlatformAccount());
        }
        if (resourceSubjectModifyReq.getPlatformProjectId() != null) {
            resourceSubjectUpdate.set(qResourceSubject.platformProjectId, resourceSubjectModifyReq.getPlatformProjectId());
        }
        if (resourceSubjectModifyReq.getExposeKind() != null) {
            resourceSubjectUpdate.set(qResourceSubject.exposeKind, resourceSubjectModifyReq.getExposeKind());
        }
        return updateEntity(resourceSubjectUpdate);
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

    public Resp<Page<ResourceSubjectResp>> pageResourceSubjects(Long pageNumber, Integer pageSize, Long relAppId, Long relTenantId) {
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
        var qResource = QResource.resource;
        if (sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.relResourceSubjectId.eq(resourceSubjectId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE_SUBJECT, "请先删除关联的资源数据");
        }
        var qResourceSubject = QResourceSubject.resourceSubject;
        return softDelEntity(sqlBuilder
                .selectFrom(qResourceSubject)
                .where(qResourceSubject.id.eq(resourceSubjectId))
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId)));
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addResource(ResourceAddReq resourceAddReq, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        if (sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .where(qResource.uri.eq(resourceAddReq.getUri()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE, "资源URI已存在");
        }
        if (resourceAddReq.getParentId() != Constant.OBJECT_UNDEFINED && sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.id.eq(resourceAddReq.getParentId()))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_RESOURCE, "资源所属组Id不合法");
        }
        var resource = $.bean.copyProperties(resourceAddReq, Resource.class);
        resource.setRelTenantId(relTenantId);
        resource.setRelAppId(relAppId);
        return saveEntity(resource);
    }

    @Transactional
    public Resp<Void> modifyResource(Long resourceId, ResourceModifyReq resourceModifyReq, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        if (resourceModifyReq.getUri() != null && sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .where(qResource.uri.eq(resourceModifyReq.getUri()))
                .where(qResource.id.ne(resourceId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE, "资源URI已存在");
        }
        if (resourceModifyReq.getParentId() != null && sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.id.eq(resourceModifyReq.getParentId()))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_RESOURCE, "资源所属组Id不合法");
        }
        var resourceUpdate = sqlBuilder.update(qResource)
                .where(qResource.id.eq(resourceId))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId));
        if (resourceModifyReq.getName() != null) {
            resourceUpdate.set(qResource.name, resourceModifyReq.getName());
        }
        if (resourceModifyReq.getUri() != null) {
            resourceUpdate.set(qResource.uri, resourceModifyReq.getUri());
        }
        if (resourceModifyReq.getIcon() != null) {
            resourceUpdate.set(qResource.icon, resourceModifyReq.getIcon());
        }
        if (resourceModifyReq.getAction() != null) {
            resourceUpdate.set(qResource.action, resourceModifyReq.getAction());
        }
        if (resourceModifyReq.getSort() != null) {
            resourceUpdate.set(qResource.sort, resourceModifyReq.getSort());
        }
        if (resourceModifyReq.getResGroup() != null) {
            resourceUpdate.set(qResource.resGroup, resourceModifyReq.getResGroup());
        }
        if (resourceModifyReq.getParentId() != null) {
            resourceUpdate.set(qResource.parentId, resourceModifyReq.getParentId());
        }
        if (resourceModifyReq.getExposeKind() != null) {
            resourceUpdate.set(qResource.exposeKind, resourceModifyReq.getExposeKind());
        }
        return updateEntity(resourceUpdate);
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
                qResource.resGroup,
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

    public Resp<Page<ResourceResp>> pageResources(Long pageNumber, Integer pageSize, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        return pageDTOs(sqlBuilder.select(Projections.bean(ResourceResp.class,
                qResource.id,
                qResource.name,
                qResource.uri,
                qResource.icon,
                qResource.name,
                qResource.action,
                qResource.sort,
                qResource.resGroup,
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
        var deleteResourceIds = findResourceAndGroups(resourceId, relAppId, relTenantId);
        deleteResourceIds.add(resourceId);
        var qAuthPolicy = QAuthPolicy.authPolicy;
        if (sqlBuilder.select(qAuthPolicy.id)
                .from(qAuthPolicy)
                .where(qAuthPolicy.relResourceId.in(deleteResourceIds))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_RESOURCE, "请先删除关联的权限策略数据");
        }
        var qResource = QResource.resource;
        return softDelEntity(sqlBuilder
                .selectFrom(qResource)
                .where(qResource.id.in(deleteResourceIds))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId)));
    }

    private List<Long> findResourceAndGroups(Long resourceParentId, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        return sqlBuilder
                .select(qResource.id)
                .from(qResource)
                .where(qResource.parentId.eq(resourceParentId))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .fetch()
                .stream()
                .flatMap(resId -> findResourceAndGroups(resId, relAppId, relTenantId).stream())
                .collect(Collectors.toList());
    }

    protected List<ResourceResp> findResourceByGroups(Long resourceGroupId, Long relAppId, Long relTenantId) {
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
                        qResource.resGroup,
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
                    if (res.getResGroup()) {
                        return findResourceByGroups(res.getId(), relAppId, relTenantId).stream();
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
