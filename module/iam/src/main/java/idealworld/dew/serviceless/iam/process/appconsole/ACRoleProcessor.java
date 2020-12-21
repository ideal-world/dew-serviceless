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

import com.ecfront.dew.common.Page;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.domain.auth.*;
import idealworld.dew.serviceless.iam.dto.ExposeKind;
import idealworld.dew.serviceless.iam.process.appconsole.dto.role.*;
import io.vertx.core.Future;

import java.util.HashMap;
import java.util.List;

/**
 * 应用控制台下的角色控制器.
 *
 * @author gudaoxuri
 */
public class ACRoleProcessor {

    static {
        // 添加当前应用的角色定义
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/app/role/def", eventBusContext ->
                addRoleDef(eventBusContext.req.body(RoleDefAddReq.class), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前应用的某个角色定义
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/app/role/def/{roleDefId}", eventBusContext ->
                modifyRoleDef(Long.parseLong(eventBusContext.req.params.get("roleDefId")), eventBusContext.req.body(RoleDefModifyReq.class), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的某个角色定义信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/role/def/{roleDefId}", eventBusContext ->
                getRoleDef(Long.parseLong(eventBusContext.req.params.get("roleDefId")), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的角色定义列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/role/def", eventBusContext ->
                pageRoleDef(eventBusContext.req.params.getOrDefault("code", null), eventBusContext.req.params.getOrDefault("name", null), eventBusContext.req.pageNumber(), eventBusContext.req.pageSize(), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 删除当前应用的某个角色定义
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/app/role/def/{roleDefId}", eventBusContext ->
                deleteRoleDef(Long.parseLong(eventBusContext.req.params.get("roleDefId")), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));

        // 添加当前应用的角色
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/app/role", eventBusContext ->
                addRole(eventBusContext.req.body(RoleAddReq.class), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前应用的某个角色
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/app/role/{roleId}", eventBusContext ->
                modifyRole(Long.parseLong(eventBusContext.req.params.get("roleId")), eventBusContext.req.body(RoleModifyReq.class), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的某个角色信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/role/{roleId}", eventBusContext ->
                getRole(Long.parseLong(eventBusContext.req.params.get("roleId")), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前应用的角色列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/role", eventBusContext ->
                findRoles(eventBusContext.req.params.getOrDefault("name", null), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 删除当前应用的某个角色
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/app/role/{roleId}", eventBusContext ->
                deleteRole(Long.parseLong(eventBusContext.req.params.get("roleId")), eventBusContext.req.identOptInfo.getAppId(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
    }

    public static Future<Long> addRoleDef(RoleDefAddReq roleDefAddReq, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.helper.existToError(
                context.sql.exists(
                        new HashMap<>() {
                            {
                                put("code", roleDefAddReq.getCode());
                                put("rel_app_id", relAppId);
                                put("rel_tenant_id", relTenantId);
                            }
                        },
                        RoleDef.class), () -> new ConflictException("角色定义编码已存在"))
                .compose(existsRoleDef -> {
                    var roleDef = context.helper.convert(roleDefAddReq, RoleDef.class);
                    roleDef.setRelTenantId(relTenantId);
                    roleDef.setRelAppId(relAppId);
                    return context.sql.save(roleDef);
                });
    }

    public static Future<Void> modifyRoleDef(Long roleDefId, RoleDefModifyReq roleDefModifyReq, Long relAppId, Long relTenantId, ProcessContext context) {
        var future = Future.succeededFuture();
        if (roleDefModifyReq.getCode() != null) {
            future
                    .compose(resp ->
                            context.helper.existToError(
                                    context.sql.exists(
                                            new HashMap<>() {
                                                {
                                                    put("!id", roleDefId);
                                                    put("code", roleDefModifyReq.getCode());
                                                    put("rel_app_id", relAppId);
                                                    put("rel_tenant_id", relTenantId);
                                                }
                                            },
                                            RoleDef.class), () -> new ConflictException("角色定义编码已存在")));
        }
        return future
                .compose(resp ->
                        context.sql.update(
                                new HashMap<>() {
                                    {
                                        put("id", roleDefId);
                                        put("rel_app_id", relAppId);
                                        put("rel_tenant_id", relTenantId);
                                    }
                                },
                                context.helper.convert(roleDefModifyReq, RoleDef.class))
                );
    }

    public static Future<RoleDefResp> getRoleDef(Long roleDefId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", roleDefId);
                        put("rel_app_id", relAppId);
                        put("rel_tenant_id", relTenantId);
                    }
                },
                RoleDef.class)
                .compose(roleDef -> context.helper.success(roleDef, RoleDefResp.class));
    }

    public static Future<Page<RoleDefResp>> pageRoleDef(String code, String name, Long pageNumber, Long pageSize, Long relAppId, Long relTenantId, ProcessContext context) {
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
        return context.sql.page(
                whereParameters,
                pageNumber,
                pageSize,
                RoleDef.class)
                .compose(roleDefs -> context.helper.success(roleDefs, RoleDefResp.class));
    }

    public static Future<Void> deleteRoleDef(Long roleDefId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.helper.existToError(
                context.sql.exists(
                        new HashMap<>() {
                            {
                                put("rel_role_def_id", roleDefId);
                            }
                        },
                        Role.class), () -> new ConflictException("请先删除关联的角色数据"))
                .compose(existsRoleDefRel ->
                        context.sql.softDelete(
                                new HashMap<>() {
                                    {
                                        put("id", roleDefId);
                                        put("rel_app_id", relAppId);
                                        put("rel_tenant_id", relTenantId);
                                    }
                                },
                                RoleDef.class)
                );
    }

    // --------------------------------------------------------------------

    public static Future<Long> addRole(RoleAddReq roleAddReq, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.helper.existToError(
                context.sql.exists(
                        new HashMap<>() {
                            {
                                put("rel_role_def_id", roleAddReq.getRelRoleDefId());
                                put("rel_group_node_id", roleAddReq.getRelGroupNodeId());
                            }
                        },
                        Role.class), () -> new ConflictException("角色已存在"))
                .compose(resp ->
                        context.helper.notExistToError(
                                context.sql.getOne(
                                        new HashMap<String,Object>() {
                                            {
                                                put("id", roleAddReq.getRelRoleDefId());
                                                put("rel_app_id", relAppId);
                                                put("rel_tenant_id", relTenantId);
                                            }
                                        },
                                        RoleDef.class), () -> new UnAuthorizedException("对应的角色定义不合法"))
                )
                .compose(fetchRoleDef -> {
                    if (roleAddReq.getName() == null) {
                        if (roleAddReq.getRelGroupNodeId() != DewConstant.OBJECT_UNDEFINED) {
                            return context.helper.notExistToError(
                                    context.sql.getOne(
                                            String.format("SELECT node.name FROM %s AS node" +
                                                            " INNER JOIN %s AS _group ON _group.id = node.rel_group_id" +
                                                            " WHERE node.id = #{rel_group_node_id} AND _group.rel_tenant_id = #{rel_tenant_id} AND _group.rel_app_id = #{rel_app_id}",
                                                    new GroupNode().tableName(), new Group().tableName()),
                                            new HashMap<>() {
                                                {
                                                    put("rel_group_node_id", roleAddReq.getRelGroupNodeId());
                                                    put("rel_app_id", relAppId);
                                                    put("rel_tenant_id", relTenantId);
                                                }
                                            }), () -> new UnAuthorizedException("对应的群组节点不合法"))
                                    .compose(fetchGroupNodeName -> {
                                        roleAddReq.setName(fetchGroupNodeName.getString("name") + " ");
                                        return context.helper.success();
                                    });
                        } else {
                            roleAddReq.setName("");
                        }
                        roleAddReq.setName(roleAddReq.getName() + fetchRoleDef.getName());
                    }
                    return context.helper.success();
                })
                .compose(resp -> {
                    var role = context.helper.convert(roleAddReq, Role.class);
                    role.setRelTenantId(relTenantId);
                    role.setRelAppId(relAppId);
                    return context.sql.save(role);
                });
    }

    public static Future<Void> modifyRole(Long roleId, RoleModifyReq roleModifyReq, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.update(
                new HashMap<>() {
                    {
                        put("id", roleId);
                        put("rel_app_id", relAppId);
                        put("rel_tenant_id", relTenantId);
                    }
                },
                context.helper.convert(roleModifyReq, Role.class));
    }

    public static Future<RoleResp> getRole(Long roleId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", roleId);
                        put("rel_app_id", relAppId);
                        put("rel_tenant_id", relTenantId);
                    }
                },
                RoleDef.class)
                .compose(role -> context.helper.success(role, RoleResp.class));
    }

    public static Future<List<RoleResp>> findRoles(String name, Long relAppId, Long relTenantId, ProcessContext context) {
        var whereParameters = new HashMap<String, Object>() {
            {
                put("rel_app_id", relAppId);
                put("rel_tenant_id", relTenantId);
            }
        };
        if (name != null && !name.isBlank()) {
            whereParameters.put("%name", "%" + name + "%");
        }
        return context.sql.list(
                whereParameters,
                Role.class)
                .compose(roles -> context.helper.success(roles, RoleResp.class));
    }

    public static Future<List<RoleResp>> findExposeRoles(String name, Long relAppId, Long relTenantId, ProcessContext context) {
        var sql = "SELECT * FROM %s" +
                " WHERE expose_kind = #{expose_kind_tenant} AND rel_tenant_id = #{rel_tenant_id}" +
                " OR expose_kind = #{expose_kind_global}";
        var whereParameters = new HashMap<String, Object>() {
            {
                put("expose_kind_tenant", ExposeKind.TENANT);
                put("rel_tenant_id", relTenantId);
                put("expose_kind_global", ExposeKind.GLOBAL);
            }
        };
        if (name != null && !name.isBlank()) {
            sql += " AND name like name #{name}";
            whereParameters.put("name", "%" + name + "%");
        }
        return context.sql.list(
                String.format(sql, new Role().tableName()),
                whereParameters,
                Role.class)
                .compose(roles -> context.helper.success(roles, RoleResp.class));
    }

    public static Future<Void> deleteRole(Long roleId, Long relAppId, Long relTenantId, ProcessContext context) {
        return context.helper.existToError(
                context.sql.exists(
                        new HashMap<>() {
                            {
                                put("rel_role_id", roleId);
                            }
                        },
                        AccountRole.class), () -> new ConflictException("请先删除关联的账号角色数据"))
                .compose(resp ->
                        context.helper.existToError(
                                context.sql.exists(
                                        new HashMap<>() {
                                            {
                                                put("rel_subject_kind", AuthSubjectKind.ROLE);
                                                put("%rel_subject_ids", "%" + roleId + ",%");
                                            }
                                        },
                                        AuthPolicy.class), () -> new ConflictException("请先删除关联的权限策略数据")))
                .compose(existsRoleRelByPolicy ->
                        context.sql.softDelete(
                                new HashMap<>() {
                                    {
                                        put("id", roleId);
                                        put("rel_tenant_id", relTenantId);
                                        put("rel_app_id", relAppId);
                                    }
                                },
                                Role.class));
    }

    public static Future<Long> getTenantAdminRoleId(ProcessContext context) {
        return context.sql.getOne(
                String.format("SELECT role.id FROM %s AS role" +
                        " INNER JOIN %s def ON def.id = role.rel_role_def_id" +
                        " WHERE def.code = #{role_def_code}" +
                        " ORDER BY def.create_time ASC", new Role().tableName(), new RoleDef().tableName()),
                new HashMap<>() {
                    {
                        put("role_def_code", ((IAMConfig) context.conf).getSecurity().getTenantAdminRoleDefCode());
                    }
                })
                .compose(fetchRoleId -> context.helper.success(fetchRoleId.getLong("id")));
    }

    public static Future<Long> getAppAdminRoleId(ProcessContext context) {
        return context.sql.getOne(
                String.format("SELECT role.id FROM %s AS role" +
                        " INNER JOIN %s def ON def.id = role.rel_role_def_id" +
                        " WHERE def.code = #{role_def_code}" +
                        " ORDER BY def.create_time ASC", new Role().tableName(), new RoleDef().tableName()),
                new HashMap<>() {
                    {
                        put("role_def_code", ((IAMConfig) context.conf).getSecurity().getAppAdminRoleDefCode());
                    }
                })
                .compose(fetchRoleId -> context.helper.success(fetchRoleId.getLong("id")));
    }

}
