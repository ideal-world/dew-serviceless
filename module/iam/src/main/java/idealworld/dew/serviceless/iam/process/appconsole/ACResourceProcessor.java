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

package idealworld.dew.serviceless.iam.process.appconsole;

import com.ecfront.dew.common.$;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.auth.dto.ResourceExchange;
import idealworld.dew.framework.fun.auth.dto.ResourceSubjectExchange;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.domain.auth.AuthPolicy;
import idealworld.dew.serviceless.iam.domain.auth.Resource;
import idealworld.dew.serviceless.iam.domain.auth.ResourceSubject;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.process.appconsole.dto.resource.*;
import io.vertx.core.Future;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用控制台下的资源控制器.
 *
 * @author gudaoxuri
 */
public class ACResourceProcessor {

    {
        // 添加当前应用的资源主体
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/app/resource/subject", addResourceSubject());
        // 修改当前应用的某个资源主体
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/app/resource/subject/{resourceSubjectId}", modifyResourceSubject());
        // 获取当前应用的某个资源主体信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/resource/subject/{resourceSubjectId}", getResourceSubject());
        // 获取当前应用的资源主体列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/resource/subject", findResourceSubjects());
        // 删除当前应用的某个资源主体
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/app/resource/subject/{resourceSubjectId}", deleteResourceSubject());

        // 添加当前应用的资源
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/app/resource", addResource());
        // 修改当前应用的某个资源
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/app/resource/{resourceId}", modifyResource());
        // 获取当前应用的某个资源信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/resource/{resourceId}", getResource());
        // 获取当前应用的资源列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/resource", findResources());
        // 删除当前应用的某个资源
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/app/resource/{resourceId}", deleteResource());
    }


    public ProcessFun<Long> addResourceSubject() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var resourceSubjectAddReq = context.req.body(ResourceSubjectAddReq.class);
            var resourceCode = relAppId + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT
                    + resourceSubjectAddReq.getKind().toString().toLowerCase() + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT
                    + resourceSubjectAddReq.getCodePostfix();
            resourceSubjectAddReq.setUri(URIHelper.formatUri(resourceSubjectAddReq.getUri()));
            return context.helper.existToError(
                    context.fun.sql.exists(
                            new HashMap<>() {
                                {
                                    put("code", resourceCode);
                                    put("rel_app_id", relAppId);
                                    put("rel_tenant_id", relTenantId);
                                }
                            },
                            ResourceSubject.class), () -> new ConflictException("资源主体编码已存在"))
                    .compose(resp ->
                            context.helper.existToError(
                                    context.fun.sql.exists(
                                            new HashMap<>() {
                                                {
                                                    put("uri", resourceSubjectAddReq.getUri());
                                                    put("rel_app_id", relAppId);
                                                    put("rel_tenant_id", relTenantId);
                                                }
                                            },
                                            ResourceSubject.class), () -> new ConflictException("资源主体URI已存在")))
                    .compose(resp -> {
                        var resourceSubject = context.helper.convert(resourceSubjectAddReq, ResourceSubject.class);
                        resourceSubject.setCode(resourceCode);
                        resourceSubject.setRelTenantId(relTenantId);
                        resourceSubject.setRelAppId(relAppId);
                        return context.fun.sql.save(resourceSubject)
                                .compose(saveResourceSubjectId -> {
                                    sendMQBySave(
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
                                                    .timeoutMS(resourceSubject.getTimeoutMS())
                                                    .build(), String.class, Object.class));
                                    return context.helper.success(saveResourceSubjectId);
                                });
                    });
        };
    }

    public ProcessFun<Void> modifyResourceSubject() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var resourceSubjectId = Long.parseLong(context.req.params.get("resourceSubjectId"));
            var resourceSubjectModifyReq = context.req.body(ResourceSubjectModifyReq.class);
            var future = Future.succeededFuture();
            if (resourceSubjectModifyReq.getCodePostfix() != null) {
                if (resourceSubjectModifyReq.getKind() == null) {
                    throw new BadRequestException("资源主体编码后缀与类型必须同时存在");
                }
                String resourceCode = relAppId + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT
                        + resourceSubjectModifyReq.getKind().toString().toLowerCase() + IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT
                        + resourceSubjectModifyReq.getCodePostfix();
                future
                        .compose(resp ->
                                context.helper.existToError(
                                        context.fun.sql.exists(
                                                new HashMap<>() {
                                                    {
                                                        put("!id", resourceSubjectId);
                                                        put("code", resourceCode);
                                                        put("rel_app_id", relAppId);
                                                        put("rel_tenant_id", relTenantId);
                                                    }
                                                },
                                                ResourceSubject.class), () -> new ConflictException("资源主体编码已存在")));
            }
            if (resourceSubjectModifyReq.getUri() != null) {
                resourceSubjectModifyReq.setUri(URIHelper.formatUri(resourceSubjectModifyReq.getUri()));
                future
                        .compose(resp ->
                                context.helper.existToError(
                                        context.fun.sql.exists(
                                                new HashMap<>() {
                                                    {
                                                        put("!id", resourceSubjectId);
                                                        put("uri", resourceSubjectModifyReq.getUri());
                                                        put("rel_app_id", relAppId);
                                                        put("rel_tenant_id", relTenantId);
                                                    }
                                                },
                                                ResourceSubject.class), () -> new ConflictException("资源主体URI已存在")));
            }
            return future
                    .compose(resp ->
                            context.fun.sql.update(
                                    new HashMap<>() {
                                        {
                                            put("id", resourceSubjectId);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    context.helper.convert(resourceSubjectModifyReq, ResourceSubject.class))
                    )
                    .compose(resp -> {
                        sendMQByUpdate(
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
                                        .timeoutMS(resourceSubject.getTimeoutMS())
                                        .build(), String.class, Object.class)
                        );
                        return context.helper.success();
                    });
        };
    }

    public ProcessFun<ResourceSubjectResp> getResourceSubject() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var resourceSubjectId = Long.parseLong(context.req.params.get("resourceSubjectId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", resourceSubjectId);
                            put("rel_app_id", relAppId);
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    ResourceSubject.class)
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
                                            .timeoutMS(resourceSubject.getTimeoutMS())
                                            .relAppId(resourceSubject.getRelAppId())
                                            .relTenantId(resourceSubject.getRelTenantId())
                                            .build()
                            )
                    );
        };
    }

    public ProcessFun<List<ResourceSubjectResp>> findResourceSubjects() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var code = context.req.params.getOrDefault("code", null);
            var name = context.req.params.getOrDefault("name", null);
            var kind = context.req.params.getOrDefault("kind", null);
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
                whereParameters.put("%kind", "%" + kind + "%");
            }
            return context.fun.sql.list(
                    whereParameters,
                    ResourceSubject.class)
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
                                                            .timeoutMS(resourceSubject.getTimeoutMS())
                                                            .relAppId(resourceSubject.getRelAppId())
                                                            .relTenantId(resourceSubject.getRelTenantId())
                                                            .build()
                                            )
                                            .collect(Collectors.toList())
                            )
                    );
        };
    }

    public ProcessFun<Void> deleteResourceSubject() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var resourceSubjectId = Long.parseLong(context.req.params.get("resourceSubjectId"));
            return context.helper.existToError(
                    context.fun.sql.exists(
                            new HashMap<>() {
                                {
                                    put("rel_resource_subject_id", resourceSubjectId);
                                }
                            },
                            Resource.class), () -> new ConflictException("请先删除关联的资源数据"))
                    .compose(resp ->
                            context.helper.notExistToError(
                                    context.fun.sql.exists(
                                            new HashMap<>() {
                                                {
                                                    put("id", resourceSubjectId);
                                                    put("rel_app_id", relAppId);
                                                    put("rel_tenant_id", relTenantId);
                                                }
                                            },
                                            ResourceSubject.class), () -> new BadRequestException("资源主题不存在")))
                    .compose(resp ->
                            context.fun.sql.softDelete(
                                    new HashMap<>() {
                                        {
                                            put("id", resourceSubjectId);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    ResourceSubject.class)
                                    .compose(r -> {
                                        sendMQByDelete(
                                                deleteR,
                                                ResourceSubject.class.getSimpleName().toLowerCase() + "." + resourceSubject.getKind().toString().toLowerCase(),
                                                resourceSubjectId,
                                                new HashMap<>() {
                                                    {
                                                        put("code", resourceSubject.getCode());
                                                    }
                                                });
                                        return context.helper.success();
                                    })
                    );
        };
    }

    // --------------------------------------------------------------------

    public ProcessFun<Long> addResource() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var resourceAddReq = context.req.body(ResourceAddReq.class);
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", resourceAddReq.getRelResourceSubjectId());
                            put("rel_app_id", relAppId);
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    ResourceSubject.class)
                    .compose(resourceSubject -> {
                        resourceAddReq.setPathAndQuery(URIHelper.formatUri(resourceSubject.getUri(), resourceAddReq.getPathAndQuery()));
                        return context.helper.existToError(
                                context.fun.sql.exists(
                                        new HashMap<>() {
                                            {
                                                put("uri", resourceAddReq.getPathAndQuery());
                                                put("rel_app_id", relAppId);
                                                put("rel_tenant_id", relTenantId);
                                            }
                                        },
                                        Resource.class), () -> new ConflictException("资源URI已存在"));
                    })
                    .compose(resp -> {
                        if (resourceAddReq.getParentId() != DewConstant.OBJECT_UNDEFINED) {
                            return context.helper.notExistToError(
                                    context.fun.sql.exists(
                                            new HashMap<>() {
                                                {
                                                    put("id", resourceAddReq.getParentId());
                                                    put("rel_app_id", relAppId);
                                                    put("rel_tenant_id", relTenantId);
                                                }
                                            },
                                            Resource.class), () -> new UnAuthorizedException("资源所属组Id不合法"));
                        } else {
                            return context.helper.success(true);
                        }
                    })
                    .compose(resp -> {
                        var resource = context.helper.convert(resourceAddReq, Resource.class);
                        resource.setUri(resourceAddReq.getPathAndQuery());
                        resource.setRelTenantId(relTenantId);
                        resource.setRelAppId(relAppId);
                        return context.fun.sql.save(resource)
                                .compose(r -> {
                                    sendMQBySave(
                                            saveEntity(resource),
                                            Resource.class,
                                            $.json.toMap(ResourceExchange.builder()
                                                    .resourceActionKind(resource.getAction())
                                                    .resourceUri(resource.getUri())
                                                    .build(), String.class, Object.class)
                                    );
                                    return context.helper.success();
                                });
                    });
        };
    }

    public ProcessFun<Void> modifyResource() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var resourceId = Long.parseLong(context.req.params.get("resourceId"));
            var resourceModifyReq = context.req.body(ResourceModifyReq.class);
            var future = Future.succeededFuture();
            if (resourceModifyReq.getPathAndQuery() != null) {
                future
                        .compose(resp ->
                                context.fun.sql.getOne(
                                        String.format("SELECT subject.uri FROM %s AS resource" +
                                                        "  INNER JOIN %s AS subject ON subject.id = resource.rel_resource_subject_id" +
                                                        "  WHERE resource.id = #{id}",
                                                new Resource().tableName(), new ResourceSubject().tableName()),
                                        new HashMap<>() {
                                            {
                                                put("id", resourceId);
                                            }
                                        })
                                        .compose(fetchResourceSubjectUri ->
                                                context.helper.existToError(
                                                        context.fun.sql.exists(
                                                                new HashMap<>() {
                                                                    {
                                                                        put("id", resourceId);
                                                                        put("uri", fetchResourceSubjectUri.getString("subject.uri"));
                                                                        put("rel_app_id", relAppId);
                                                                        put("rel_tenant_id", relTenantId);
                                                                    }
                                                                },
                                                                Resource.class), () -> new ConflictException("资源URI已存在"))
                                                        .compose(r -> {
                                                            resourceModifyReq.setPathAndQuery(URIHelper.formatUri(fetchResourceSubjectUri.getString("subject.uri"), resourceModifyReq.getPathAndQuery()));
                                                            return context.helper.success();
                                                        }))
                        );
            }
            if (resourceModifyReq.getParentId() != DewConstant.OBJECT_UNDEFINED) {
                future.compose(resp ->
                        context.helper.notExistToError(
                                context.fun.sql.exists(
                                        new HashMap<>() {
                                            {
                                                put("id", resourceModifyReq.getParentId());
                                                put("rel_app_id", relAppId);
                                                put("rel_tenant_id", relTenantId);
                                            }
                                        },
                                        Resource.class), () -> new UnAuthorizedException("资源所属组Id不合法")));
            }
            return future.compose(resp -> {
                var resource = context.helper.convert(resourceModifyReq, Resource.class);
                resource.setUri(resourceModifyReq.getPathAndQuery());
                return context.fun.sql.update(
                        new HashMap<>() {
                            {
                                put("id", resourceId);
                                put("rel_app_id", relAppId);
                                put("rel_tenant_id", relTenantId);
                            }
                        },
                        resource)
                        .compose(resp -> {
                            sendMQByUpdate(
                                    updateR,
                                    Resource.class,
                                    resourceId,
                                    $.json.toMap(ResourceExchange.builder()
                                            .resourceActionKind(resource.getAction())
                                            .resourceUri(resource.getUri())
                                            .build(), String.class, Object.class));
                            return context.helper.success();
                        });
            });
        };
    }

    public ProcessFun<ResourceResp> getResource() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var resourceId = Long.parseLong(context.req.params.get("resourceId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", resourceId);
                            put("rel_app_id", relAppId);
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    Resource.class)
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
        };
    }

    public ProcessFun<List<ResourceResp>> findResources() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var name = context.req.params.getOrDefault("name", null);
            var uri = context.req.params.getOrDefault("uri", null);
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
            return context.fun.sql.list(
                    whereParameters,
                    Resource.class)
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
        };
    }

    public ProcessFun<List<ResourceResp>> findExposeResources() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var name = context.req.params.getOrDefault("name", null);
            var uri = context.req.params.getOrDefault("uri", null);
            var sql = "SELECT * FROM %s" +
                    "  WHERE ( expose_kind = #{expose_kind_tenant} AND rel_tenant_id = #{rel_tenant_id}" +
                    "    OR expose_kind = #{expose_kind_global} )";
            var whereParameters = new HashMap<String, Object>() {
                {
                    put("expose_kind_tenant", ExposeKind.TENANT);
                    put("rel_tenant_id", relTenantId);
                    put("expose_kind_global", ExposeKind.GLOBAL);
                }
            };
            if (name != null && !name.isBlank()) {
                sql += " AND name = #{name}";
                whereParameters.put("%name", "%" + name + "%");
            }
            if (uri != null && !uri.isBlank()) {
                sql += " AND uri = #{uri}";
                whereParameters.put("%uri", "%" + uri + "%");
            }
            return context.fun.sql.list(
                    String.format(sql,new Resource().tableName()),
                    whereParameters,
                    Resource.class)
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
        };
    }

    public ProcessFun<Void> deleteResource() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var resourceId = Long.parseLong(context.req.params.get("resourceId"));
            return context.helper.existToError(
                    context.fun.sql.exists(
                            new HashMap<>() {
                                {
                                    put("rel_resource_id", resourceId);
                                }
                            },
                            AuthPolicy.class), () -> new ConflictException("请先删除关联的权限策略数据"))
                    .compose(resp ->
                            context.fun.sql.softDelete(
                                    new HashMap<>() {
                                        {
                                            put("id", resourceId);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    Resource.class)
                                    .compose(r -> {
                                        sendMQByDelete(
                                                softDelEntity(sqlBuilder
                                                        .selectFrom(qResource)
                                                        .where(qResource.id.in(deleteResourceIds))
                                                        .where(qResource.relTenantId.eq(relTenantId))
                                                        .where(qResource.relAppId.eq(relAppId))),
                                                Resource.class,
                                                deleteResourceIds
                                        );
                                        return context.helper.success();
                                    })
                    );
        };
    }

    private Future<List<Long>> findResourceAndGroups(Long resourceParentId, Long relAppId, Long relTenantId, ProcessContext context) {
        TODO 递归封装
        /*var qResource = QResource.resource;
        return sqlBuilder
                .select(qResource.id)
                .from(qResource)
                .where(qResource.parentId.eq(resourceParentId))
                .where(qResource.relTenantId.eq(relTenantId))
                .where(qResource.relAppId.eq(relAppId))
                .fetch()
                .stream()
                .flatMap(resId -> findResourceAndGroups(resId, relAppId, relTenantId).stream())
                .collect(Collectors.toList());*/
    }

    public static Future<Long> getTenantAdminRoleResourceId(ProcessContext context) {
        return context.fun.sql.getOne(
                String.format("SELECT id FROM %s" +
                                "  WHERE uri = #{uri}" +
                                "   ORDER BY create_time ASC",
                        new Resource().tableName()),
                new HashMap<>() {
                    {
                        put("uri", "http://" + context.moduleName + "/console/tenant/**");
                    }
                })
                .compose(fetchResourceId -> Future.succeededFuture(fetchResourceId.getLong("id")));
    }

    public static Future<Long> getAppAdminRoleResourceId(ProcessContext context) {
        return context.fun.sql.getOne(
                String.format("SELECT id FROM %s" +
                                "  WHERE uri = #{uri}" +
                                "   ORDER BY create_time ASC",
                        new Resource().tableName()),
                new HashMap<>() {
                    {
                        put("uri", "http://" + context.moduleName + "/console/app/**");
                    }
                })
                .compose(fetchResourceId -> Future.succeededFuture(fetchResourceId.getLong("id")));
    }

}
