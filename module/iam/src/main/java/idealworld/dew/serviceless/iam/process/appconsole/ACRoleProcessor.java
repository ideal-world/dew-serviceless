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
import idealworld.dew.framework.fun.eventbus.ProcessFun;
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
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/app/role/def", addRoleDef());
        // 修改当前应用的某个角色定义
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/app/role/def/{roleDefId}", modifyRoleDef());
        // 获取当前应用的某个角色定义信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/role/def/{roleDefId}", getRoleDef());
        // 获取当前应用的角色定义列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/role/def", pageRoleDef());
        // 删除当前应用的某个角色定义
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/app/role/def/{roleDefId}", deleteRoleDef());

        // 添加当前应用的角色
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/app/role", addRole());
        // 修改当前应用的某个角色
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/app/role/{roleId}", modifyRole());
        // 获取当前应用的某个角色信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/role/{roleId}", getRole());
        // 获取当前应用的角色列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/app/role", findRoles());
        // 删除当前应用的某个角色
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/app/role/{roleId}", deleteRole());
    }

    public static ProcessFun<Long> addRoleDef() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleDefAddReq = context.req.body(RoleDefAddReq.class);
            return context.helper.existToError(
                    context.fun.sql.exists(
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
                        return context.fun.sql.save(roleDef);
                    });
        };
    }

    public static ProcessFun<Void> modifyRoleDef() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleDefId = Long.parseLong(context.req.params.get("roleDefId"));
            var roleDefModifyReq = context.req.body(RoleDefModifyReq.class);
            var future = Future.succeededFuture();
            if (roleDefModifyReq.getCode() != null) {
                future
                        .compose(resp ->
                                context.helper.existToError(
                                        context.fun.sql.exists(
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
                            context.fun.sql.update(
                                    new HashMap<>() {
                                        {
                                            put("id", roleDefId);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    context.helper.convert(roleDefModifyReq, RoleDef.class))
                    );

        };
    }

    public static ProcessFun<RoleDefResp> getRoleDef() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleDefId = Long.parseLong(context.req.params.get("roleDefId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", roleDefId);
                            put("rel_app_id", relAppId);
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    RoleDef.class)
                    .compose(roleDef -> context.helper.success(roleDef, RoleDefResp.class));
        };
    }

    public static ProcessFun<Page<RoleDefResp>> pageRoleDef() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var code = context.req.params.getOrDefault("code", null);
            var name = context.req.params.getOrDefault("name", null);
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
            return context.fun.sql.page(
                    whereParameters,
                    context.req.pageNumber(),
                    context.req.pageSize(),
                    RoleDef.class)
                    .compose(roleDefs -> context.helper.success(roleDefs, RoleDefResp.class));
        };
    }

    public static ProcessFun<Void> deleteRoleDef() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleDefId = Long.parseLong(context.req.params.get("roleDefId"));
            return context.helper.existToError(
                    context.fun.sql.exists(
                            new HashMap<>() {
                                {
                                    put("rel_role_def_id", roleDefId);
                                }
                            },
                            Role.class), () -> new ConflictException("请先删除关联的角色数据"))
                    .compose(existsRoleDefRel ->
                            context.fun.sql.softDelete(
                                    new HashMap<>() {
                                        {
                                            put("id", roleDefId);
                                            put("rel_app_id", relAppId);
                                            put("rel_tenant_id", relTenantId);
                                        }
                                    },
                                    RoleDef.class)
                    );
        };
    }

    // --------------------------------------------------------------------

    public static ProcessFun<Long> addRole() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleAddReq = context.req.body(RoleAddReq.class);
            return context.helper.existToError(
                    context.fun.sql.exists(
                            new HashMap<>() {
                                {
                                    put("rel_role_def_id", roleAddReq.getRelRoleDefId());
                                    put("rel_group_node_id", roleAddReq.getRelGroupNodeId());
                                }
                            },
                            Role.class), () -> new ConflictException("角色已存在"))
                    .compose(resp ->
                            context.helper.notExistToError(
                                    context.fun.sql.getOne(
                                            new HashMap<>() {
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
                                        context.fun.sql.getOne(
                                                String.format("SELECT node.name FROM %s AS node" +
                                                                "  INNER JOIN %s AS group ON group.id = node.rel_group_id" +
                                                                "  WHERE node.id = #{rel_group_node_id} AND group.rel_tenant_id = #{rel_tenant_id} AND group.rel_app_id = #{rel_app_id}",
                                                        new GroupNode().tableName(), new Group().tableName()),
                                                new HashMap<>() {
                                                    {
                                                        put("rel_group_node_id", roleAddReq.getRelGroupNodeId());
                                                        put("rel_app_id", relAppId);
                                                        put("rel_tenant_id", relTenantId);
                                                    }
                                                }), () -> new UnAuthorizedException("对应的群组节点不合法"))
                                        .compose(fetchGroupNodeName -> {
                                            roleAddReq.setName(fetchGroupNodeName.getString("node.name") + " ");
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
                        return context.fun.sql.save(role);
                    });
        };
    }

    public static ProcessFun<Void> modifyRole() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleId = Long.parseLong(context.req.params.get("roleId"));
            var roleModifyReq = context.req.body(RoleModifyReq.class);
            return context.fun.sql.update(
                    new HashMap<>() {
                        {
                            put("id", roleId);
                            put("rel_app_id", relAppId);
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    context.helper.convert(roleModifyReq, Role.class));
        };
    }

    public static ProcessFun<RoleResp> getRole() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleId = Long.parseLong(context.req.params.get("roleId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("id", roleId);
                            put("rel_app_id", relAppId);
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    RoleDef.class)
                    .compose(role -> context.helper.success(role, RoleResp.class));
        };
    }

    public static ProcessFun<List<RoleResp>> findRoles() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var name = context.req.params.getOrDefault("name", null);
            var whereParameters = new HashMap<String, Object>() {
                {
                    put("rel_app_id", relAppId);
                    put("rel_tenant_id", relTenantId);
                }
            };
            if (name != null && !name.isBlank()) {
                whereParameters.put("%name", "%" + name + "%");
            }
            return context.fun.sql.list(
                    whereParameters,
                    Role.class)
                    .compose(roles -> context.helper.success(roles, RoleResp.class));
        };
    }

    public static ProcessFun<List<RoleResp>> findExposeRoles() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var name = context.req.params.getOrDefault("name", null);
            var sql = "SELECT * FROM %s" +
                    "  WHERE expose_kind = #{expose_kind_tenant} AND rel_tenant_id = #{rel_tenant_id}" +
                    "    OR expose_kind = #{expose_kind_global}";
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
            return context.fun.sql.list(
                    String.format(sql,new Role().tableName()),
                    whereParameters,
                    Role.class)
                    .compose(roles -> context.helper.success(roles, RoleResp.class));
        };
    }

    public static ProcessFun<Void> deleteRole() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleId = Long.parseLong(context.req.params.get("roleId"));
            return context.helper.existToError(
                    context.fun.sql.exists(
                            new HashMap<>() {
                                {
                                    put("rel_role_id", roleId);
                                }
                            },
                            AccountRole.class), () -> new ConflictException("请先删除关联的账号角色数据"))
                    .compose(resp ->
                            context.helper.existToError(
                                    context.fun.sql.exists(
                                            new HashMap<>() {
                                                {
                                                    put("rel_subject_kind", AuthSubjectKind.ROLE);
                                                    put("%rel_subject_ids", "%" + roleId + ",%");
                                                }
                                            },
                                            AuthPolicy.class), () -> new ConflictException("请先删除关联的权限策略数据")))
                    .compose(existsRoleRelByPolicy ->
                            context.fun.sql.softDelete(
                                    new HashMap<>() {
                                        {
                                            put("id", roleId);
                                            put("rel_tenant_id", relTenantId);
                                            put("rel_app_id", relAppId);
                                        }
                                    },
                                    Role.class));
        };
    }

    public static Future<Long> getTenantAdminRoleId(ProcessContext context) {
        return context.fun.sql.getOne(
                String.format("SELECT role.id FROM %s AS role" +
                        "  INNER JOIN %s def ON def.id = role.rel_role_def_id" +
                        "  WHERE def.code = #{role_def_code}" +
                        "   ORDER BY def.create_time ASC", new Role().tableName(), new RoleDef().tableName()),
                new HashMap<>() {
                    {
                        put("role_def_code", ((IAMConfig) context.conf).getSecurity().getTenantAdminRoleDefCode());
                    }
                })
                .compose(fetchRoleId -> context.helper.success(fetchRoleId.getLong("role.id")));
    }

    public static Future<Long> getAppAdminRoleId(ProcessContext context) {
        return context.fun.sql.getOne(
                String.format("SELECT role.id FROM %s AS role" +
                        "  INNER JOIN %s def ON def.id = role.rel_role_def_id" +
                        "  WHERE def.code = #{role_def_code}" +
                        "   ORDER BY def.create_time ASC", new Role().tableName(), new RoleDef().tableName()),
                new HashMap<>() {
                    {
                        put("role_def_code", ((IAMConfig) context.conf).getSecurity().getAppAdminRoleDefCode());
                    }
                })
                .compose(fetchRoleId -> context.helper.success(fetchRoleId.getLong("role.id")));
    }

}
