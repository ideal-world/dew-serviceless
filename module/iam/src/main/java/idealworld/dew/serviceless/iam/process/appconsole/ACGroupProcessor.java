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

    static {
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

    public static ProcessFun<Long> addGroup() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupAddReq = context.req.body(GroupAddReq.class);
            return context.helper.existToError(
                    context.fun.sql.exists(
                            new HashMap<>() {
                                {
                                    put("code", groupAddReq.getCode());
                                    put("rel_app_id", relAppId);
                                    put("rel_tenant_id", relTenantId);
                                }
                            },
                            Group.class), () -> new ConflictException("群组编码已存在"))
                    .compose(resp -> {
                        var group = context.helper.convert(groupAddReq, Group.class);
                        group.setRelTenantId(relTenantId);
                        group.setRelAppId(relAppId);
                        return context.fun.sql.save(group);
                    });
        };
    }

    public static ProcessFun<Void> modifyGroup() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupId = Long.parseLong(context.req.params.get("groupId"));
            var groupModifyReq = context.req.body(GroupModifyReq.class);
            var future = context.helper.success();
            if (groupModifyReq.getCode() != null) {
                future
                        .compose(resp ->
                                context.helper.existToError(
                                        context.fun.sql.exists(
                                                new HashMap<>() {
                                                    {
                                                        put("!id", groupId);
                                                        put("code", groupModifyReq.getCode());
                                                        put("rel_app_id", relAppId);
                                                        put("rel_tenant_id", relTenantId);
                                                    }
                                                },
                                                Group.class), () -> new ConflictException("群组编码已存在")));
            }
            return future
                    .compose(resp ->
                            context.fun.sql.update(
                                    new HashMap<>() {
                                        {
                                            put("id", groupId);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    context.helper.convert(groupModifyReq, Group.class)));

        };
    }

    public static ProcessFun<GroupResp> getGroup() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupId = Long.parseLong(context.req.params.get("groupId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", groupId);
                            put("rel_app_id", relAppId);
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    Group.class)
                    .compose(group -> context.helper.success(group, GroupResp.class));
        };
    }

    public static ProcessFun<List<GroupResp>> findGroups() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var code = context.req.params.getOrDefault("code", null);
            var name = context.req.params.getOrDefault("name", null);
            var kind = context.req.params.getOrDefault("kind", null);
            var whereParameters = new HashMap<String, Object>() {
                {
                    put("rel_app_id", relAppId);
                    put("rel_tenant_id", relTenantId);
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
                    Group.class)
                    .compose(groups -> context.helper.success(groups, GroupResp.class));
        };
    }

    public static ProcessFun<List<GroupResp>> findExposeGroups() {
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
            return context.fun.sql.list(
                    String.format(sql, new Group().tableName()),
                    whereParameters,
                    Group.class)
                    .compose(groups -> context.helper.success(groups, GroupResp.class));
        };
    }

    public static ProcessFun<Void> deleteGroup() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupId = Long.parseLong(context.req.params.get("groupId"));
            return context.helper.existToError(
                    context.fun.sql.exists(
                            new HashMap<>() {
                                {
                                    put("rel_group_id", groupId);
                                }
                            },
                            GroupNode.class), () -> new ConflictException("请先删除关联的群组节点数据"))
                    .compose(resp ->
                            context.fun.sql.softDelete(
                                    new HashMap<>() {
                                        {
                                            put("id", groupId);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    Group.class));
        };
    }

    // --------------------------------------------------------------------

    public static ProcessFun<Long> addGroupNode() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupId = Long.parseLong(context.req.params.get("groupId"));
            var groupNodeAddReq = context.req.body(GroupNodeAddReq.class);
            return context.fun.sql.tx(context, () ->
                    context.helper.notExistToError(
                            context.fun.sql.exists(
                                    new HashMap<>() {
                                        {
                                            put("id", groupId);
                                            put("rel_tenant_id", relTenantId);
                                            put("rel_app_id", relAppId);
                                        }
                                    },
                                    Group.class), () -> new ConflictException("关联群组不合法"))
                            .compose(resp ->
                                    packageGroupNodeCode(groupId, groupNodeAddReq.getParentId(), groupNodeAddReq.getSiblingId(), null,
                                            (IAMConfig) context.conf, context.fun.sql, context)
                                            .compose(fetchGroupNodeCode ->
                                                    context.helper.existToError(
                                                            context.fun.sql.exists(
                                                                    new HashMap<>() {
                                                                        {
                                                                            put("code", fetchGroupNodeCode);
                                                                            put("rel_group_id", groupId);
                                                                        }
                                                                    },
                                                                    GroupNode.class), () -> new ConflictException("群组节点编码已存在"))
                                                            .compose(r -> {
                                                                var groupNode = context.helper.convert(groupNodeAddReq, GroupNode.class);
                                                                groupNode.setRelGroupId(groupId);
                                                                groupNode.setCode(fetchGroupNodeCode);
                                                                return context.fun.sql.save(groupNode);
                                                            })))
            );
        };
    }

    public static ProcessFun<Void> modifyGroupNode() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupNodeId = Long.parseLong(context.req.params.get("groupNodeId"));
            var groupNodeModifyReq = context.req.body(GroupNodeModifyReq.class);
            var groupNodeSets = context.helper.convert(groupNodeModifyReq, GroupNode.class);
            return context.fun.sql.tx(context, () ->
                    context.helper.notExistToError(
                            context.fun.sql.getOne(
                                    String.format("SELECT node.* FROM %s AS node" +
                                                    "  INNER JOIN %s AS group ON group.id = node.rel_group_id" +
                                                    "  WHERE node.id = #{id} AND group.rel_tenant_id = #{rel_tenant_id} AND group.rel_app_id = #{rel_app_id}",
                                            new GroupNode().tableName(), new Group().tableName()),
                                    new HashMap<>() {
                                        {
                                            put("id", groupNodeId);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    GroupNode.class), () -> new UnAuthorizedException("关联群组不合法"))
                            .compose(fetchGroupNode -> {
                                if (groupNodeModifyReq.getParentId() != DewConstant.OBJECT_UNDEFINED || groupNodeModifyReq.getSiblingId() != DewConstant.OBJECT_UNDEFINED) {
                                    return packageGroupNodeCode(fetchGroupNode.getRelGroupId(), groupNodeModifyReq.getParentId(),
                                            groupNodeModifyReq.getSiblingId(), fetchGroupNode.getCode(), (IAMConfig) context.conf, context.fun.sql, context)
                                            .compose(fetchGroupNodeCode ->
                                                    context.helper.existToError(
                                                            context.fun.sql.exists(
                                                                    new HashMap<>() {
                                                                        {
                                                                            put("!id", groupNodeId);
                                                                            put("code", fetchGroupNodeCode);
                                                                            put("rel_group_id", fetchGroupNode.getRelGroupId());
                                                                        }
                                                                    },
                                                                    GroupNode.class), () -> new ConflictException("群组节点编码已存在"))
                                                            .compose(resp -> {
                                                                groupNodeSets.setCode(fetchGroupNodeCode);
                                                                return context.helper.success();
                                                            })
                                            );
                                } else {
                                    return context.helper.success();
                                }
                            })
                            .compose(resp -> context.fun.sql.update(groupNodeId, groupNodeSets))
            );
        };
    }

    public static ProcessFun<List<GroupNodeResp>> findGroupNodes() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupId = context.req.params.get("groupId");
            return context.fun.sql.list(
                    String.format("SELECT node.* FROM %s AS node" +
                                    "  INNER JOIN %s AS group ON group.id = node.rel_group_id" +
                                    "  WHERE group.rel_tenant_id = #{rel_tenant_id} AND group.rel_app_id = #{rel_app_id} AND node.rel_group_id = #{rel_group_id}" +
                                    "  ORDER BY node.code ASC",
                            new GroupNode().tableName(), new Group().tableName()),
                    new HashMap<>() {
                        {
                            put("rel_group_id", groupId);
                            put("rel_app_id", relAppId);
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    GroupNode.class)
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
                        return context.helper.success(roleNodeRespList);
                    });
        };
    }

    public static ProcessFun<Void> deleteGroupNode() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var groupNodeId = Long.parseLong(context.req.params.get("groupNodeId"));
            return context.fun.sql.tx(context,() ->
                    context.helper.existToError(
                            context.fun.sql.exists(
                                    new HashMap<>() {
                                        {
                                            put("rel_group_node_id", groupNodeId);
                                        }
                                    },
                                    AccountGroup.class), () -> new ConflictException("请先删除关联的账号群组数据"))
                            .compose(resp ->
                                    context.helper.existToError(
                                            context.fun.sql.exists(
                                                    new HashMap<>() {
                                                        {
                                                            put("rel_subject_kind", AuthSubjectKind.GROUP_NODE);
                                                            put("%rel_subject_ids", "%" + groupNodeId + ",%");
                                                        }
                                                    },
                                                    AuthPolicy.class), () -> new ConflictException("请先删除关联的权限策略数据"))
                                            .compose(r ->
                                                    context.fun.sql.getOne(
                                                            String.format("SELECT node.* FROM %s AS node" +
                                                                            "  INNER JOIN %s group ON group.id = node.rel_group_id" +
                                                                            "  WHERE node.id = #{node_id} AND group.rel_tenant_id = #{rel_tenant_id} AND group.rel_app_id = #{rel_app_id}",
                                                                    new GroupNode().tableName(), new Group().tableName()),
                                                            new HashMap<>() {
                                                                {
                                                                    put("node_id", groupNodeId);
                                                                    put("rel_tenant_id", relTenantId);
                                                                    put("rel_app_id", relAppId);
                                                                }
                                                            },
                                                            GroupNode.class)
                                                            .compose(fetchGroupNode ->
                                                                    context.fun.sql.softDelete(fetchGroupNode.getId(), GroupNode.class)
                                                                            .compose(re -> updateOtherGroupNodeCode(fetchGroupNode.getRelGroupId(), fetchGroupNode.getCode(), true, null,
                                                                                    (IAMConfig) context.conf, context.fun.sql, context))
                                                            ))));
        };
    }

    private static Future<String> packageGroupNodeCode(Long groupId, Long parentId, Long siblingId, String originalGroupNodeCode, IAMConfig config, FunSQLClient client, ProcessContext context) {
        if (parentId == DewConstant.OBJECT_UNDEFINED && siblingId == DewConstant.OBJECT_UNDEFINED) {
            var currentNodeCode = config.getApp().getInitNodeCode();
            return updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode, config, client, context)
                    .compose(resp -> context.helper.success(currentNodeCode));
        }
        if (siblingId == DewConstant.OBJECT_UNDEFINED) {
            return client.getOne(
                    String.format("SELECT code FROM %s" +
                                    "  WHERE rel_group_id = #{rel_group_id} AND id = #{id}",
                            new GroupNode().tableName()),
                    new HashMap<>() {
                        {
                            put("rel_group_id", groupId);
                            put("id", parentId);
                        }
                    })
                    .compose(fetchParentGroupNodeCode -> {
                        var currentNodeCode = fetchParentGroupNodeCode.getString("code") + config.getApp().getInitNodeCode();
                        return updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode, config, client, context)
                                .compose(resp -> context.helper.success(currentNodeCode));
                    });
        }
        return client.getOne(
                String.format("SELECT code FROM %s" +
                                "  WHERE id = #{id}",
                        new GroupNode().tableName()),
                new HashMap<>() {
                    {
                        put("id", siblingId);
                    }
                })
                .compose(fetchSiblingCode -> {
                    var siblingCode = fetchSiblingCode.getString("code");
                    var currentNodeLength = siblingCode.length();
                    var parentLength = currentNodeLength - config.getApp().getInitNodeCode().length();
                    var parentCode = siblingCode.substring(0, parentLength);
                    var currentLevelCode = Long.parseLong(siblingCode.substring(parentLength));
                    var currentNodeCode = parentCode + (currentLevelCode + 1);
                    return updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode, config, client, context)
                            .compose(resp -> context.helper.success(currentNodeCode));
                });
    }

    private static Future<Void> updateOtherGroupNodeCode(Long groupId, String currentGroupNodeCode, Boolean deleteOpt, String originalGroupNodeCode, IAMConfig config, FunSQLClient client, ProcessContext context) {
        var currentNodeLength = currentGroupNodeCode.length();
        var parentLength = currentNodeLength - config.getApp().getInitNodeCode().length();
        var parentNodeCode = currentGroupNodeCode.substring(0, parentLength);
        List<GroupNode> originalGroupNodes = new ArrayList<>();
        var offset = deleteOpt ? -1 : 1;
        var future = context.helper.success();
        if (originalGroupNodeCode != null) {
            var originalParentLength = originalGroupNodeCode.length() - config.getApp().getInitNodeCode().length();
            var originalParentNodeCode = originalGroupNodeCode.substring(0, originalParentLength);
            future.compose(resp ->
                    client.list(
                            String.format("SELECT * FROM %s" +
                                            "  WHERE rel_group_id = #{rel_group_id} AND code like #{code} AND code >= #{code_goe}",
                                    new GroupNode().tableName()),
                            new HashMap<>() {
                                {
                                    put("rel_group_id", groupId);
                                    put("code", originalParentNodeCode + "%");
                                    put("code_goe", originalGroupNodeCode);
                                }
                            },
                            GroupNode.class))
                    .compose(fetchGroupNodes -> {
                        originalGroupNodes.addAll(fetchGroupNodes);
                        return context.helper.success();
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
                    String.format(updateSql, new GroupNode().tableName()),
                    parameters,
                    GroupNode.class)
                    .compose(groupNodes ->
                            CompositeFuture.all(
                                    groupNodes.stream()
                                            .map(node -> {
                                                var code = node.getCode();
                                                var currNodeCode = Long.parseLong(code.substring(parentLength, currentNodeLength));
                                                var childNodeCode = code.substring(currentNodeLength);
                                                node.setCode(parentNodeCode + (currNodeCode + offset) + childNodeCode);
                                                return client.update(node);
                                            })
                                            .collect(Collectors.toList())
                            )
                    )
                    .compose(re -> {
                        if (originalGroupNodeCode != null) {
                            return CompositeFuture.all(
                                    originalGroupNodes.stream()
                                            .map(node -> {
                                                var code = node.getCode();
                                                node.setCode(parentNodeCode + code.substring(parentLength));
                                                return client.update(node);
                                            })
                                            .collect(Collectors.toList())
                            )
                                    .compose(r -> context.helper.success());
                        } else {
                            return context.helper.success();
                        }
                    });
        });
    }

}
