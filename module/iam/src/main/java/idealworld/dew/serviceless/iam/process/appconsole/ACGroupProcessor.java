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

import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.domain.auth.AccountGroup;
import idealworld.dew.serviceless.iam.domain.auth.AuthPolicy;
import idealworld.dew.serviceless.iam.domain.auth.Group;
import idealworld.dew.serviceless.iam.domain.auth.GroupNode;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.process.appconsole.dto.group.*;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 应用控制台下的群组控制器.
 *
 * @author gudaoxuri
 */
public class ACGroupProcessor {

    {
        // 添加当前应用的群组
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/app/group", addGroup());
        // 修改当前应用的某个群组
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/app/group/{groupId}", modifyGroup());
        // 获取当前应用的某个群组信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/group/{groupId}", getGroup());
        // 获取当前应用的群组列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/group", findGroups());
        // 删除当前应用的某个群组
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/app/group/{groupId}", deleteGroup());

        // 添加当前应用某个群组的节点
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/app/group/{groupId}/node", addGroupNode());
        // 修改当前应用某个群组的节点
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/app/group/{groupId}/node/{groupNodeId}", modifyGroupNode());
        // 获取当前应用某个群组的节点列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/group/{groupId}/node", findGroupNodes());
        // 删除当前应用某个群组的节点
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/app/group/{groupId}/node/{groupNodeId}", deleteGroupNode());
    }

    public ProcessFun<Long> addGroup() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupAddReq = context.helper.parseBody(context.req.body, GroupAddReq.class);
            return context.fun.sql.exists(
                    new HashMap<>() {
                        {
                            put("rel_tenant_id", relTenantId);
                            put("rel_app_id", relAppId);
                            put("code", groupAddReq.getCode());
                        }
                    },
                    Group.class,
                    context)
                    .compose(existsGroup -> {
                        if (existsGroup) {
                            throw new ConflictException("群组编码已存在");
                        }
                        var group = context.helper.convert(groupAddReq, Group.class);
                        group.setRelTenantId(relTenantId);
                        group.setRelAppId(relAppId);
                        return context.fun.sql.save(group, context);
                    });
        };
    }

    public ProcessFun<Void> modifyGroup() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupId = Long.parseLong(context.req.params.get("groupId"));
            var groupModifyReq = context.helper.parseBody(context.req.body, GroupModifyReq.class);
            var future = Future.succeededFuture();
            if (groupModifyReq.getCode() != null) {
                future
                        .compose(resp ->
                                context.fun.sql.exists(
                                        new HashMap<>() {
                                            {
                                                put("rel_tenant_id", relTenantId);
                                                put("rel_app_id", relAppId);
                                                put("code", groupModifyReq.getCode());
                                                put("-id", groupId);
                                            }
                                        },
                                        Group.class,
                                        context)
                                        .compose(existsGroup -> {
                                            if (existsGroup) {
                                                throw new ConflictException("群组编码已存在");
                                            }
                                            return Future.succeededFuture();
                                        }));
            }
            return future
                    .compose(resp ->
                            context.fun.sql.update(
                                    new HashMap<>() {
                                        {
                                            put("rel_tenant_id", relTenantId);
                                            put("rel_app_id", relAppId);
                                            put("id", groupId);
                                        }
                                    },
                                    context.helper.convert(groupModifyReq, Group.class),
                                    context)
                    );

        };
    }

    public ProcessFun<GroupResp> getGroup() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupId = Long.parseLong(context.req.params.get("groupId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("rel_tenant_id", relTenantId);
                            put("rel_app_id", relAppId);
                            put("id", groupId);
                        }
                    },
                    Group.class,
                    context
            )
                    .compose(group -> context.helper.success(group, GroupResp.class));
        };
    }

    public ProcessFun<List<GroupResp>> findGroups() {
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
                    Group.class,
                    context
            )
                    .compose(groups -> context.helper.success(groups, GroupResp.class));
        };
    }

    public ProcessFun<List<GroupResp>> findExposeGroups() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var code = context.req.params.getOrDefault("code", null);
            var name = context.req.params.getOrDefault("name", null);
            var kind = context.req.params.getOrDefault("kind", null);
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
            if (code != null && !code.isBlank()) {
                sql += " AND code = #{code}";
                whereParameters.put("%code", "%" + code + "%");
            }
            if (name != null && !name.isBlank()) {
                sql += " AND name = #{name}";
                whereParameters.put("%name", "%" + name + "%");
            }
            if (kind != null && !kind.isBlank()) {
                sql += " AND kind = #{kind}";
                whereParameters.put("%kind", "%" + kind + "%");
            }
            return context.fun.sql.list(sql,
                    whereParameters,
                    Group.class,
                    context
            )
                    .compose(groups -> context.helper.success(groups, GroupResp.class));
        };
    }

    public ProcessFun<Void> deleteGroup() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupId = Long.parseLong(context.req.params.get("groupId"));
            return context.fun.sql.exists(
                    new HashMap<>() {
                        {
                            put("rel_group_id", groupId);
                        }
                    },
                    GroupNode.class,
                    context)
                    .compose(existsGroup -> {
                        if (existsGroup) {
                            throw new ConflictException("请先删除关联的群组节点数据");
                        }
                        return context.fun.sql.softDelete(
                                new HashMap<>() {
                                    {
                                        put("id", groupId);
                                        put("rel_tenant_id", relTenantId);
                                        put("rel_app_id", relAppId);
                                    }
                                },
                                Group.class,
                                context);
                    });
        };
    }

    // --------------------------------------------------------------------

    public ProcessFun<Long> addGroupNode() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupId = Long.parseLong(context.req.params.get("groupId"));
            var groupNodeAddReq = context.helper.parseBody(context.req.body, GroupNodeAddReq.class);
            return context.fun.sql.tx(client ->
                    client.exists(
                            new HashMap<>() {
                                {
                                    put("id", groupId);
                                    put("rel_tenant_id", relTenantId);
                                    put("rel_app_id", relAppId);
                                }
                            },
                            Group.class,
                            context)
                            .compose(existsGroup -> {
                                if (!existsGroup) {
                                    throw new ConflictException("关联群组不合法");
                                }
                                return packageGroupNodeCode(groupId, groupNodeAddReq.getParentId(), groupNodeAddReq.getSiblingId(), null,
                                        (IAMConfig) context.getConf(), client, context)
                                        .compose(fetchGroupNodeCode ->
                                                client.exists(
                                                        new HashMap<>() {
                                                            {
                                                                put("rel_group_id", groupId);
                                                                put("code", fetchGroupNodeCode);
                                                            }
                                                        },
                                                        GroupNode.class,
                                                        context
                                                )
                                                        .compose(existsGroupNode -> {
                                                            if (existsGroupNode) {
                                                                throw new ConflictException("群组节点编码已存在");
                                                            }
                                                            var groupNode = context.helper.convert(groupNodeAddReq, GroupNode.class);
                                                            groupNode.setRelGroupId(groupId);
                                                            groupNode.setCode(fetchGroupNodeCode);
                                                            return client.save(groupNode, context);
                                                        })
                                        );

                            })
            );
        };
    }

    public ProcessFun<Void> modifyGroupNode() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupNodeId = Long.parseLong(context.req.params.get("groupNodeId"));
            var groupNodeModifyReq = context.helper.parseBody(context.req.body, GroupNodeModifyReq.class);
            var groupNodeSets = context.helper.convert(groupNodeModifyReq, GroupNode.class);
            return context.fun.sql.tx(client ->
                    client.getOne(
                            "SELECT node.* FROM %s AS node" +
                                    "  INNER JOIN %s AS group ON group.id = node.rel_group_id" +
                                    "  WHERE node.id = #{id} AND group.rel_tenant_id = #{rel_tenant_id} AND group.rel_app_id = #{rel_app_id}",
                            new HashMap<>() {
                                {
                                    put("id", groupNodeId);
                                    put("rel_tenant_id", relTenantId);
                                    put("rel_app_id", relAppId);
                                }
                            },
                            GroupNode.class,
                            context,
                            new GroupNode().tableName(), new Group().tableName()
                    )
                            .compose(fetchGroupNode -> {
                                if (fetchGroupNode == null) {
                                    throw new UnAuthorizedException("关联群组不合法");
                                }
                                if (groupNodeModifyReq.getParentId() != DewConstant.OBJECT_UNDEFINED || groupNodeModifyReq.getSiblingId() != DewConstant.OBJECT_UNDEFINED) {
                                    return packageGroupNodeCode(fetchGroupNode.getRelGroupId(), groupNodeModifyReq.getParentId(),
                                            groupNodeModifyReq.getSiblingId(), fetchGroupNode.getCode(), (IAMConfig) context.conf, client, context)
                                            .compose(fetchGroupNodeCode ->
                                                    client.exists(
                                                            new HashMap<>() {
                                                                {
                                                                    put("rel_group_id", fetchGroupNode.getRelGroupId());
                                                                    put("code", fetchGroupNodeCode);
                                                                    put("-id", groupNodeId);
                                                                }
                                                            },
                                                            GroupNode.class,
                                                            context
                                                    )
                                                            .compose(existsGroupNode -> {
                                                                if (existsGroupNode) {
                                                                    throw new ConflictException("群组节点编码已存在");
                                                                }
                                                                groupNodeSets.setCode(fetchGroupNodeCode);
                                                                return Future.succeededFuture();
                                                            })
                                            );
                                } else {
                                    return Future.succeededFuture();
                                }
                            })
                            .compose(resp -> client.update(groupNodeId, groupNodeSets, context))
            );
        };
    }

    public ProcessFun<List<GroupNodeResp>> findGroupNodes() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupId = context.req.params.get("groupId");
            return context.fun.sql.list(
                    "SELECT node.* FROM %s AS node" +
                            "  INNER JOIN %s AS group ON group.id = node.rel_group_id" +
                            "  WHERE group.rel_tenant_id = #{rel_tenant_id} AND group.rel_app_id = #{rel_app_id} AND node.rel_group_id = #{rel_group_id}" +
                            "  ORDER BY node.code ASC",
                    new HashMap<>() {
                        {
                            put("rel_tenant_id", relTenantId);
                            put("rel_app_id", relAppId);
                            put("rel_group_id", groupId);
                        }
                    },
                    GroupNode.class,
                    context,
                    new GroupNode().tableName(), new Group().tableName()
            )
                    .compose(fetchGroupNodes -> {
                        var nodeLength = ((IAMConfig) context.conf).getApp().getInitNodeCode().length();
                        List<GroupNodeResp> roleNodeRespList = fetchGroupNodes.stream()
                                .map(node -> {
                                    var parentCode = node.getCode().substring(0, node.getCode().length() - nodeLength);
                                    var parentId = parentCode.isEmpty()
                                            ? DewConstant.OBJECT_UNDEFINED
                                            : fetchGroupNodes.stream().filter(n -> n.getCode().equals(parentCode)).findAny().get().getId();
                                    return GroupNodeResp.builder()
                                            .id(node.getId())
                                            .busCode(node.getBusCode())
                                            .name(node.getName())
                                            .parameters(node.getParameters())
                                            .relGroupId(node.getRelGroupId())
                                            .parentId(parentId)
                                            .build();
                                })
                                .collect(Collectors.toList());
                        return Future.succeededFuture(roleNodeRespList);
                    });
        };
    }

    public ProcessFun<Void> deleteGroupNode() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupNodeId = Long.parseLong(context.req.params.get("groupNodeId"));
            return context.fun.sql.tx(client ->
                    client.exists(
                            new HashMap<>() {
                                {
                                    put("rel_group_node_id", groupNodeId);
                                }
                            },
                            AccountGroup.class,
                            context)
                            .compose(existsGroupByAccountGroup -> {
                                if (existsGroupByAccountGroup) {
                                    throw new ConflictException("请先删除关联的账号群组数据");
                                }
                                return client.exists(
                                        new HashMap<>() {
                                            {
                                                put("rel_subject_kind", AuthSubjectKind.GROUP_NODE);
                                                put("%rel_subject_ids", "%" + groupNodeId + ",%");
                                            }
                                        },
                                        AuthPolicy.class,
                                        context);
                            })
                            .compose(existsGroupByPolicy -> {
                                if (existsGroupByPolicy) {
                                    throw new ConflictException("请先删除关联的权限策略数据");
                                }
                                return client.getOne(
                                        "SELECT node.* FROM %s AS node" +
                                                "  INNER JOIN %s group ON group.id = node.rel_group_id" +
                                                "  WHERE node.id = #{node_id} AND group.rel_tenant_id = #{rel_tenant_id} AND group.rel_app_id = #{rel_app_id}",
                                        new HashMap<>() {
                                            {
                                                put("node_id", groupNodeId);
                                                put("rel_tenant_id", relTenantId);
                                                put("rel_app_id", relAppId);
                                            }
                                        },
                                        GroupNode.class,
                                        context,
                                        new GroupNode().tableName(), new Group().tableName()
                                )
                                        .compose(fetchGroupNode ->
                                                client.softDelete(fetchGroupNode.getId(), GroupNode.class, context)
                                                        .compose(resp -> updateOtherGroupNodeCode(fetchGroupNode.getRelGroupId(), fetchGroupNode.getCode(), true, null,
                                                                (IAMConfig) context.conf, client, context))
                                        );
                            })
            );
        };
    }

    private static Future<String> packageGroupNodeCode(Long groupId, Long parentId, Long siblingId, String originalGroupNodeCode, IAMConfig config, FunSQLClient client, ProcessContext context) {
        if (parentId == DewConstant.OBJECT_UNDEFINED && siblingId == DewConstant.OBJECT_UNDEFINED) {
            var currentNodeCode = config.getApp().getInitNodeCode();
            return updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode, config, client, context)
                    .compose(resp -> Future.succeededFuture(currentNodeCode));
        }
        if (siblingId == DewConstant.OBJECT_UNDEFINED) {
            return client.getOne(
                    "SELECT code FROM %s" +
                            "  WHERE rel_group_id = #{rel_group_id} AND id = #{id}",
                    new HashMap<>() {
                        {
                            put("rel_group_id", groupId);
                            put("id", parentId);
                        }
                    },
                    context,
                    new GroupNode().tableName()
            )
                    .compose(fetchParentGroupNodeCode -> {
                        var currentNodeCode = fetchParentGroupNodeCode.getString("code") + config.getApp().getInitNodeCode();
                        return updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode, config, client, context)
                                .compose(resp -> Future.succeededFuture(currentNodeCode));
                    });
        }
        return client.getOne(
                "SELECT code FROM %s" +
                        "  WHERE id = #{id}",
                new HashMap<>() {
                    {
                        put("id", siblingId);
                    }
                },
                context,
                new GroupNode().tableName()
        )
                .compose(fetchSiblingCode -> {
                    var siblingCode = fetchSiblingCode.getString("code");
                    var currentNodeLength = siblingCode.length();
                    var parentLength = currentNodeLength - config.getApp().getInitNodeCode().length();
                    var parentCode = siblingCode.substring(0, parentLength);
                    var currentLevelCode = Long.parseLong(siblingCode.substring(parentLength));
                    var currentNodeCode = parentCode + (currentLevelCode + 1);
                    return updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode, config, client, context)
                            .compose(resp -> Future.succeededFuture(currentNodeCode));
                });
    }

    private static Future<Void> updateOtherGroupNodeCode(Long groupId, String currentGroupNodeCode, Boolean deleteOpt, String originalGroupNodeCode, IAMConfig config, FunSQLClient client, ProcessContext context) {
        var currentNodeLength = currentGroupNodeCode.length();
        var parentLength = currentNodeLength - config.getApp().getInitNodeCode().length();
        var parentNodeCode = currentGroupNodeCode.substring(0, parentLength);
        List<GroupNode> originalGroupNodes = new ArrayList<>();
        var offset = deleteOpt ? -1 : 1;
        var future = Future.succeededFuture();
        if (originalGroupNodeCode != null) {
            var originalParentLength = originalGroupNodeCode.length() - config.getApp().getInitNodeCode().length();
            var originalParentNodeCode = originalGroupNodeCode.substring(0, originalParentLength);
            future.compose(resp ->
                    client.list(
                            "SELECT * FROM %s" +
                                    "  WHERE rel_group_id = #{rel_group_id} AND code like #{code} AND code >= #{code_goe}",
                            new HashMap<>() {
                                {
                                    put("rel_group_id", groupId);
                                    put("code", originalParentNodeCode + "%");
                                    put("code_goe", originalGroupNodeCode);
                                }
                            },
                            GroupNode.class,
                            context,
                            new GroupNode().tableName()
                    ))
                    .compose(fetchGroupNodes -> {
                        originalGroupNodes.addAll(fetchGroupNodes);
                        return Future.succeededFuture();
                    });
        }
        return future.compose(resp -> {
            var updateSql = "SELECT * FROM %s" +
                    "  WHERE rel_group_id = #{rel_group_id} AND code like #{code} AND code >= #{code_goe}";
            var parameters = new HashMap<String, Object>() {
                {
                    put("rel_group_id", groupId);
                    put("code", parentNodeCode + "%");
                    put("code_goe", currentGroupNodeCode);
                }
            };
            if (!originalGroupNodes.isEmpty()) {
                var originalGroupNodesMap = IntStream.range(0, originalGroupNodes.size())
                        .mapToObj(i -> new Object[]{"ori_node_" + i, originalGroupNodes.get(i)})
                        .collect(Collectors.toMap(i -> (String) i[0], i -> i[1]));
                parameters.putAll(originalGroupNodesMap);
                updateSql += " AND id NOT IN (" + originalGroupNodesMap.keySet().stream().map(i -> "#{" + i + "}").collect(Collectors.joining(", ")) + ")";

            }
            // 排序，避免索引重复
            if (deleteOpt) {
                updateSql += " ORDER BY code ASC";
            } else {
                updateSql += " ORDER BY code DESC";
            }
            return client.list(
                    updateSql,
                    parameters,
                    GroupNode.class,
                    context,
                    new GroupNode().tableName()
            );
        })
                .compose(groupNodes ->
                        CompositeFuture.all(
                                groupNodes.stream()
                                        .map(node -> {
                                            var code = node.getCode();
                                            var currNodeCode = Long.parseLong(code.substring(parentLength, currentNodeLength));
                                            var childNodeCode = code.substring(currentNodeLength);
                                            node.setCode(parentNodeCode + (currNodeCode + offset) + childNodeCode);
                                            return client.update(node, context);
                                        })
                                        .collect(Collectors.toList())
                        )
                )
                .compose(resp -> {
                    if (originalGroupNodeCode != null) {
                        return CompositeFuture.all(
                                originalGroupNodes.stream()
                                        .map(node -> {
                                            var code = node.getCode();
                                            node.setCode(parentNodeCode + code.substring(parentLength));
                                            return client.update(node, context);
                                        })
                                        .collect(Collectors.toList())
                        )
                                .compose(r -> Future.succeededFuture());
                    } else {
                        return Future.succeededFuture();
                    }
                });
    }

}
