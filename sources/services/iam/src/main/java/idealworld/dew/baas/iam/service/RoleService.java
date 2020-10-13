/*
 * Copyright 2020. the original author or authors.
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

package idealworld.dew.baas.iam.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.domain.auth.QRole;
import idealworld.dew.baas.iam.domain.auth.QRoleDef;
import idealworld.dew.baas.iam.domain.auth.Role;
import idealworld.dew.baas.iam.domain.auth.RoleDef;
import idealworld.dew.baas.iam.dto.role.RoleAddOrModifyReq;
import idealworld.dew.baas.iam.dto.role.RoleDefAddOrModifyReq;
import idealworld.dew.baas.iam.dto.role.RoleDefResp;
import idealworld.dew.baas.iam.dto.role.RoleResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Role service.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class RoleService extends IAMBasicService {

    private static final String BUSINESS_ROLE = "ROLE";
    private static final String BUSINESS_ROLE_DEF = "ROLE_DEF";

    @Transactional
    public Resp<Long> addRoleDef(RoleDefAddOrModifyReq roleDefAddReq, Long relAppId, Long relTenantId) {
        var qRoleDef = QRoleDef.roleDef;
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
        return saveEntity(roleDef);
    }

    @Transactional
    public Resp<Long> modifyRoleDef(Long roleDefId, RoleDefAddOrModifyReq roleDefModifyReq, Long relAppId, Long relTenantId) {
        var qRoleDef = QRoleDef.roleDef;
        if (sqlBuilder.select(qRoleDef.id)
                .from(qRoleDef)
                .where(qRoleDef.relTenantId.eq(relTenantId))
                .where(qRoleDef.relAppId.eq(relAppId))
                .where(qRoleDef.code.eq(roleDefModifyReq.getCode()))
                .where(qRoleDef.id.ne(roleDefId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ROLE_DEF, "角色定义编码已存在");
        }
        var roleDef = $.bean.copyProperties(roleDefModifyReq, RoleDef.class);
        roleDef.setId(roleDefId);
        roleDef.setRelTenantId(relTenantId);
        roleDef.setRelAppId(relAppId);
        return updateEntity(roleDef);
    }

    public Resp<RoleDefResp> getRoleDef(Long roleDefId, Long relAppId, Long relTenantId) {
        var qRoleDef = QRoleDef.roleDef;
        return getDTO(sqlBuilder.select(Projections.bean(RoleDefResp.class,
                qRoleDef.id,
                qRoleDef.code,
                qRoleDef.name,
                qRoleDef.sort,
                qRoleDef.exposeKind,
                qRoleDef.relAppId,
                qRoleDef.relTenantId))
                .from(qRoleDef)
                .where(qRoleDef.id.eq(roleDefId))
                .where(qRoleDef.relTenantId.eq(relTenantId))
                .where(qRoleDef.relAppId.eq(relAppId)));
    }

    public Resp<Page<RoleDefResp>> pageRoleDef(Long pageNumber, Integer pageSize, Long relAppId, Long relTenantId) {
        var qRoleDef = QRoleDef.roleDef;
        return pageDTOs(sqlBuilder.select(Projections.bean(RoleDefResp.class,
                qRoleDef.id,
                qRoleDef.code,
                qRoleDef.name,
                qRoleDef.sort,
                qRoleDef.exposeKind,
                qRoleDef.relAppId,
                qRoleDef.relTenantId))
                .from(qRoleDef)
                .where(qRoleDef.relTenantId.eq(relTenantId))
                .where(qRoleDef.relAppId.eq(relAppId)), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteRoleDef(Long roleDefId, Long relAppId, Long relTenantId) {
        var qRoleDef = QRoleDef.roleDef;
        return softDelEntity(sqlBuilder
                .selectFrom(qRoleDef)
                .where(qRoleDef.id.eq(roleDefId))
                .where(qRoleDef.relTenantId.eq(relTenantId))
                .where(qRoleDef.relAppId.eq(relAppId)));
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addRole(RoleAddOrModifyReq roleAddReq, Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        if (sqlBuilder.select(qRole.id)
                .from(qRole)
                .where(qRole.relRoleDefId.eq(roleAddReq.getRelRoleDefId()))
                .where(qRole.relGroupNodeId.eq(roleAddReq.getRelGroupNodeId()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ROLE, "角色已存在");
        }
        var role = $.bean.copyProperties(roleAddReq, Role.class);
        role.setRelTenantId(relTenantId);
        role.setRelAppId(relAppId);
        return saveEntity(role);
    }

    @Transactional
    public Resp<Long> modifyRole(Long roleId, RoleAddOrModifyReq roleModifyReq, Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        if (sqlBuilder.select(qRole.id)
                .from(qRole)
                .where(qRole.relRoleDefId.eq(roleModifyReq.getRelRoleDefId()))
                .where(qRole.relGroupNodeId.eq(roleModifyReq.getRelGroupNodeId()))
                .where(qRole.id.ne(roleId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_ROLE, "角色已存在");
        }
        var role = $.bean.copyProperties(roleModifyReq, Role.class);
        role.setId(roleId);
        role.setRelTenantId(relTenantId);
        role.setRelAppId(relAppId);
        return updateEntity(role);
    }

    public Resp<RoleResp> getRole(Long roleId, Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        return getDTO(sqlBuilder.select(Projections.bean(RoleResp.class,
                qRole.id,
                qRole.relRoleDefId,
                qRole.relGroupNodeId,
                qRole.sort,
                qRole.exposeKind,
                qRole.relAppId,
                qRole.relTenantId))
                .from(qRole)
                .where(qRole.id.eq(roleId))
                .where(qRole.relTenantId.eq(relTenantId))
                .where(qRole.relAppId.eq(relAppId)));
    }

    public Resp<Page<RoleResp>> pageRole(Long pageNumber, Integer pageSize, Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        return pageDTOs(sqlBuilder.select(Projections.bean(RoleResp.class,
                qRole.id,
                qRole.relRoleDefId,
                qRole.relGroupNodeId,
                qRole.sort,
                qRole.exposeKind,
                qRole.relAppId,
                qRole.relTenantId))
                .from(qRole)
                .where(qRole.relTenantId.eq(relTenantId))
                .where(qRole.relAppId.eq(relAppId)), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteRole(Long roleDef, Long relAppId, Long relTenantId) {
        var qRole = QRole.role;
        return softDelEntity(sqlBuilder
                .selectFrom(qRole)
                .where(qRole.id.eq(roleDef))
                .where(qRole.relTenantId.eq(relTenantId))
                .where(qRole.relAppId.eq(relAppId)));
    }

}
