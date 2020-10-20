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

package idealworld.dew.baas.iam.scene.appconsole.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.core.types.Projections;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.IAMConfig;
import idealworld.dew.baas.iam.domain.auth.*;
import idealworld.dew.baas.iam.enumeration.AuthSubjectKind;
import idealworld.dew.baas.iam.scene.appconsole.dto.group.*;
import idealworld.dew.baas.iam.scene.common.service.IAMBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 应用控制台下的群组服务.
 * <p>
 * // TODO 不同组合成一个组
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class ACGroupService extends IAMBasicService {

    @Autowired
    private IAMConfig iamConfig;

    @Transactional
    public Resp<Long> addGroup(GroupAddReq groupAddReq, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        if (sqlBuilder.select(qGroup.id)
                .from(qGroup)
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .where(qGroup.name.eq(groupAddReq.getName()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_GROUP, "群组名称已存在");
        }
        var group = $.bean.copyProperties(groupAddReq, Group.class);
        group.setRelTenantId(relTenantId);
        group.setRelAppId(relAppId);
        return saveEntity(group);
    }

    @Transactional
    public Resp<Void> modifyGroup(Long groupId, GroupModifyReq groupModifyReq, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        if (groupModifyReq.getName() != null && sqlBuilder.select(qGroup.id)
                .from(qGroup)
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .where(qGroup.name.eq(groupModifyReq.getName()))
                .where(qGroup.id.ne(groupId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_GROUP, "群组名称已存在");
        }
        var groupUpdate = sqlBuilder.update(qGroup)
                .where(qGroup.id.eq(groupId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId));
        if (groupModifyReq.getKind() != null) {
            groupUpdate.set(qGroup.kind, groupModifyReq.getKind());
        }
        if (groupModifyReq.getName() != null) {
            groupUpdate.set(qGroup.name, groupModifyReq.getName());
        }
        if (groupModifyReq.getIcon() != null) {
            groupUpdate.set(qGroup.icon, groupModifyReq.getIcon());
        }
        if (groupModifyReq.getSort() != null) {
            groupUpdate.set(qGroup.sort, groupModifyReq.getSort());
        }
        if (groupModifyReq.getRelGroupId() != null) {
            groupUpdate.set(qGroup.relGroupId, groupModifyReq.getRelGroupId());
        }
        if (groupModifyReq.getRelGroupNodeId() != null) {
            groupUpdate.set(qGroup.relGroupNodeId, groupModifyReq.getRelGroupNodeId());
        }
        if (groupModifyReq.getExposeKind() != null) {
            groupUpdate.set(qGroup.exposeKind, groupModifyReq.getExposeKind());
        }
        return updateEntity(groupUpdate);
    }

    public Resp<GroupResp> getGroup(Long groupId, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        return getDTO(sqlBuilder.select(Projections.bean(GroupResp.class,
                qGroup.id,
                qGroup.kind,
                qGroup.name,
                qGroup.icon,
                qGroup.sort,
                qGroup.relGroupId,
                qGroup.relGroupNodeId,
                qGroup.exposeKind,
                qGroup.relAppId,
                qGroup.relTenantId))
                .from(qGroup)
                .where(qGroup.id.eq(groupId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId)));
    }

    public Resp<Page<GroupResp>> pageRoleDefs(Long pageNumber, Integer pageSize, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        return pageDTOs(sqlBuilder.select(Projections.bean(GroupResp.class,
                qGroup.id,
                qGroup.kind,
                qGroup.name,
                qGroup.icon,
                qGroup.sort,
                qGroup.relGroupId,
                qGroup.relGroupNodeId,
                qGroup.exposeKind,
                qGroup.relAppId,
                qGroup.relTenantId))
                .from(qGroup)
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId)), pageNumber, pageSize);
    }

    @Transactional
    public Resp<Void> deleteGroup(Long groupId, Long relAppId, Long relTenantId) {
        var qGroupNode = QGroupNode.groupNode;
        if (sqlBuilder.select(qGroupNode.id)
                .from(qGroupNode)
                .where(qGroupNode.relGroupId.eq(groupId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_GROUP, "请先删除关联的群组节点数据");
        }
        var qGroup = QGroup.group;
        return softDelEntity(sqlBuilder
                .selectFrom(qGroup)
                .where(qGroup.id.eq(groupId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId)));
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addGroupNode(GroupNodeAddReq groupNodeAddReq, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        if (sqlBuilder.select(qGroup.id)
                .from(qGroup)
                .where(qGroup.id.eq(groupNodeAddReq.getRelGroupId()))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_GROUP_NODE, "关联群组不合法");
        }
        var groupNode = $.bean.copyProperties(groupNodeAddReq, GroupNode.class);
        var groupNodeCode = packageGroupNodeCode(groupNodeAddReq.getRelGroupId(), groupNodeAddReq.getParentId(), groupNodeAddReq.getSiblingId());
        groupNode.setCode(groupNodeCode);
        return saveEntity(groupNode);
    }

    @Transactional
    public Resp<Void> modifyGroupNode(Long groupNodeId, GroupNodeModifyReq groupNodeModifyReq, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        var qGroupNode = QGroupNode.groupNode;
        var relGroupId = sqlBuilder.select(qGroupNode.relGroupId)
                .from(qGroupNode)
                .leftJoin(qGroup).on(qGroup.id.eq(qGroupNode.relGroupId))
                .where(qGroupNode.id.eq(groupNodeId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .fetchOne();
        if (relGroupId == null) {
            return StandardResp.unAuthorized(BUSINESS_GROUP_NODE, "关联群组不合法");
        }
        var groupNodeUpdate = sqlBuilder.update(qGroupNode)
                .where(qGroupNode.id.eq(groupNodeId));
        if (groupNodeModifyReq.getBusCode() != null) {
            groupNodeUpdate.set(qGroupNode.busCode, groupNodeModifyReq.getBusCode());
        }
        if (groupNodeModifyReq.getName() != null) {
            groupNodeUpdate.set(qGroupNode.name, groupNodeModifyReq.getName());
        }
        if (groupNodeModifyReq.getParameters() != null) {
            groupNodeUpdate.set(qGroupNode.parameters, groupNodeModifyReq.getParameters());
        }
        if (groupNodeModifyReq.getParentId() != null || groupNodeModifyReq.getSiblingId() != null) {
            var groupNodeCode = packageGroupNodeCode(relGroupId, groupNodeModifyReq.getParentId(), groupNodeModifyReq.getSiblingId());
            groupNodeUpdate.set(qGroupNode.code, groupNodeCode);
        }
        return updateEntity(groupNodeUpdate);
    }

    private String packageGroupNodeCode(Long relGroupId, Long parentId, Long siblingId) {
        if (parentId == Constant.OBJECT_UNDEFINED && siblingId == Constant.OBJECT_UNDEFINED) {
            return iamConfig.getApp().getInitNodeCode();
        }
        var qGroupNode = QGroupNode.groupNode;
        if (siblingId == Constant.OBJECT_UNDEFINED) {
            var parentCode = sqlBuilder.select(qGroupNode.code)
                    .from(qGroupNode)
                    .where(qGroupNode.relGroupId.eq(relGroupId))
                    .where(qGroupNode.id.eq(parentId))
                    .fetchOne();
            return parentCode + iamConfig.getApp().getInitNodeCode();
        }
        var siblingCode = sqlBuilder.select(qGroupNode.code)
                .from(qGroupNode)
                .where(qGroupNode.id.eq(siblingId))
                .fetchOne();
        var currentNodeLength = siblingCode.length();
        var parentLength = currentNodeLength - iamConfig.getApp().getInitNodeCode().length();
        var parentCode = siblingCode.substring(0, parentLength);
        var currentLevelCode = Long.parseLong(siblingCode.substring(parentLength));
        var currentNodeCode = parentCode + (currentLevelCode + 1);
        sqlBuilder
                .selectFrom(qGroupNode)
                .where(qGroupNode.relGroupId.eq(relGroupId))
                .where(qGroupNode.code.like(parentCode + "%"))
                .where(qGroupNode.code.goe(siblingCode))
                .fetch()
                .forEach(node -> {
                    // 当前节点+1
                    var code = node.getCode();
                    var currNodeCode = Long.parseLong(code.substring(parentLength, currentNodeLength));
                    var childNodeCode = code.substring(currentNodeLength);
                    node.setCode(parentCode + (currNodeCode + 1) + childNodeCode);
                    updateEntity(node);
                });
        return currentNodeCode;
    }

    public Resp<List<GroupNodeResp>> findRoleNodes(Long relGroupId, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        var qGroupNode = QGroupNode.groupNode;
        var roleNodes = sqlBuilder
                .selectFrom(qGroupNode)
                .leftJoin(qGroup).on(qGroup.id.eq(qGroupNode.relGroupId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .where(qGroupNode.relGroupId.eq(relGroupId))
                .fetch()
                .stream()
                .sorted(Comparator.comparing(GroupNode::getCode))
                .collect(Collectors.toList());
        var nodeLength = iamConfig.getApp().getInitNodeCode().length();
        List<GroupNodeResp> roleNodeRespList = roleNodes.stream()
                .map(node -> {
                    var parentCode = node.getCode().substring(0, node.getCode().length() - nodeLength);
                    var parentId = parentCode.isEmpty()
                            ? Constant.OBJECT_UNDEFINED
                            : roleNodes.stream().filter(n -> n.getCode().equals(parentCode)).findAny().get().getId();
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
        return Resp.success(roleNodeRespList);
    }

    @Transactional
    public Resp<Void> deleteGroupNode(Long groupNodeId, Long relAppId, Long relTenantId) {
        var qAccountGroup = QAccountGroup.accountGroup;
        if (sqlBuilder.select(qAccountGroup.id)
                .from(qAccountGroup)
                .where(qAccountGroup.relGroupNodeId.eq(groupNodeId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_GROUP_NODE, "请先删除关联的账号群组数据");
        }
        var qAuthPolicy = QAuthPolicy.authPolicy;
        if (sqlBuilder.select(qAuthPolicy.id)
                .from(qAuthPolicy)
                .where(qAuthPolicy.relSubjectKind.eq(AuthSubjectKind.GROUP_NODE))
                .where(qAuthPolicy.relSubjectIds.like("%" + groupNodeId + ",%"))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_GROUP_NODE, "请先删除关联的权限策略数据");
        }
        var qGroup = QGroup.group;
        var qGroupNode = QGroupNode.groupNode;
        return softDelEntity(sqlBuilder
                .selectFrom(qGroupNode)
                .leftJoin(qGroup).on(qGroup.id.eq(qGroupNode.relGroupId))
                .where(qGroupNode.id.eq(groupNodeId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId)));
    }

}
