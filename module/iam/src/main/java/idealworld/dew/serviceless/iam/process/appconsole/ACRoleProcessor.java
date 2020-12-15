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
import com.ecfront.dew.common.Resp;
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

    {
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

    public ProcessFun<Long> addRoleDef() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleDefAddReq = context.helper.parseBody(context.req.body, RoleDefAddReq.class);
            return context.fun.sql.exists(
                    new HashMap<>() {
                        {
                            put("rel_tenant_id", relTenantId);
                            put("rel_app_id", relAppId);
                            put("code", roleDefAddReq.getCode());
                        }
                    },
                    RoleDef.class,
                    context)
                    .compose(existsRoleDef -> {
                        if (existsRoleDef) {
                            throw new ConflictException("角色定义编码已存在");
                        }
                        var roleDef = context.helper.convert(roleDefAddReq, RoleDef.class);
                        roleDef.setRelTenantId(relTenantId);
                        roleDef.setRelAppId(relAppId);
                        return context.fun.sql.save(roleDef, context);
                    });
        };
    }

    public ProcessFun<Void> modifyRoleDef() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleDefId = Long.parseLong(context.req.params.get("roleDefId"));
            var roleDefModifyReq = context.helper.parseBody(context.req.body, RoleDefModifyReq.class);
            var future = Future.succeededFuture();
            if (roleDefModifyReq.getCode() != null) {
                future
                        .compose(resp ->
                                context.fun.sql.exists(
                                        new HashMap<>() {
                                            {
                                                put("rel_tenant_id", relTenantId);
                                                put("rel_app_id", relAppId);
                                                put("code", roleDefModifyReq.getCode());
                                                put("-id", roleDefId);
                                            }
                                        },
                                        RoleDef.class,
                                        context)
                                        .compose(existsRoleDef -> {
                                            if (existsRoleDef) {
                                                throw new ConflictException("角色定义编码已存在");
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
                                            put("id", roleDefId);
                                        }
                                    },
                                    context.helper.convert(roleDefModifyReq, RoleDef.class),
                                    context)
                    );

        };
    }

    public ProcessFun<RoleDefResp> getRoleDef() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleDefId = Long.parseLong(context.req.params.get("roleDefId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("rel_tenant_id", relTenantId);
                            put("rel_app_id", relAppId);
                            put("id", roleDefId);
                        }
                    },
                    RoleDef.class,
                    context
            )
                    .compose(roleDef -> context.helper.success(roleDef, RoleDefResp.class));
        };
    }

    public ProcessFun<Page<RoleDefResp>> pageRoleDef() {
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
                    RoleDef.class,
                    context
            )
                    .compose(roleDefs -> context.helper.success(roleDefs, RoleDefResp.class));
        };
    }

    public ProcessFun<Void> deleteRoleDef() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleDefId = Long.parseLong(context.req.params.get("roleDefId"));
            return context.fun.sql.exists(
                    new HashMap<>() {
                        {
                            put("rel_role_def_id", roleDefId);
                        }
                    },
                    Role.class,
                    context)
                    .compose(existsRoleDefRel -> {
                        if (existsRoleDefRel) {
                            throw new ConflictException("请先删除关联的角色数据");
                        }
                        return context.fun.sql.softDelete(
                                new HashMap<>() {
                                    {
                                        put("id", roleDefId);
                                        put("rel_tenant_id", relTenantId);
                                        put("rel_app_id", relAppId);
                                    }
                                },
                                RoleDef.class,
                                context);
                    });
        };
    }

    // --------------------------------------------------------------------

    public ProcessFun<Long> addRole() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleAddReq = context.helper.parseBody(context.req.body, RoleAddReq.class);
            return context.fun.sql.exists(
                    new HashMap<>() {
                        {
                            put("rel_role_def_id", roleAddReq.getRelRoleDefId());
                            put("rel_group_node_id", roleAddReq.getRelGroupNodeId();
                        }
                    },
                    Role.class,
                    context)
                    .compose(existsRole -> {
                        if (existsRole) {
                            throw new ConflictException("角色已存在");
                        }
                        return context.fun.sql.getOne(
                                new HashMap<>() {
                                    {
                                        put("id", roleAddReq.getRelRoleDefId());
                                        put("rel_tenant_id", relTenantId);
                                        put("rel_app_id", relAppId);
                                    }
                                },
                                RoleDef.class,
                                context);
                    })
                    .compose(fetchRoleDef ->{
                        if(fetchRoleDef == null){
                            throw new UnAuthorizedException("对应的角色定义不合法");
                        }
                        if (roleAddReq.getName() == null) {
                            if (roleAddReq.getRelGroupNodeId() != DewConstant.OBJECT_UNDEFINED) {
                                return context.fun.sql.getOne(
                                        "SELECT node.name FROM %s AS node" +
                                                "  INNER JOIN %s AS group ON group.id = node.rel_group_id" +
                                                "  WHERE node.id = #{rel_group_node_id} AND group.rel_tenant_id = #{rel_tenant_id} AND group.rel_app_id = #{rel_app_id}",
                                        new HashMap<>(){
                                            {
                                                put("rel_group_node_id", roleAddReq.getRelGroupNodeId());
                                                put("rel_tenant_id", relTenantId);
                                                put("rel_app_id", relAppId);
                                            }
                                        },
                                        context,
                                        new GroupNode().tableName(),new Group().tableName())
                                        .compose(fetchGroupNodeName -> {
                                            if (fetchGroupNodeName == null) {
                                                throw new UnAuthorizedException("对应的群组节点不合法");
                                            }
                                            roleAddReq.setName(fetchGroupNodeName.getString("node.name") + " ");
                                            return Future.succeededFuture();
                                        });
                            } else {
                                roleAddReq.setName("");
                            }
                            roleAddReq.setName(roleAddReq.getName() + fetchRoleDef.getName());
                        }
                        return Future.succeededFuture();
                    })
                    .compose(resp -> {
                        var role = context.helper.convert(roleAddReq, Role.class);
                        role.setRelTenantId(relTenantId);
                        role.setRelAppId(relAppId);
                        return context.fun.sql.save(role, context);
                    });
        };
    }

    public ProcessFun<Void> modifyRole() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleId = Long.parseLong(context.req.params.get("roleId"));
            var roleModifyReq = context.helper.parseBody(context.req.body, RoleModifyReq.class);
            return context.fun.sql.update(
                    new HashMap<>() {
                        {
                            put("rel_tenant_id", relTenantId);
                            put("rel_app_id", relAppId);
                            put("id", roleId);
                        }
                    },
                    context.helper.convert(roleModifyReq, Role.class),
                    context);
        };
    }

    public ProcessFun<RoleResp> getRole() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleId = Long.parseLong(context.req.params.get("roleId"));
            return context.fun.sql.getOne(
                    new HashMap<>() {
                        {
                            put("rel_tenant_id", relTenantId);
                            put("rel_app_id", relAppId);
                            put("id", roleId);
                        }
                    },
                    RoleDef.class,
                    context
            )
                    .compose(role -> context.helper.success(role, RoleResp.class));
        };
    }

    public ProcessFun<List<RoleResp>> findRoles() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var name = context.req.params.getOrDefault("name", null);
            var whereParameters = new HashMap<String, Object>() {
                {
                    put("rel_tenant_id", relTenantId);
                    put("rel_app_id", relAppId);
                }
            };
            if (name != null && !name.isBlank()) {
                whereParameters.put("%name", "%" + name + "%");
            }
            return context.fun.sql.list(
                    whereParameters,
                    Role.class,
                    context
            )
                    .compose(roles -> context.helper.success(roles, RoleResp.class));
        };
    }

    public ProcessFun<List<RoleResp>> findExposeRoles() {
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
                sql+= " AND name like name #{name}";
                whereParameters.put("name", "%" + name + "%");
            }
            return context.fun.sql.list(sql,
                    whereParameters,
                    Role.class,
                    context
            )
                    .compose(roles -> context.helper.success(roles, RoleResp.class));
        };
    }

    public ProcessFun<Void> deleteRole() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var relAppId = context.req.identOptInfo.getAppId();
            var roleId = Long.parseLong(context.req.params.get("roleId"));
            return context.fun.sql.exists(
                    new HashMap<>() {
                        {
                            put("rel_role_id", roleId);
                        }
                    },
                    AccountRole.class,
                    context)
                    .compose(existsRoleRelByAccount -> {
                                if (existsRoleRelByAccount) {
                                    throw new ConflictException("请先删除关联的账号角色数据");
                                }
                                return context.fun.sql.exists(
                                        new HashMap<>() {
                                            {
                                                put("rel_subject_kind", AuthSubjectKind.ROLE);
                                                put("%rel_subject_ids", "%" + roleId + ",%");
                                            }
                                        },
                                        AuthPolicy.class,
                                        context);
                            })
                    .compose(existsRoleRelByPolicy -> {
                        if (existsRoleRelByPolicy) {
                            throw new ConflictException("请先删除关联的权限策略数据");
                        }
                        return context.fun.sql.softDelete(
                                new HashMap<>() {
                                    {
                                        put("id", roleId);
                                        put("rel_tenant_id", relTenantId);
                                        put("rel_app_id", relAppId);
                                    }
                                },
                                Role.class,
                                context);
                    });
        };
    }

    public static Future<Long> getTenantAdminRoleId(ProcessContext context) {
       return context.fun.sql.getOne(
                "SELECT role.id FROM %s AS role" +
                        "  INNER JOIN %s def ON def.id = role.rel_role_def_id"+
                        "  WHERE def.code = #{role_def_code}" +
                                "   ORDER BY def.create_time ASC",
                new HashMap<>() {
                    {
                        put("role_def_code", ((IAMConfig)context.conf).getSecurity().getTenantAdminRoleDefCode());
                    }
                },
                context)
                .compose(fetchRoleId -> Future.succeededFuture(fetchRoleId.getLong("role.id")));
    }

    public static Future<Long> getAppAdminRoleId(ProcessContext context) {
        return  context.fun.sql.getOne(
                "SELECT role.id FROM %s AS role" +
                        "  INNER JOIN %s def ON def.id = role.rel_role_def_id"+
                        "  WHERE def.code = #{role_def_code}" +
                        "   ORDER BY def.create_time ASC",
                new HashMap<>() {
                    {
                        put("role_def_code", ((IAMConfig)context.conf).getSecurity().getAppAdminRoleDefCode());
                    }
                },
                context)
                .compose(fetchRoleId -> Future.succeededFuture(fetchRoleId.getLong("role.id")));
    }

}
