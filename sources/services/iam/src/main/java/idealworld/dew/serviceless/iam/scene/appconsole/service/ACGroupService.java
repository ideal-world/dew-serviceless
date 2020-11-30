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
import idealworld.dew.serviceless.common.domain.IdEntity;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.domain.auth.*;
import idealworld.dew.serviceless.iam.enumeration.ExposeKind;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.group.*;
import idealworld.dew.serviceless.iam.scene.common.service.IAMBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
                .where(qGroup.code.eq(groupAddReq.getCode()))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_GROUP, "群组编码已存在");
        }
        var group = $.bean.copyProperties(groupAddReq, Group.class);
        group.setRelTenantId(relTenantId);
        group.setRelAppId(relAppId);
        return sendMQBySave(
                saveEntity(group),
                Group.class
        );
    }

    @Transactional
    public Resp<Void> modifyGroup(Long groupId, GroupModifyReq groupModifyReq, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        if (groupModifyReq.getCode() != null && sqlBuilder.select(qGroup.id)
                .from(qGroup)
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .where(qGroup.code.eq(groupModifyReq.getCode()))
                .where(qGroup.id.ne(groupId))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_GROUP, "群组编码已存在");
        }
        var groupUpdate = sqlBuilder.update(qGroup)
                .where(qGroup.id.eq(groupId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId));
        if (groupModifyReq.getCode() != null) {
            groupUpdate.set(qGroup.code, groupModifyReq.getCode());
        }
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
        return sendMQByUpdate(
                updateEntity(groupUpdate),
                Group.class,
                groupId
        );
    }

    public Resp<GroupResp> getGroup(Long groupId, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        return getDTO(sqlBuilder.select(Projections.bean(GroupResp.class,
                qGroup.id,
                qGroup.code,
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

    public Resp<List<GroupResp>> findGroups(Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        return findDTOs(sqlBuilder.select(Projections.bean(GroupResp.class,
                qGroup.id,
                qGroup.code,
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
                .where(qGroup.relAppId.eq(relAppId))
                .orderBy(qGroup.sort.asc()));
    }

    public Resp<List<GroupResp>> findExposeGroups(Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        return findDTOs(sqlBuilder.select(Projections.bean(GroupResp.class,
                qGroup.id,
                qGroup.code,
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
                .where((qGroup.exposeKind.eq(ExposeKind.TENANT).and(qGroup.relTenantId.eq(relTenantId)))
                        .or(qGroup.exposeKind.eq(ExposeKind.GLOBAL)))
                .where(qGroup.relAppId.ne(relAppId))
                .orderBy(qGroup.sort.asc()));
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
        return sendMQByDelete(
                softDelEntity(sqlBuilder
                        .selectFrom(qGroup)
                        .where(qGroup.id.eq(groupId))
                        .where(qGroup.relTenantId.eq(relTenantId))
                        .where(qGroup.relAppId.eq(relAppId))),
                Group.class,
                groupId
        );
    }

    // --------------------------------------------------------------------

    @Transactional
    public Resp<Long> addGroupNode(GroupNodeAddReq groupNodeAddReq, Long groupId, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        var qGroupNode = QGroupNode.groupNode;
        if (sqlBuilder.select(qGroup.id)
                .from(qGroup)
                .where(qGroup.id.eq(groupId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .fetchCount() == 0) {
            return StandardResp.unAuthorized(BUSINESS_GROUP_NODE, "关联群组不合法");
        }
        var groupNode = $.bean.copyProperties(groupNodeAddReq, GroupNode.class);
        var groupNodeCode = packageGroupNodeCode(groupId, groupNodeAddReq.getParentId(), groupNodeAddReq.getSiblingId(), null);
        if (sqlBuilder.select(qGroupNode.id)
                .from(qGroupNode)
                .where(qGroupNode.relGroupId.eq(groupId))
                .where(qGroupNode.code.eq(groupNodeCode))
                .fetchCount() != 0) {
            return StandardResp.conflict(BUSINESS_GROUP_NODE, "群组节点编码已存在");
        }
        groupNode.setRelGroupId(groupId);
        groupNode.setCode(groupNodeCode);
        return sendMQBySave(
                saveEntity(groupNode),
                GroupNode.class
        );
    }

    @Transactional
    public Resp<Void> modifyGroupNode(Long groupNodeId, GroupNodeModifyReq groupNodeModifyReq, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        var qGroupNode = QGroupNode.groupNode;
        var persistentGroupNode = sqlBuilder
                .selectFrom(qGroupNode)
                .innerJoin(qGroup).on(qGroup.id.eq(qGroupNode.relGroupId))
                .where(qGroupNode.id.eq(groupNodeId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .fetchOne();
        if (persistentGroupNode == null) {
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
        if (groupNodeModifyReq.getParentId() != Constant.OBJECT_UNDEFINED || groupNodeModifyReq.getSiblingId() != Constant.OBJECT_UNDEFINED) {
            var groupNodeCode = packageGroupNodeCode(persistentGroupNode.getRelGroupId(), groupNodeModifyReq.getParentId(),
                    groupNodeModifyReq.getSiblingId(), persistentGroupNode.getCode());
            if (sqlBuilder.select(qGroupNode.id)
                    .from(qGroupNode)
                    .where(qGroupNode.relGroupId.eq(persistentGroupNode.getRelGroupId()))
                    .where(qGroupNode.code.eq(groupNodeCode))
                    .where(qGroupNode.id.ne(groupNodeId))
                    .fetchCount() != 0) {
                return StandardResp.conflict(BUSINESS_GROUP_NODE, "群组节点编码已存在");
            }
            groupNodeUpdate.set(qGroupNode.code, groupNodeCode);
        }
        return sendMQByUpdate(
                updateEntity(groupNodeUpdate),
                GroupNode.class,
                groupNodeId
        );
    }

    public Resp<List<GroupNodeResp>> findGroupNodes(Long groupId, Long relAppId, Long relTenantId) {
        var qGroup = QGroup.group;
        var qGroupNode = QGroupNode.groupNode;
        var groupNodes = sqlBuilder
                .selectFrom(qGroupNode)
                .innerJoin(qGroup).on(qGroup.id.eq(qGroupNode.relGroupId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .where(qGroupNode.relGroupId.eq(groupId))
                .fetch()
                .stream()
                .sorted(Comparator.comparing(GroupNode::getCode))
                .collect(Collectors.toList());
        var nodeLength = iamConfig.getApp().getInitNodeCode().length();
        List<GroupNodeResp> roleNodeRespList = groupNodes.stream()
                .map(node -> {
                    var parentCode = node.getCode().substring(0, node.getCode().length() - nodeLength);
                    var parentId = parentCode.isEmpty()
                            ? Constant.OBJECT_UNDEFINED
                            : groupNodes.stream().filter(n -> n.getCode().equals(parentCode)).findAny().get().getId();
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
        var groupNode = sqlBuilder
                .selectFrom(qGroupNode)
                .innerJoin(qGroup).on(qGroup.id.eq(qGroupNode.relGroupId))
                .where(qGroupNode.id.eq(groupNodeId))
                .where(qGroup.relTenantId.eq(relTenantId))
                .where(qGroup.relAppId.eq(relAppId))
                .fetchOne();
        var deleteR = sendMQByDelete(
                softDelEntity(groupNode),
                GroupNode.class,
                groupNodeId
        );
        if (!deleteR.ok()) {
            return deleteR;
        }
        updateOtherGroupNodeCode(groupNode.getRelGroupId(), groupNode.getCode(), true, null);
        return Resp.success(null);
    }

    private String packageGroupNodeCode(Long groupId, Long parentId, Long siblingId, String originalGroupNodeCode) {
        if (parentId == Constant.OBJECT_UNDEFINED && siblingId == Constant.OBJECT_UNDEFINED) {
            var currentNodeCode = iamConfig.getApp().getInitNodeCode();
            updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode);
            return currentNodeCode;
        }
        var qGroupNode = QGroupNode.groupNode;
        if (siblingId == Constant.OBJECT_UNDEFINED) {
            var parentCode = sqlBuilder.select(qGroupNode.code)
                    .from(qGroupNode)
                    .where(qGroupNode.relGroupId.eq(groupId))
                    .where(qGroupNode.id.eq(parentId))
                    .fetchOne();
            var currentNodeCode = parentCode + iamConfig.getApp().getInitNodeCode();
            updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode);
            return currentNodeCode;
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
        updateOtherGroupNodeCode(groupId, currentNodeCode, false, originalGroupNodeCode);
        return currentNodeCode;
    }

    private void updateOtherGroupNodeCode(Long groupId, String currentGroupNodeCode, Boolean deleteOpt, String originalGroupNodeCode) {
        var qGroupNode = QGroupNode.groupNode;
        var currentNodeLength = currentGroupNodeCode.length();
        var parentLength = currentNodeLength - iamConfig.getApp().getInitNodeCode().length();
        var parentNodeCode = currentGroupNodeCode.substring(0, parentLength);
        List<GroupNode> originalGroupNodes = new ArrayList<>();
        if (originalGroupNodeCode != null) {
            var originalParentLength = originalGroupNodeCode.length() - iamConfig.getApp().getInitNodeCode().length();
            var originalParentNodeCode = originalGroupNodeCode.substring(0, originalParentLength);
            originalGroupNodes = sqlBuilder
                    .selectFrom(qGroupNode)
                    .where(qGroupNode.relGroupId.eq(groupId))
                    .where(qGroupNode.code.like(originalParentNodeCode + "%"))
                    .where(qGroupNode.code.goe(originalGroupNodeCode))
                    .fetch();
        }
        var offset = deleteOpt ? -1 : 1;
        var update = sqlBuilder
                .selectFrom(qGroupNode)
                .where(qGroupNode.relGroupId.eq(groupId))
                .where(qGroupNode.code.like(parentNodeCode + "%"))
                .where(qGroupNode.code.goe(currentGroupNodeCode));
        if (!originalGroupNodes.isEmpty()) {
            update.where(qGroupNode.id.notIn(originalGroupNodes.stream().map(IdEntity::getId).collect(Collectors.toList())));
        }
        // 排序，避免索引重复
        if (deleteOpt) {
            update.orderBy(qGroupNode.code.asc());
        } else {
            update.orderBy(qGroupNode.code.desc());
        }
        update.fetch()
                .forEach(node -> {
                    var code = node.getCode();
                    var currNodeCode = Long.parseLong(code.substring(parentLength, currentNodeLength));
                    var childNodeCode = code.substring(currentNodeLength);
                    node.setCode(parentNodeCode + (currNodeCode + offset) + childNodeCode);
                    updateEntity(node);
                });
        if (originalGroupNodeCode != null) {
            originalGroupNodes.forEach(node -> {
                var code = node.getCode();
                node.setCode(parentNodeCode + code.substring(parentLength));
                updateEntity(node);
            });
        }
    }

}
