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
import idealworld.dew.framework.domain.IdEntity;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.exception.NotFoundException;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
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

/**
 * 应用控制台下的群组控制器.
 *
 * @author gudaoxuri
 */
public class ACGroupProcessor extends EventBusProcessor {

    {
        // 添加当前应用的群组
        addProcessor(OptActionKind.CREATE, "/console/app/group", eventBusContext ->
                addGroup(eventBusContext.req.body(GroupAddReq.class), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前应用的某个群组
        addProcessor(OptActionKind.PATCH, "/console/app/group/{groupId}", eventBusContext ->
                modifyGroup(Long.parseLong(eventBusContext.req.params.get("groupId")), eventBusContext.req.body(GroupModifyReq.class),
                        eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的某个群组信息
        addProcessor(OptActionKind.FETCH, "/console/app/group/{groupId}", eventBusContext ->
                getGroup(Long.parseLong(eventBusContext.req.params.get("groupId")), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的群组列表信息
        addProcessor(OptActionKind.FETCH, "/console/app/group", eventBusContext -> {
            if (eventBusContext.req.params.getOrDefault("expose", "false").equalsIgnoreCase("false")) {
                return findGroups(eventBusContext.req.params.getOrDefault("code", null), eventBusContext.req.params.getOrDefault("name", null),
                        eventBusContext.req.params.getOrDefault("kind", null), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context);
            } else {
                return findExposeGroups(eventBusContext.req.params.getOrDefault("code", null), eventBusContext.req.params.getOrDefault("name",
                        null), eventBusContext.req.params.getOrDefault("kind", null), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context);
            }
        });
        // 删除当前应用的某个群组
        addProcessor(OptActionKind.DELETE, "/console/app/group/{groupId}", eventBusContext ->
                deleteGroup(Long.parseLong(eventBusContext.req.params.get("groupId")), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));

        // 添加当前应用某个群组的节点
        addProcessor(OptActionKind.CREATE, "/console/app/group/{groupId}/node", eventBusContext ->
                addGroupNode(Long.parseLong(eventBusContext.req.params.get("groupId")), eventBusContext.req.body(GroupNodeAddReq.class),
                        eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前应用某个群组的节点
        addProcessor(OptActionKind.PATCH, "/console/app/group/{groupId}/node/{groupNodeId}", eventBusContext ->
                modifyGroupNode(Long.parseLong(eventBusContext.req.params.get("groupNodeId")), eventBusContext.req.body(GroupNodeModifyReq.class),
                        eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用某个群组的节点列表信息
        addProcessor(OptActionKind.FETCH, "/console/app/group/{groupId}/node", eventBusContext ->
                findGroupNodes(Long.parseLong(eventBusContext.req.params.get("groupId")), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 删除当前应用某个群组的节点
        addProcessor(OptActionKind.DELETE, "/console/app/group/{groupId}/node/{groupNodeId}", eventBusContext ->
                deleteGroupNode(Long.parseLong(eventBusContext.req.params.get("groupNodeId")), eventBusContext.req.identOptInfo.getAppId(),
                        eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
    }

    public ACGroupProcessor(String moduleName) {
        super(moduleName);
    }

    public static Future<Long> addGroup(GroupAddReq groupAddReq, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.helper.existToError(
                context.sql.exists(
                        Group.class,
                        new HashMap<>() {
                            {
                                put("code", groupAddReq.getCode());
                                put("rel_app_id", relAppId);
                                put("rel_tenant_id", relTenantId);
                            }
                        }), () -> new ConflictException("群组编码已存在"))
                .compose(resp -> {
                    var group = context.helper.convert(groupAddReq, Group.class);
                    group.setRelTenantId(relTenantId);
                    group.setRelAppId(relAppId);
                    return context.sql.save(group);
                });
    }

    public static Future<Void> modifyGroup(Long groupId, GroupModifyReq groupModifyReq, Long relAppId, Long relTenantId, ProcessContext context) {
        var future = context.helper.success();
        if (groupModifyReq.getCode() != null) {
            future
                    .compose(resp ->
                            context.helper.existToError(
                                    context.sql.exists(
                                            Group.class,
                                            new HashMap<>() {
                                                {
                                                    put("!id", groupId);
                                                    put("code", groupModifyReq.getCode());
                                                    put("rel_app_id", relAppId);
                                                    put("rel_tenant_id", relTenantId);
                                                }
                                            }), () -> new ConflictException("群组编码已存在")));
        }
        return future
                .compose(resp ->
                        context.sql.update(
                                context.helper.convert(groupModifyReq, Group.class),
                                new HashMap<>() {
                                    {
                                        put("id", groupId);
                                        put("rel_app_id", relAppId);
                                        put("rel_tenant_id", relTenantId);
                                    }
                                }));
    }

    public static Future<GroupResp> getGroup(Long groupId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                Group.class,
                new HashMap<>() {
                    {
                        put("id", groupId);
                        put("rel_app_id", relAppId);
                        put("rel_tenant_id", relTenantId);
                    }
                })
                .compose(group -> context.helper.success(group, GroupResp.class));
    }

    public static Future<List<GroupResp>> findGroups(String code, String name, String kind, Long relAppId, Long relTenantId, ProcessContext context) {
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
        return context.sql.list(Group.class, whereParameters)
                .compose(groups -> context.helper.success(groups, GroupResp.class));
    }

    public static Future<List<GroupResp>> findExposeGroups(String code, String name, String kind, Long relAppId, Long relTenantId,
                                                           ProcessContext context) {
        var sql = "SELECT * FROM %s" +
                " WHERE (expose_kind = ? AND rel_tenant_id = ?" +
                " OR expose_kind = ?)";
        var parameters = new ArrayList<>();
        parameters.add(Group.class);
        parameters.add(ExposeKind.TENANT);
        parameters.add(relTenantId);
        parameters.add(ExposeKind.GLOBAL);
        if (code != null && !code.isBlank()) {
            sql += " AND code = ?";
            parameters.add("%" + code + "%");
        }
        if (name != null && !name.isBlank()) {
            sql += " AND name = ?";
            parameters.add("%" + name + "%");
        }
        if (kind != null && !kind.isBlank()) {
            sql += " AND kind = ?";
            parameters.add("%" + kind + "%");
        }
        return context.sql.list(Group.class, sql, parameters)
                .compose(groups -> context.helper.success(groups, GroupResp.class));
    }

    public static Future<Void> deleteGroup(Long groupId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.helper.existToError(
                context.sql.exists(
                        GroupNode.class,
                        new HashMap<>() {
                            {
                                put("rel_group_id", groupId);
                            }
                        }), () -> new ConflictException("请先删除关联的群组节点数据"))
                .compose(resp ->
                        context.sql.softDelete(
                                Group.class,
                                new HashMap<>() {
                                    {
                                        put("id", groupId);
                                        put("rel_app_id", relAppId);
                                        put("rel_tenant_id", relTenantId);
                                    }
                                }));
    }

    // --------------------------------------------------------------------

    public static Future<Long> addGroupNode(Long groupId, GroupNodeAddReq groupNodeAddReq, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.tx(context, () ->
                context.helper.notExistToError(
                        context.sql.exists(
                                Group.class,
                                new HashMap<>() {
                                    {
                                        put("id", groupId);
                                        put("rel_tenant_id", relTenantId);
                                        put("rel_app_id", relAppId);
                                    }
                                }), () -> new NotFoundException("找不到对应的关联群组"))
                        .compose(resp ->
                                packageGroupNodeCode(groupId, groupNodeAddReq.getParentId(), groupNodeAddReq.getSiblingId(), null,
                                        (IAMConfig) context.conf, context.sql, context)
                                        .compose(fetchGroupNodeCode ->
                                                context.helper.existToError(
                                                        context.sql.exists(
                                                                GroupNode.class,
                                                                new HashMap<>() {
                                                                    {
                                                                        put("code", fetchGroupNodeCode);
                                                                        put("rel_group_id", groupId);
                                                                    }
                                                                }), () -> new ConflictException("群组节点编码已存在"))
                                                        .compose(r -> {
                                                            var groupNode = context.helper.convert(groupNodeAddReq, GroupNode.class);
                                                            groupNode.setRelGroupId(groupId);
                                                            groupNode.setCode(fetchGroupNodeCode);
                                                            return context.sql.save(groupNode);
                                                        })))
        );
    }

    public static Future<Void> modifyGroupNode(Long groupNodeId, GroupNodeModifyReq groupNodeModifyReq, Long relAppId, Long relTenantId,
                                               ProcessContext context) {
        var groupNodeSets = context.helper.convert(groupNodeModifyReq, GroupNode.class);
        return context.sql.tx(context, () ->
                context.helper.notExistToError(
                        context.sql.getOne(
                                GroupNode.class,
                                "SELECT node.* FROM %s AS node" +
                                        " INNER JOIN %s AS _group ON _group.id = node.rel_group_id" +
                                        " WHERE node.id = ? AND _group.rel_tenant_id = ? AND _group.rel_app_id = ?",
                                GroupNode.class, Group.class, groupNodeId, relTenantId, relAppId),
                        () -> new NotFoundException("找不到对应的关联群组"))
                        .compose(fetchGroupNode -> {
                            if (groupNodeModifyReq.getParentId() != DewConstant.OBJECT_UNDEFINED
                                    || groupNodeModifyReq.getSiblingId() != DewConstant.OBJECT_UNDEFINED) {
                                return packageGroupNodeCode(fetchGroupNode.getRelGroupId(), groupNodeModifyReq.getParentId(),
                                        groupNodeModifyReq.getSiblingId(), fetchGroupNode.getCode(), (IAMConfig) context.conf, context.sql, context)
                                        .compose(fetchGroupNodeCode ->
                                                context.helper.existToError(
                                                        context.sql.exists(
                                                                GroupNode.class,
                                                                new HashMap<>() {
                                                                    {
                                                                        put("!id", groupNodeId);
                                                                        put("code", fetchGroupNodeCode);
                                                                        put("rel_group_id", fetchGroupNode.getRelGroupId());
                                                                    }
                                                                }), () -> new ConflictException("群组节点编码已存在"))
                                                        .compose(resp -> {
                                                            groupNodeSets.setCode(fetchGroupNodeCode);
                                                            return context.helper.success();
                                                        })
                                        );
                            } else {
                                return context.helper.success();
                            }
                        })
                        .compose(resp -> context.sql.update(groupNodeSets, groupNodeId))
        );
    }

    public static Future<List<GroupNodeResp>> findGroupNodes(Long groupId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.list(
                GroupNode.class,
                "SELECT node.* FROM %s AS node" +
                        " INNER JOIN %s AS _group ON _group.id = node.rel_group_id" +
                        " WHERE _group.rel_tenant_id = ? AND _group.rel_app_id = ? AND node.rel_group_id = ?" +
                        " ORDER BY node.code ASC",
                GroupNode.class, Group.class, relTenantId, relAppId, groupId)
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
    }

    public static Future<Void> deleteGroupNode(Long groupNodeId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.tx(context, () ->
                context.helper.existToError(
                        context.sql.exists(
                                AccountGroup.class,
                                new HashMap<>() {
                                    {
                                        put("rel_group_node_id", groupNodeId);
                                    }
                                }),
                        () -> new ConflictException("请先删除关联的账号群组数据"))
                        .compose(resp ->
                                context.helper.existToError(
                                        context.sql.exists(
                                                AuthPolicy.class,
                                                new HashMap<>() {
                                                    {
                                                        put("rel_subject_kind", AuthSubjectKind.GROUP_NODE);
                                                        put("%rel_subject_ids", "%" + groupNodeId + ",%");
                                                    }
                                                }),
                                        () -> new ConflictException("请先删除关联的权限策略数据"))
                                        .compose(r ->
                                                context.sql.getOne(
                                                        GroupNode.class,
                                                        "SELECT node.* FROM %s AS node" +
                                                                " INNER JOIN %s _group ON _group.id = node.rel_group_id" +
                                                                " WHERE node.id = ? AND _group.rel_tenant_id = ? " +
                                                                "AND _group.rel_app_id = ?",
                                                        GroupNode.class, Group.class, groupNodeId, relTenantId, relAppId)
                                                        .compose(fetchGroupNode ->
                                                                context.sql.softDelete(GroupNode.class, fetchGroupNode.getId())
                                                                        .compose(re -> updateOtherGroupNodeCode(fetchGroupNode.getRelGroupId(),
                                                                                fetchGroupNode.getCode(), true, null,
                                                                                (IAMConfig) context.conf, context.sql, context))
                                                        ))));
    }

    private static Future<String> packageGroupNodeCode(Long groupId, Long parentId, Long siblingId, String originalGroupNodeCode, IAMConfig config,
                                                       FunSQLClient client, ProcessContext context) {
        if (parentId == DewConstant.OBJECT_UNDEFINED && siblingId == DewConstant.OBJECT_UNDEFINED) {
            var currentNodeCode = config.getApp().getInitNodeCode();
            return updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode, config, client, context)
                    .compose(resp -> context.helper.success(currentNodeCode));
        }
        if (siblingId == DewConstant.OBJECT_UNDEFINED) {
            return client.getOne(
                    "SELECT code FROM %s WHERE rel_group_id = ? AND id = ?",
                    GroupNode.class, groupId, parentId)
                    .compose(fetchParentGroupNodeCode -> {
                        var currentNodeCode = fetchParentGroupNodeCode.getString("code") + config.getApp().getInitNodeCode();
                        return updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode, config, client, context)
                                .compose(resp -> context.helper.success(currentNodeCode));
                    });
        }
        return client.getOne(
                "SELECT code FROM %s WHERE id = ?",
                GroupNode.class, siblingId)
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

    private static Future<Void> updateOtherGroupNodeCode(Long groupId, String currentGroupNodeCode, Boolean deleteOpt, String originalGroupNodeCode,
                                                         IAMConfig config, FunSQLClient client, ProcessContext context) {
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
                            GroupNode.class,
                            "SELECT * FROM %s" +
                                    " WHERE rel_group_id = ? AND code like ? AND code >= ?",
                            GroupNode.class, groupId, originalParentNodeCode + "%", originalGroupNodeCode))
                    .compose(fetchGroupNodes -> {
                        originalGroupNodes.addAll(fetchGroupNodes);
                        return context.helper.success();
                    });
        }
        return future.compose(resp -> {
            var updateSql = "SELECT * FROM %s" +
                    " WHERE rel_group_id = ? AND code like ? AND code >= ?";
            var parameters = new ArrayList<>();
            parameters.add(GroupNode.class);
            parameters.add(groupId);
            parameters.add(parentNodeCode + "%");
            parameters.add(currentGroupNodeCode);
            if (!originalGroupNodes.isEmpty()) {
                parameters.addAll(originalGroupNodes.stream().map(IdEntity::getId).collect(Collectors.toList()));
                updateSql += " AND id NOT IN (" + originalGroupNodes.stream().map(i -> "?").collect(Collectors.joining(", ")) + ")";
            }
            // 排序，避免索引重复
            if (deleteOpt) {
                updateSql += " ORDER BY code ASC";
            } else {
                updateSql += " ORDER BY code DESC";
            }
            return client.list(GroupNode.class, updateSql, parameters)
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
