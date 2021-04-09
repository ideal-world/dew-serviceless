/*
 * Copyright 2021. gudaoxuri
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

package idealworld.dew.serviceless.iam.process.appconsole;

import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.exception.NotFoundException;
import idealworld.dew.framework.fun.auth.dto.ResourceExchange;
import idealworld.dew.framework.fun.auth.dto.ResourceSubjectExchange;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.domain.auth.AuthPolicy;
import idealworld.dew.serviceless.iam.domain.auth.Resource;
import idealworld.dew.serviceless.iam.domain.auth.ResourceSubject;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.process.IAMBasicProcessor;
import idealworld.dew.serviceless.iam.process.appconsole.dto.resource.*;
import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用控制台下的资源控制器.
 *
 * @author gudaoxuri
 */
public class ACResourceProcessor extends EventBusProcessor {

    {
        // 添加当前应用的资源主体
        addProcessor(OptActionKind.CREATE, "/console/app/resource/subject", eventBusContext ->
                addResourceSubject(eventBusContext.req.body(ResourceSubjectAddReq.class), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前应用的某个资源主体
        addProcessor(OptActionKind.PATCH, "/console/app/resource/subject/{resourceSubjectId}", eventBusContext ->
                modifyResourceSubject(Long.parseLong(eventBusContext.req.params.get("resourceSubjectId")),
                        eventBusContext.req.body(ResourceSubjectModifyReq.class), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的某个资源主体信息
        addProcessor(OptActionKind.FETCH, "/console/app/resource/subject/{resourceSubjectId}", eventBusContext ->
                getResourceSubject(Long.parseLong(eventBusContext.req.params.get("resourceSubjectId")), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的资源主体列表信息
        addProcessor(OptActionKind.FETCH, "/console/app/resource/subject", eventBusContext ->
                findResourceSubjects(eventBusContext.req.params.getOrDefault("code", null), eventBusContext.req.params.getOrDefault("name", null),
                        eventBusContext.req.params.getOrDefault("kind", null), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 删除当前应用的某个资源主体
        addProcessor(OptActionKind.DELETE, "/console/app/resource/subject/{resourceSubjectId}", eventBusContext ->
                deleteResourceSubject(Long.parseLong(eventBusContext.req.params.get("resourceSubjectId")),
                        eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));

        // 添加当前应用的资源
        addProcessor(OptActionKind.CREATE, "/console/app/resource", eventBusContext ->
                addResource(eventBusContext.req.body(ResourceAddReq.class), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前应用的某个资源
        addProcessor(OptActionKind.PATCH, "/console/app/resource/{resourceId}", eventBusContext ->
                modifyResource(Long.parseLong(eventBusContext.req.params.get("resourceId")), eventBusContext.req.body(ResourceModifyReq.class),
                        eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的某个资源信息
        addProcessor(OptActionKind.FETCH, "/console/app/resource/{resourceId}", eventBusContext ->
                getResource(Long.parseLong(eventBusContext.req.params.get("resourceId")), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的资源列表信息
        addProcessor(OptActionKind.FETCH, "/console/app/resource", eventBusContext -> {
            if (eventBusContext.req.params.getOrDefault("expose", "false").equalsIgnoreCase("false")) {
                return findResources(eventBusContext.req.params.getOrDefault("name", null), eventBusContext.req.params.getOrDefault("uri", null),
                        eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context);
            } else {
                return findExposeResources(eventBusContext.req.params.getOrDefault("name", null), eventBusContext.req.params.getOrDefault("uri",
                        null), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context);
            }
        });
        // 删除当前应用的某个资源
        addProcessor(OptActionKind.DELETE, "/console/app/resource/{resourceId}", eventBusContext ->
                deleteResource(Long.parseLong(eventBusContext.req.params.get("resourceId")), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
    }

    public ACResourceProcessor(String moduleName) {
        super(moduleName);
    }

    public static Future<Long> addResourceSubject(ResourceSubjectAddReq resourceSubjectAddReq, Long relAppId, Long relTenantId,
                                                  ProcessContext context) {
        return IAMBasicProcessor.getAppCodeById(relAppId, relTenantId, context)
                .compose(appCode -> {
                    var resourceCode = appCode + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT
                            + resourceSubjectAddReq.getKind().toString().toLowerCase() + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT
                            + resourceSubjectAddReq.getCodePostfix();
                    resourceSubjectAddReq.setUri(URIHelper.formatUri(resourceSubjectAddReq.getUri()));
                    return context.helper.existToError(
                            context.sql.exists(
                                    ResourceSubject.class,
                                    new HashMap<>() {
                                        {
                                            put("code", resourceCode);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    }),
                            () -> new ConflictException("资源主体编码已存在"))
                            .compose(resp -> {
                                var resourceSubject = context.helper.convert(resourceSubjectAddReq, ResourceSubject.class);
                                resourceSubject.setCode(resourceCode);
                                resourceSubject.setRelTenantId(relTenantId);
                                resourceSubject.setRelAppId(relAppId);
                                return context.sql.save(resourceSubject);
                            })
                            .compose(resourceSubjectId -> context.sql.getOne(ResourceSubject.class, resourceSubjectId))
                            .compose(resourceSubject -> {
                                ExchangeProcessor.publish(
                                        OptActionKind.CREATE,
                                        ResourceSubject.class.getSimpleName().toLowerCase() + "." + resourceSubject.getKind(),
                                        resourceSubject.getId(),
                                        ResourceSubjectExchange.builder()
                                                .code(resourceSubject.getCode())
                                                .name(resourceSubject.getName())
                                                .kind(resourceSubject.getKind())
                                                .uri(resourceSubject.getUri())
                                                .ak(resourceSubject.getAk())
                                                .sk(resourceSubject.getSk())
                                                .platformAccount(resourceSubject.getPlatformAccount())
                                                .platformProjectId(resourceSubject.getPlatformProjectId())
                                                .timeoutMs(resourceSubject.getTimeoutMs())
                                                .build(),
                                        context);
                                return context.helper.success(resourceSubject.getId());
                            });
                });
    }

    public static Future<Void> modifyResourceSubject(Long resourceSubjectId, ResourceSubjectModifyReq resourceSubjectModifyReq, Long relAppId,
                                                     Long relTenantId, ProcessContext context) {
        var future = Future.succeededFuture();
        if (resourceSubjectModifyReq.getUri() != null) {
            resourceSubjectModifyReq.setUri(URIHelper.formatUri(resourceSubjectModifyReq.getUri()));
        }
        var resourceSubject = context.helper.convert(resourceSubjectModifyReq, ResourceSubject.class);
        if (resourceSubjectModifyReq.getCodePostfix() != null) {
            if (resourceSubjectModifyReq.getKind() == null) {
                throw new BadRequestException("资源主体编码后缀与类型必须同时存在");
            }
            future =
                    IAMBasicProcessor.getAppCodeById(relAppId, relTenantId, context)
                            .compose(appCode -> {
                                var resourceSubjectCode = appCode + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT
                                        + resourceSubjectModifyReq.getKind().toString().toLowerCase() + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT
                                        + resourceSubjectModifyReq.getCodePostfix();
                                resourceSubject.setCode(resourceSubjectCode);
                                return context.helper.existToError(
                                        context.sql.exists(
                                                ResourceSubject.class,
                                                new HashMap<>() {
                                                    {
                                                        put("!id", resourceSubjectId);
                                                        put("code", resourceSubjectCode);
                                                        put("rel_app_id", relAppId);
                                                        put("rel_tenant_id", relTenantId);
                                                    }
                                                }),
                                        () -> new ConflictException("资源主体编码已存在"));
                            })
                            .compose(resp -> context.helper.success(null));
        }
        return future
                .compose(resp ->
                        context.sql.update(
                                resourceSubject,
                                new HashMap<>() {
                                    {
                                        put("id", resourceSubjectId);
                                        put("rel_app_id", relAppId);
                                        put("rel_tenant_id", relTenantId);
                                    }
                                })
                )
                .compose(resp -> context.sql.getOne(ResourceSubject.class, resourceSubjectId))
                .compose(storedResourceSubject -> {
                    ExchangeProcessor.publish(
                            OptActionKind.MODIFY,
                            ResourceSubject.class.getSimpleName().toLowerCase() + "." + storedResourceSubject.getKind(),
                            storedResourceSubject.getId(),
                            ResourceSubjectExchange.builder()
                                    .code(storedResourceSubject.getCode())
                                    .name(storedResourceSubject.getName())
                                    .kind(storedResourceSubject.getKind())
                                    .uri(storedResourceSubject.getUri())
                                    .ak(storedResourceSubject.getAk())
                                    .sk(storedResourceSubject.getSk())
                                    .platformAccount(storedResourceSubject.getPlatformAccount())
                                    .platformProjectId(storedResourceSubject.getPlatformProjectId())
                                    .timeoutMs(storedResourceSubject.getTimeoutMs())
                                    .build(),
                            context);
                    return context.helper.success();
                });
    }

    public static Future<ResourceSubjectResp> getResourceSubject(Long resourceSubjectId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                ResourceSubject.class,
                new HashMap<>() {
                    {
                        put("id", resourceSubjectId);
                        put("rel_app_id", relAppId);
                        put("rel_tenant_id", relTenantId);
                    }
                })
                .compose(resourceSubject ->
                        context.helper.success(
                                ResourceSubjectResp.builder()
                                        .id(resourceSubject.getId())
                                        .code(resourceSubject.getCode())
                                        .codePostfix(resourceSubject.getCode().split("\\" + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT)[2])
                                        .name(resourceSubject.getName())
                                        .sort(resourceSubject.getSort())
                                        .kind(resourceSubject.getKind())
                                        .uri(resourceSubject.getUri())
                                        .ak(resourceSubject.getAk())
                                        .sk(resourceSubject.getSk())
                                        .platformAccount(resourceSubject.getPlatformAccount())
                                        .platformProjectId(resourceSubject.getPlatformProjectId())
                                        .timeoutMs(resourceSubject.getTimeoutMs())
                                        .relAppId(resourceSubject.getRelAppId())
                                        .relTenantId(resourceSubject.getRelTenantId())
                                        .build()));
    }

    public static Future<List<ResourceSubjectResp>> findResourceSubjects(String code, String name, String kind, Long relAppId, Long relTenantId,
                                                                         ProcessContext context) {
        var whereParameters = new HashMap<String, Object>() {
            {
                put("rel_tenant_id", relTenantId);
                put("rel_app_id", relAppId);
            }
        };
        if (code != null && !code.isBlank()) {
            whereParameters.put("%code", "%" + code + "%");
        }
        if (name != null && !name.isBlank()) {
            whereParameters.put("%name", "%" + name + "%");
        }
        if (kind != null && !kind.isBlank()) {
            whereParameters.put("kind", kind);
        }
        return context.sql.list(ResourceSubject.class, whereParameters)
                .compose(resourceSubjects ->
                        context.helper.success(
                                resourceSubjects.stream()
                                        .map(resourceSubject ->
                                                ResourceSubjectResp.builder()
                                                        .id(resourceSubject.getId())
                                                        .code(resourceSubject.getCode())
                                                        .codePostfix(resourceSubject.getCode().split("\\" + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT)[2])
                                                        .name(resourceSubject.getName())
                                                        .sort(resourceSubject.getSort())
                                                        .kind(resourceSubject.getKind())
                                                        .uri(resourceSubject.getUri())
                                                        .ak(resourceSubject.getAk())
                                                        .sk(resourceSubject.getSk())
                                                        .platformAccount(resourceSubject.getPlatformAccount())
                                                        .platformProjectId(resourceSubject.getPlatformProjectId())
                                                        .timeoutMs(resourceSubject.getTimeoutMs())
                                                        .relAppId(resourceSubject.getRelAppId())
                                                        .relTenantId(resourceSubject.getRelTenantId())
                                                        .build()
                                        )
                                        .collect(Collectors.toList())));
    }

    public static Future<Void> deleteResourceSubject(Long resourceSubjectId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.helper.existToError(
                context.sql.exists(
                        Resource.class,
                        new HashMap<>() {
                            {
                                put("rel_resource_subject_id", resourceSubjectId);
                            }
                        }),
                () -> new ConflictException("请先删除关联的资源数据"))
                .compose(resp ->
                        context.helper.notExistToError(
                                context.sql.getOne(
                                        ResourceSubject.class,
                                        new HashMap<String,Object>() {
                                            {
                                                put("id", resourceSubjectId);
                                                put("rel_app_id", relAppId);
                                                put("rel_tenant_id", relTenantId);
                                            }
                                        }),
                                () -> new NotFoundException("找不到对应的资源主体")))
                .compose(storedResourceSubject ->
                        context.sql.softDelete(
                                ResourceSubject.class,
                                new HashMap<>() {
                                    {
                                        put("id", resourceSubjectId);
                                        put("rel_app_id", relAppId);
                                        put("rel_tenant_id", relTenantId);
                                    }
                                })
                                .compose(resp -> context.helper.success(storedResourceSubject)))
                .compose(storedResourceSubject -> {
                    ExchangeProcessor.publish(
                            OptActionKind.DELETE,
                            ResourceSubject.class.getSimpleName().toLowerCase() + "." + storedResourceSubject.getKind(),
                            storedResourceSubject.getId(),
                            ResourceSubjectExchange.builder()
                                    .code(storedResourceSubject.getCode())
                                    .name(storedResourceSubject.getName())
                                    .kind(storedResourceSubject.getKind())
                                    .uri(storedResourceSubject.getUri())
                                    .ak(storedResourceSubject.getAk())
                                    .sk(storedResourceSubject.getSk())
                                    .platformAccount(storedResourceSubject.getPlatformAccount())
                                    .platformProjectId(storedResourceSubject.getPlatformProjectId())
                                    .timeoutMs(storedResourceSubject.getTimeoutMs())
                                    .build(),
                            context
                    );
                    return context.helper.success();
                });
    }

    // --------------------------------------------------------------------

    public static Future<Long> addResource(ResourceAddReq resourceAddReq, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                ResourceSubject.class,
                new HashMap<>() {
                    {
                        put("id", resourceAddReq.getRelResourceSubjectId());
                        put("rel_app_id", relAppId);
                        put("rel_tenant_id", relTenantId);
                    }
                })
                .compose(resourceSubject -> {
                    resourceAddReq.setPathAndQuery(
                            URIHelper.formatUri(
                                    resourceSubject.getKind().toString().toLowerCase() + "://" + resourceSubject.getCode(),
                                    resourceAddReq.getPathAndQuery()));
                    return context.helper.existToError(
                            context.sql.exists(
                                    Resource.class,
                                    new HashMap<>() {
                                        {
                                            put("uri", resourceAddReq.getPathAndQuery());
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    }),
                            () -> new ConflictException("资源URI已存在"))
                            .compose(resp -> {
                                if (resourceAddReq.getParentId() != DewConstant.OBJECT_UNDEFINED) {
                                    return context.helper.notExistToError(
                                            context.sql.exists(
                                                    Resource.class,
                                                    new HashMap<>() {
                                                        {
                                                            put("id", resourceAddReq.getParentId());
                                                            put("rel_app_id", relAppId);
                                                            put("rel_tenant_id", relTenantId);
                                                        }
                                                    }),
                                            () -> new NotFoundException("找不到对应的资源所属组"));
                                } else {
                                    return context.helper.success(true);
                                }
                            })
                            .compose(resp -> {
                                var resource = context.helper.convert(resourceAddReq, Resource.class);
                                resource.setUri(resourceAddReq.getPathAndQuery());
                                resource.setRelTenantId(relTenantId);
                                resource.setRelAppId(relAppId);
                                return context.sql.save(resource);
                            })
                            .compose(resourceId -> context.sql.getOne(Resource.class, resourceId))
                            .compose(storedResource -> {
                                ExchangeProcessor.publish(
                                        OptActionKind.CREATE,
                                        Resource.class.getSimpleName().toLowerCase() + "." + resourceSubject.getKind(),
                                        storedResource.getId(),
                                        ResourceExchange.builder()
                                                .actionKind(storedResource.getAction())
                                                .uri(storedResource.getUri())
                                                .build(),
                                        context);
                                return context.helper.success(storedResource.getId());
                            });
                });
    }

    public static Future<Void> modifyResource(Long resourceId, ResourceModifyReq resourceModifyReq, Long relAppId, Long relTenantId,
                                              ProcessContext context) {
        var future = Future.succeededFuture();
        if (resourceModifyReq.getPathAndQuery() != null) {
            future
                    .compose(resp ->
                            context.helper.notExistToError(
                                    context.sql.getOne(
                                            "SELECT subject.code, subject.kind, subject.uri FROM %s AS resource" +
                                                    " INNER JOIN %s AS subject ON subject.id = resource.rel_resource_subject_id" +
                                                    " WHERE resource.id = ? AND resource.rel_app_id = ? AND resource" +
                                                    ".rel_tenant_id = ?",
                                            Resource.class, ResourceSubject.class, resourceId, relAppId, relTenantId),
                                    () -> new NotFoundException("找不到对应的资源主体"))
                                    .compose(fetchResourceSubject ->
                                            context.helper.existToError(
                                                    context.sql.exists(
                                                            Resource.class,
                                                            new HashMap<>() {
                                                                {
                                                                    put("!id", resourceId);
                                                                    put("uri",
                                                                            URIHelper.formatUri(
                                                                                    fetchResourceSubject.getString("kind").toLowerCase()
                                                                                            + "://" + fetchResourceSubject.getString("code"),
                                                                                    resourceModifyReq.getPathAndQuery()));
                                                                    put("rel_app_id", relAppId);
                                                                    put("rel_tenant_id", relTenantId);
                                                                }
                                                            }),
                                                    () -> new ConflictException("资源URI已存在"))
                                                    .compose(r -> {
                                                        resourceModifyReq.setPathAndQuery(
                                                                URIHelper.formatUri(fetchResourceSubject.getString("kind").toLowerCase()
                                                                                + "://" + fetchResourceSubject.getString("code"),
                                                                        resourceModifyReq.getPathAndQuery()));
                                                        return context.helper.success();
                                                    }))
                    );
        }
        if (resourceModifyReq.getParentId() != null && resourceModifyReq.getParentId() != DewConstant.OBJECT_UNDEFINED) {
            future.compose(resp ->
                    context.helper.notExistToError(
                            context.sql.exists(
                                    Resource.class,
                                    new HashMap<>() {
                                        {
                                            put("id", resourceModifyReq.getParentId());
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    }),
                            () -> new NotFoundException("找不到对应的资源所属组")));
        }
        return future.compose(resp -> {
            var resource = context.helper.convert(resourceModifyReq, Resource.class);
            resource.setUri(resourceModifyReq.getPathAndQuery());
            return context.sql.update(
                    resource,
                    new HashMap<>() {
                        {
                            put("id", resourceId);
                            put("rel_app_id", relAppId);
                            put("rel_tenant_id", relTenantId);
                        }
                    });
        })
                .compose(resp -> context.sql.getOne(Resource.class, resourceId))
                .compose(storedResource ->
                        context.sql.getOne(ResourceSubject.class, storedResource.getRelResourceSubjectId())
                                .compose(resourceSubject -> {
                                    ExchangeProcessor.publish(
                                            OptActionKind.MODIFY,
                                            Resource.class.getSimpleName().toLowerCase() + "." + resourceSubject.getKind(),
                                            storedResource.getId(),
                                            ResourceExchange.builder()
                                                    .actionKind(storedResource.getAction())
                                                    .uri(storedResource.getUri())
                                                    .build(),
                                            context);
                                    return context.helper.success();
                                })
                );
    }

    public static Future<ResourceResp> getResource(Long resourceId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                Resource.class,
                new HashMap<>() {
                    {
                        put("id", resourceId);
                        put("rel_app_id", relAppId);
                        put("rel_tenant_id", relTenantId);
                    }
                })
                .compose(resource ->
                        context.helper.success(
                                ResourceResp.builder()
                                        .id(resource.getId())
                                        .name(resource.getName())
                                        .pathAndQuery(URIHelper.getPathAndQuery(resource.getUri()))
                                        .icon(resource.getIcon())
                                        .action(resource.getAction())
                                        .sort(resource.getSort())
                                        .resGroup(resource.getResGroup())
                                        .parentId(resource.getParentId())
                                        .relResourceSubjectId(resource.getRelResourceSubjectId())
                                        .exposeKind(resource.getExposeKind())
                                        .relAppId(resource.getRelAppId())
                                        .relTenantId(resource.getRelTenantId())
                                        .build()
                        )
                );
    }

    public static Future<List<ResourceResp>> findResources(String name, String uri, Long relAppId, Long relTenantId, ProcessContext context) {
        var whereParameters = new HashMap<String, Object>() {
            {
                put("rel_tenant_id", relTenantId);
                put("rel_app_id", relAppId);
            }
        };
        if (name != null && !name.isBlank()) {
            whereParameters.put("%name", "%" + name + "%");
        }
        if (uri != null && !uri.isBlank()) {
            whereParameters.put("%uri", "%" + uri + "%");
        }
        return context.sql.list(Resource.class, whereParameters)
                .compose(resources ->
                        context.helper.success(
                                resources.stream()
                                        .map(resource ->
                                                ResourceResp.builder()
                                                        .id(resource.getId())
                                                        .name(resource.getName())
                                                        .pathAndQuery(URIHelper.getPathAndQuery(resource.getUri()))
                                                        .icon(resource.getIcon())
                                                        .action(resource.getAction())
                                                        .sort(resource.getSort())
                                                        .resGroup(resource.getResGroup())
                                                        .parentId(resource.getParentId())
                                                        .relResourceSubjectId(resource.getRelResourceSubjectId())
                                                        .exposeKind(resource.getExposeKind())
                                                        .relAppId(resource.getRelAppId())
                                                        .relTenantId(resource.getRelTenantId())
                                                        .build()
                                        )
                                        .collect(Collectors.toList())
                        )
                );
    }

    public static Future<List<ResourceResp>> findExposeResources(String name, String uri, Long relAppId, Long relTenantId, ProcessContext context) {
        var sql = "SELECT * FROM %s" +
                " WHERE (expose_kind = ? AND rel_tenant_id = ? OR expose_kind = ?)";
        var parameters = new ArrayList<>();
        parameters.add(Resource.class);
        parameters.add(ExposeKind.TENANT);
        parameters.add(relTenantId);
        parameters.add(ExposeKind.GLOBAL);
        if (name != null && !name.isBlank()) {
            sql += " AND name = ?";
            parameters.add("%" + name + "%");
        }
        if (uri != null && !uri.isBlank()) {
            sql += " AND uri = ?";
            parameters.add("%" + uri + "%");
        }
        return context.sql.list(Resource.class, sql, parameters)
                .compose(resources ->
                        context.helper.success(
                                resources.stream()
                                        .map(resource ->
                                                ResourceResp.builder()
                                                        .id(resource.getId())
                                                        .name(resource.getName())
                                                        .pathAndQuery(URIHelper.getPathAndQuery(resource.getUri()))
                                                        .icon(resource.getIcon())
                                                        .action(resource.getAction())
                                                        .sort(resource.getSort())
                                                        .resGroup(resource.getResGroup())
                                                        .parentId(resource.getParentId())
                                                        .relResourceSubjectId(resource.getRelResourceSubjectId())
                                                        .exposeKind(resource.getExposeKind())
                                                        .relAppId(resource.getRelAppId())
                                                        .relTenantId(resource.getRelTenantId())
                                                        .build()
                                        )
                                        .collect(Collectors.toList())
                        )
                );
    }

    public static Future<Void> deleteResource(Long resourceId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.helper.existToError(
                context.sql.exists(
                        AuthPolicy.class,
                        new HashMap<>() {
                            {
                                put("rel_resource_id", resourceId);
                            }
                        }),
                () -> new ConflictException("请先删除关联的权限策略数据"))
                .compose(resp ->
                        context.helper.notExistToError(
                                context.sql.getOne(
                                        Resource.class,
                                        new HashMap<>() {
                                            {
                                                put("id", resourceId);
                                                put("rel_app_id", relAppId);
                                                put("rel_tenant_id", relTenantId);
                                            }
                                        }),
                                () -> new NotFoundException("找不到对应的资源"))
                                .compose(storedResource ->
                                        context.sql.softDelete(
                                                Resource.class,
                                                new HashMap<>() {
                                                    {
                                                        put("id", resourceId);
                                                        put("rel_app_id", relAppId);
                                                        put("rel_tenant_id", relTenantId);
                                                    }
                                                })
                                                .compose(r -> context.helper.success(storedResource)))
                                .compose(storedResource ->
                                        context.sql.getOne(ResourceSubject.class, storedResource.getRelResourceSubjectId())
                                                .compose(resourceSubject -> {
                                                    ExchangeProcessor.publish(
                                                            OptActionKind.DELETE,
                                                            Resource.class.getSimpleName().toLowerCase() + "." + resourceSubject.getKind(),
                                                            storedResource.getId(),
                                                            ResourceExchange.builder()
                                                                    .actionKind(storedResource.getAction())
                                                                    .uri(storedResource.getUri())
                                                                    .build(),
                                                            context);
                                                    return context.helper.success();
                                                })));
    }

    private Future<List<Long>> findResourceAndGroups(Long resourceParentId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.helper.findWithRecursion(resourceParentId, id -> doFindResourceAndGroups(id, relAppId, relTenantId, context));
    }

    private Future<List<Long>> doFindResourceAndGroups(Long resourceParentId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.list(
                "SELECT id FROM %s" +
                        " WHERE parent_id = ? AND rel_tenant_id = ? AND rel_app_id = ?",
                Resource.class, resourceParentId, relTenantId, relAppId)
                .compose(fetchResourceIds -> context.helper.success(
                        fetchResourceIds.stream()
                                .map(id -> id.getLong("id"))
                                .collect(Collectors.toList())
                ));
    }

}
