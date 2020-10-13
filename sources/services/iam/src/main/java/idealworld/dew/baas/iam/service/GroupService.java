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
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.iam.IAMConfig;
import idealworld.dew.baas.iam.domain.auth.Group;
import idealworld.dew.baas.iam.domain.auth.GroupNode;
import idealworld.dew.baas.iam.domain.auth.QGroup;
import idealworld.dew.baas.iam.domain.auth.QGroupNode;
import idealworld.dew.baas.iam.dto.group.GroupAddOrModifyReq;
import idealworld.dew.baas.iam.dto.group.GroupNodeAddOrModifyReq;
import idealworld.dew.baas.iam.dto.group.GroupNodeResp;
import idealworld.dew.baas.iam.dto.group.GroupResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Group service.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class GroupService extends IAMBasicService {

    private static final String BUSINESS_GROUP = "GROUP";
    private static final String BUSINESS_GROUP_NODE = "GROUP_NODE";

    @Autowired
    private IAMConfig iamConfig;

    @Transactional
    public Resp<Long> addGroup(GroupAddOrModifyReq groupAddReq, Long relAppId, Long relTenantId) {
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
    public Resp<Long> modifyGroup(Long groupId, GroupAddOrModifyReq groupModifyReq, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        if (sqlBuilder.select(qGroup.id)
                .from(qGroup)
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .where(qGroup.name.eq(groupModifyReq.getName()))
                .where(qGroup.id.ne(groupId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_GROUP, "群组名称已存在");
        }
        var group = $.bean.copyProperties(groupModifyReq, Group.class);
        group.setId(groupId);
        group.setRelTenantId(relTenantId);
        group.setRelAppId(relAppId);
        return updateEntity(group);
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

    public Resp<Page<GroupResp>> pageRoleDef(Long pageNumber, Integer pageSize, Long relAppId, Long relTenantId) {
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
        var qGroup = QGroup.group;
        return softDelEntity(sqlBuilder
                .selectFrom(qGroup)
                .where(qGroup.id.eq(groupId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId)));
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addGroupNode(GroupNodeAddOrModifyReq groupNodeAddReq, Long relAppId, Long relTenantId) {
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
    public Resp<Long> modifyGroupNode(Long groupNodeId, GroupNodeAddOrModifyReq groupNodeModifyReq, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        if (sqlBuilder.select(qGroup.id)
                .from(qGroup)
                .where(qGroup.id.eq(groupNodeModifyReq.getRelGroupId()))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_GROUP_NODE, "关联群组不合法");
        }
        var groupNode = $.bean.copyProperties(groupNodeModifyReq, GroupNode.class);
        var groupNodeCode = packageGroupNodeCode(groupNodeModifyReq.getRelGroupId(), groupNodeModifyReq.getParentId(), groupNodeModifyReq.getSiblingId());
        groupNode.setId(groupNodeId);
        groupNode.setCode(groupNodeCode);
        return updateEntity(groupNode);
    }

    private String packageGroupNodeCode(Long relGroupId, Long parentId, Long siblingId) {
        if (parentId.longValue() == Constant.OBJECT_UNDEFINED && siblingId.longValue() == Constant.OBJECT_UNDEFINED) {
            return iamConfig.getApp().getInitNodeCode();
        }
        var qGroupNode = QGroupNode.groupNode;
        if (siblingId.longValue() == Constant.OBJECT_UNDEFINED) {
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
        var qGroup = QGroup.group;
        var qGroupNode = QGroupNode.groupNode;
        return softDelEntity(sqlBuilder
                .selectFrom(qGroup)
                .from(qGroupNode)
                .leftJoin(qGroup).on(qGroup.id.eq(qGroupNode.relGroupId))
                .where(qGroupNode.id.eq(groupNodeId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId)));
    }

}
