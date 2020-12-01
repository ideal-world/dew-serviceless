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

package idealworld.dew.serviceless.iam.scene.appconsole.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.exchange.ResourceExchange;
import idealworld.dew.serviceless.common.dto.exchange.ResourceSubjectExchange;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.common.util.URIHelper;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.domain.auth.*;
import idealworld.dew.serviceless.iam.enumeration.ExposeKind;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.*;
import idealworld.dew.serviceless.iam.scene.common.service.IAMBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
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

    @Autowired
    private IAMConfig iamConfig;

    @Transactional
    public Resp<Long> addResourceSubject(ResourceSubjectAddReq resourceSubjectAddReq, Long relAppId, Long relTenantId) {
        var qResourceSubject = QResourceSubject.resourceSubject;
        resourceSubjectAddReq.setUri(URIHelper.formatUri(resourceSubjectAddReq.getUri()));
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
        return sendMQBySave(
                saveEntity(resourceSubject),
                ResourceSubject.class.getSimpleName().toLowerCase() + "." + resourceSubject.getKind().toString().toLowerCase(),
                $.json.toMap(ResourceSubjectExchange.builder()
                        .code(resourceSubject.getCode())
                        .name(resourceSubject.getName())
                        .kind(resourceSubject.getKind())
                        .uri(resourceSubject.getUri())
                        .ak(resourceSubject.getAk())
                        .sk(resourceSubject.getSk())
                        .platformAccount(resourceSubject.getPlatformAccount())
                        .platformProjectId(resourceSubject.getPlatformProjectId())
                        .build(), String.class, Object.class)
        );
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
        if (resourceSubjectModifyReq.getUri() != null) {
            resourceSubjectModifyReq.setUri(URIHelper.formatUri(resourceSubjectModifyReq.getUri()));
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
        if (resourceSubjectModifyReq.getSort() != null) {
            resourceSubjectUpdate.set(qResourceSubject.sort, resourceSubjectModifyReq.getSort());
        }
        if (resourceSubjectModifyReq.getDefaultByApp() != null) {
            resourceSubjectUpdate.set(qResourceSubject.defaultByApp, resourceSubjectModifyReq.getDefaultByApp());
        }
        if (resourceSubjectModifyReq.getKind() != null) {
            resourceSubjectUpdate.set(qResourceSubject.kind, resourceSubjectModifyReq.getKind());
        }
        if (resourceSubjectModifyReq.getUri() != null) {
            resourceSubjectUpdate.set(qResourceSubject.uri, resourceSubjectModifyReq.getUri());
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
        var updateR = updateEntity(resourceSubjectUpdate);
        if (!updateR.ok()) {
            return updateR;
        }
        var resourceSubject = sqlBuilder
                .selectFrom(qResourceSubject)
                .where(qResourceSubject.id.eq(resourceSubjectId))
                .fetchOne();
        return sendMQByUpdate(
                updateR,
                ResourceSubject.class.getSimpleName().toLowerCase() + "." + resourceSubject.getKind().toString().toLowerCase(),
                resourceSubjectId,
                $.json.toMap(ResourceSubjectExchange.builder()
                        .code(resourceSubject.getCode())
                        .name(resourceSubject.getName())
                        .kind(resourceSubject.getKind())
                        .uri(resourceSubject.getUri())
                        .ak(resourceSubject.getAk())
                        .sk(resourceSubject.getSk())
                        .platformAccount(resourceSubject.getPlatformAccount())
                        .platformProjectId(resourceSubject.getPlatformProjectId())
                        .build(), String.class, Object.class)
        );
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
                qResourceSubject.relAppId,
                qResourceSubject.relTenantId))
                .from(qResourceSubject)
                .where(qResourceSubject.id.eq(resourceSubjectId))
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId)));
    }

    public Resp<List<ResourceSubjectResp>> findResourceSubjects(Long relAppId, Long relTenantId) {
        var qResourceSubject = QResourceSubject.resourceSubject;
        return findDTOs(sqlBuilder.select(Projections.bean(ResourceSubjectResp.class,
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
                qResourceSubject.relAppId,
                qResourceSubject.relTenantId))
                .from(qResourceSubject)
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId))
                .orderBy(qResourceSubject.sort.asc()));
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
        var resourceSubject = sqlBuilder
                .selectFrom(qResourceSubject)
                .where(qResourceSubject.id.eq(resourceSubjectId))
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId))
                .fetchOne();
        if (resourceSubject == null) {
            return Resp.badRequest("资源主题不存在");
        }
        var deleteR = softDelEntity(sqlBuilder
                .selectFrom(qResourceSubject)
                .where(qResourceSubject.id.eq(resourceSubjectId))
                .where(qResourceSubject.relTenantId.eq(relTenantId))
                .where(qResourceSubject.relAppId.eq(relAppId)));
        if (!deleteR.ok()) {
            return deleteR;
        }
        return sendMQByDelete(
                deleteR,
                ResourceSubject.class.getSimpleName().toLowerCase() + "." + resourceSubject.getKind().toString().toLowerCase(),
                resourceSubjectId,
                new HashMap<>() {
                    {
                        put("code", resourceSubject.getCode());
                    }
                }
        );
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addResource(ResourceAddReq resourceAddReq, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        resourceAddReq.setUri(URIHelper.formatUri(resourceAddReq.getUri()));
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
        return sendMQBySave(
                saveEntity(resource),
                Resource.class,
                $.json.toMap(ResourceExchange.builder()
                        .resourceActionKind(resource.getAction())
                        .resourceUri(resource.getUri())
                        .build(), String.class, Object.class)
        );
    }

    @Transactional
    public Resp<Void> modifyResource(Long resourceId, ResourceModifyReq resourceModifyReq, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        if (resourceModifyReq.getUri() != null) {
            resourceModifyReq.setUri(URIHelper.formatUri(resourceModifyReq.getUri()));
        }
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
        var updateR = updateEntity(resourceUpdate);
        if (!updateR.ok()) {
            return updateR;
        }
        var resource = sqlBuilder
                .selectFrom(qResource)
                .where(qResource.id.eq(resourceId))
                .fetchOne();
        return sendMQByUpdate(
                updateR,
                Resource.class,
                resourceId,
                $.json.toMap(ResourceExchange.builder()
                        .resourceActionKind(resource.getAction())
                        .resourceUri(resource.getUri())
                        .build(), String.class, Object.class)
        );
    }

    public Resp<ResourceResp> getResource(Long resourceId, Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        return getDTO(sqlBuilder.select(Projections.bean(ResourceResp.class,
                qResource.id,
                qResource.name,
                qResource.uri,
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
                .where(qResource.id.eq(resourceId))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId)));
    }

    public Resp<List<ResourceResp>> findResources(Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        return findDTOs(sqlBuilder.select(Projections.bean(ResourceResp.class,
                qResource.id,
                qResource.name,
                qResource.uri,
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
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .orderBy(qResource.sort.asc()));
    }

    public Resp<List<ResourceResp>> findExposeResources(Long relAppId, Long relTenantId) {
        var qResource = QResource.resource;
        return findDTOs(sqlBuilder.select(Projections.bean(ResourceResp.class,
                qResource.id,
                qResource.name,
                qResource.uri,
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
                .where((qResource.exposeKind.eq(ExposeKind.TENANT).and(qResource.relTenantId.eq(relTenantId)))
                        .or(qResource.exposeKind.eq(ExposeKind.GLOBAL)))
                .where(qResource.relAppId.ne(relAppId))
                .orderBy(qResource.sort.asc()));
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
        return sendMQByDelete(
                softDelEntity(sqlBuilder
                        .selectFrom(qResource)
                        .where(qResource.id.in(deleteResourceIds))
                        .where(qResource.relTenantId.eq(relTenantId))
                        .where(qResource.relAppId.eq(relAppId))),
                Resource.class,
                deleteResourceIds
        );
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

    public Resp<Long> getTenantAdminRoleResourceId() {
        var qResource = QResource.resource;
        return Resp.success(sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.uri.eq(iamConfig.getServiceUrl() + "/console/tenant/**"))
                .orderBy(qResource.createTime.asc())
                .fetchOne());
    }

    public Resp<Long> getAppAdminRoleResourceId() {
        var qResource = QResource.resource;
        return Resp.success(sqlBuilder.select(qResource.id)
                .from(qResource)
                .where(qResource.uri.eq(iamConfig.getServiceUrl() + "/console/app/**"))
                .orderBy(qResource.createTime.asc())
                .fetchOne());
    }

}