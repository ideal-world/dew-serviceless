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
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.domain.auth.*;
import idealworld.dew.serviceless.iam.enumeration.ExposeKind;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.role.*;
import idealworld.dew.serviceless.iam.scene.common.service.IAMBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 应用控制台下的角色服务.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class ACRoleService extends IAMBasicService {

    @Autowired
    private IAMConfig iamConfig;

    @Transactional
    public Resp<Long> addRoleDef(RoleDefAddReq roleDefAddReq, Long relAppId, Long relTenantId) {
        var qRoleDef = QRoleDef.roleDef;
        roleDefAddReq.setCode(roleDefAddReq.getCode().toLowerCase());
        if (sqlBuilder.select(qRoleDef.id)
                .from(qRoleDef)
                .where(qRoleDef.relTenantId.eq(relTenantId))
                .where(qRoleDef.relAppId.eq(relAppId))
                .where(qRoleDef.code.eq(roleDefAddReq.getCode()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ROLE_DEF, "角色定义编码已存在");
        }
        var roleDef = $.bean.copyProperties(roleDefAddReq, RoleDef.class);
        roleDef.setRelTenantId(relTenantId);
        roleDef.setRelAppId(relAppId);
        return sendMQBySave(
                saveEntity(roleDef),
                RoleDef.class
        );
    }

    @Transactional
    public Resp<Void> modifyRoleDef(Long roleDefId, RoleDefModifyReq roleDefModifyReq, Long relAppId, Long relTenantId) {
        var qRoleDef = QRoleDef.roleDef;
        if (roleDefModifyReq.getCode() != null) {
            roleDefModifyReq.setCode(roleDefModifyReq.getCode().toLowerCase());
        }
        if (roleDefModifyReq.getCode() != null && sqlBuilder.select(qRoleDef.id)
                .from(qRoleDef)
                .where(qRoleDef.relTenantId.eq(relTenantId))
                .where(qRoleDef.relAppId.eq(relAppId))
                .where(qRoleDef.code.eq(roleDefModifyReq.getCode()))
                .where(qRoleDef.id.ne(roleDefId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ROLE_DEF, "角色定义编码已存在");
        }
        var roleDefUpdate = sqlBuilder.update(qRoleDef)
                .where(qRoleDef.id.eq(roleDefId))
                .where(qRoleDef.relTenantId.eq(relTenantId))
                .where(qRoleDef.relAppId.eq(relAppId));
        if (roleDefModifyReq.getCode() != null) {
            roleDefUpdate.set(qRoleDef.code, roleDefModifyReq.getCode());
        }
        if (roleDefModifyReq.getName() != null) {
            roleDefUpdate.set(qRoleDef.name, roleDefModifyReq.getName());
        }
        if (roleDefModifyReq.getSort() != null) {
            roleDefUpdate.set(qRoleDef.sort, roleDefModifyReq.getSort());
        }
        return sendMQByUpdate(
                updateEntity(roleDefUpdate),
                RoleDef.class,
                roleDefId
        );
    }

    public Resp<RoleDefResp> getRoleDef(Long roleDefId, Long relAppId, Long relTenantId) {
        var qRoleDef = QRoleDef.roleDef;
        return getDTO(sqlBuilder.select(Projections.bean(RoleDefResp.class,
                qRoleDef.id,
                qRoleDef.code,
                qRoleDef.name,
                qRoleDef.sort,
                qRoleDef.relAppId,
                qRoleDef.relTenantId))
                .from(qRoleDef)
                .where(qRoleDef.id.eq(roleDefId))
                .where(qRoleDef.relTenantId.eq(relTenantId))
                .where(qRoleDef.relAppId.eq(relAppId)));
    }

    public Resp<List<RoleDefResp>> findRoleDef(Long relAppId, Long relTenantId) {
        var qRoleDef = QRoleDef.roleDef;
        return findDTOs(sqlBuilder.select(Projections.bean(RoleDefResp.class,
                qRoleDef.id,
                qRoleDef.code,
                qRoleDef.name,
                qRoleDef.sort,
                qRoleDef.relAppId,
                qRoleDef.relTenantId))
                .from(qRoleDef)
                .where(qRoleDef.relTenantId.eq(relTenantId))
                .where(qRoleDef.relAppId.eq(relAppId))
                .orderBy(qRoleDef.sort.asc()));
    }

    @Transactional
    public Resp<Void> deleteRoleDef(Long roleDefId, Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        if (sqlBuilder.select(qRole.id)
                .from(qRole)
                .where(qRole.relRoleDefId.eq(roleDefId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ROLE_DEF, "请先删除关联的角色数据");
        }
        var qRoleDef = QRoleDef.roleDef;
        return sendMQByDelete(
                softDelEntity(sqlBuilder
                        .selectFrom(qRoleDef)
                        .where(qRoleDef.id.eq(roleDefId))
                        .where(qRoleDef.relTenantId.eq(relTenantId))
                        .where(qRoleDef.relAppId.eq(relAppId))),
                RoleDef.class,
                roleDefId
        );
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addRole(RoleAddReq roleAddReq, Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        if (sqlBuilder.select(qRole.id)
                .from(qRole)
                .where(qRole.relRoleDefId.eq(roleAddReq.getRelRoleDefId()))
                .where(qRole.relGroupNodeId.eq(roleAddReq.getRelGroupNodeId()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ROLE, "角色已存在");
        }
        var qRoleDef = QRoleDef.roleDef;
        var roleDefName = sqlBuilder.select(qRoleDef.name)
                .from(qRoleDef)
                .where(qRoleDef.id.eq(roleAddReq.getRelRoleDefId()))
                .where(qRoleDef.relTenantId.eq(relTenantId))
                .where(qRoleDef.relAppId.eq(relAppId))
                .fetchOne();
        if (roleDefName == null) {
            return StandardResp.unAuthorized(BUSINESS_ROLE, "对应的角色定义不合法");
        }
        if (roleAddReq.getName() == null) {
            if (roleAddReq.getRelGroupNodeId() != Constant.OBJECT_UNDEFINED) {
                var qGroup = QGroup.group;
                var qGroupNode = QGroupNode.groupNode;
                var groupNodeName = sqlBuilder
                        .select(qGroupNode.name)
                        .from(qGroupNode)
                        .innerJoin(qGroup).on(qGroup.id.eq(qGroupNode.relGroupId))
                        .where(qGroupNode.id.eq(roleAddReq.getRelGroupNodeId()))
                        .where(qGroup.relTenantId.eq(relTenantId))
                        .where(qGroup.relAppId.eq(relAppId))
                        .fetchOne();
                if (groupNodeName == null) {
                    return StandardResp.unAuthorized(BUSINESS_ROLE, "对应的群组节点不合法");
                }
                roleAddReq.setName(groupNodeName + " ");
            } else {
                roleAddReq.setName("");
            }
            roleAddReq.setName(roleAddReq.getName() + roleDefName);
        }
        var role = $.bean.copyProperties(roleAddReq, Role.class);
        role.setRelTenantId(relTenantId);
        role.setRelAppId(relAppId);
        return sendMQBySave(
                saveEntity(role),
                Role.class
        );
    }

    @Transactional
    public Resp<Void> modifyRole(Long roleId, RoleModifyReq roleModifyReq, Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        var roleUpdate = sqlBuilder.update(qRole)
                .where(qRole.id.eq(roleId))
                .where(qRole.relTenantId.eq(relTenantId))
                .where(qRole.relAppId.eq(relAppId));
        if (roleModifyReq.getName() != null) {
            roleUpdate.set(qRole.name, roleModifyReq.getName());
        }
        if (roleModifyReq.getSort() != null) {
            roleUpdate.set(qRole.sort, roleModifyReq.getSort());
        }
        if (roleModifyReq.getExposeKind() != null) {
            roleUpdate.set(qRole.exposeKind, roleModifyReq.getExposeKind());
        }
        return sendMQByUpdate(
                updateEntity(roleUpdate),
                Role.class,
                roleId
        );
    }

    public Resp<RoleResp> getRole(Long roleId, Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        return getDTO(sqlBuilder.select(Projections.bean(RoleResp.class,
                qRole.id,
                qRole.relRoleDefId,
                qRole.relGroupNodeId,
                qRole.name,
                qRole.sort,
                qRole.exposeKind,
                qRole.relAppId,
                qRole.relTenantId))
                .from(qRole)
                .where(qRole.id.eq(roleId))
                .where(qRole.relTenantId.eq(relTenantId))
                .where(qRole.relAppId.eq(relAppId)));
    }

    public Resp<List<RoleResp>> findRoles(Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        return findDTOs(sqlBuilder.select(Projections.bean(RoleResp.class,
                qRole.id,
                qRole.relRoleDefId,
                qRole.relGroupNodeId,
                qRole.name,
                qRole.sort,
                qRole.exposeKind,
                qRole.relAppId,
                qRole.relTenantId))
                .from(qRole)
                .where(qRole.relTenantId.eq(relTenantId))
                .where(qRole.relAppId.eq(relAppId))
                .orderBy(qRole.sort.asc()));
    }

    public Resp<List<RoleResp>> findExposeRoles(Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        return findDTOs(sqlBuilder.select(Projections.bean(RoleResp.class,
                qRole.id,
                qRole.relRoleDefId,
                qRole.relGroupNodeId,
                qRole.name,
                qRole.sort,
                qRole.exposeKind,
                qRole.relAppId,
                qRole.relTenantId))
                .from(qRole)
                .where((qRole.exposeKind.eq(ExposeKind.TENANT).and(qRole.relTenantId.eq(relTenantId)))
                        .or(qRole.exposeKind.eq(ExposeKind.GLOBAL)))
                .where(qRole.relAppId.ne(relAppId))
                .orderBy(qRole.sort.asc()));
    }

    @Transactional
    public Resp<Void> deleteRole(Long roleId, Long relAppId, Long relTenantId) {
        var qAccountRole = QAccountRole.accountRole;
        if (sqlBuilder.select(qAccountRole.id)
                .from(qAccountRole)
                .where(qAccountRole.relRoleId.eq(roleId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ROLE, "请先删除关联的账号角色数据");
        }
        var qAuthPolicy = QAuthPolicy.authPolicy;
        if (sqlBuilder.select(qAuthPolicy.id)
                .from(qAuthPolicy)
                .where(qAuthPolicy.relSubjectKind.eq(AuthSubjectKind.ROLE))
                .where(qAuthPolicy.relSubjectIds.like("%" + roleId + ",%"))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ROLE, "请先删除关联的权限策略数据");
        }
        var qRole = QRole.role;
        return sendMQByDelete(
                softDelEntity(sqlBuilder
                        .selectFrom(qRole)
                        .where(qRole.id.eq(roleId))
                        .where(qRole.relTenantId.eq(relTenantId))
                        .where(qRole.relAppId.eq(relAppId))),
                Role.class,
                roleId
        );
    }

    public Resp<Long> getTenantAdminRoleId() {
        var qRole = QRole.role;
        var qRoleDef = QRoleDef.roleDef;
        return Resp.success(sqlBuilder.select(qRole.id)
                .from(qRole)
                .innerJoin(qRoleDef).on(qRoleDef.id.eq(qRole.relRoleDefId))
                .where(qRoleDef.code.eq(iamConfig.getSecurity().getTenantAdminRoleDefCode().toLowerCase()))
                .orderBy(qRoleDef.createTime.asc())
                .fetchOne());
    }

    public Resp<Long> getAppAdminRoleId() {
        var qRole = QRole.role;
        var qRoleDef = QRoleDef.roleDef;
        return Resp.success(sqlBuilder.select(qRole.id)
                .from(qRole)
                .innerJoin(qRoleDef).on(qRoleDef.id.eq(qRole.relRoleDefId))
                .where(qRoleDef.code.eq(iamConfig.getSecurity().getAppAdminRoleDefCode().toLowerCase()))
                .orderBy(qRoleDef.createTime.asc())
                .fetchOne());
    }

}
