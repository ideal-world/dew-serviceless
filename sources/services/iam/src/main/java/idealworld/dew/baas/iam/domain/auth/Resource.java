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

package idealworld.dew.baas.iam.domain.auth;

import idealworld.dew.baas.common.service.domain.SafeEntity;
import idealworld.dew.baas.iam.enumeration.ResourceKind;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 资源.
 * <p>
 * URI格式： <resource kind>://<resource subject code>/<[path]>[?<property key>=<property value>]
 * <p>
 * {@link #relAppId} 及 {@link #relTenantId} 继承自 {@link ResourceSubject} 但可以重写后者，实际使用以此为准
 * <p>
 * {@link #relAppId} 及 {@link #relTenantId} 不为空时 表示应用私有资源
 * {@link #relAppId} 为空时 表示租户级资源
 * {@link #relTenantId} 为空时 表示全局资源
 * <path> 为空时 表示为（整个）该资源主体
 * expose = ture 时 表示该资源（无论是否是私有还是租户级）可以开放订阅
 * <p>
 * {@link ResourceKind#API}:
 * path = <API路径>
 * <p>
 * e.g.
 * <p>
 * path = admin/user
 * <p>
 * 当 {@link ResourceSubject} 的 {@link ResourceSubject#getUri()} = http://10.20.0.10:8080/iam 时
 * 则 资源的真正URI = http://10.20.0.10:8080/iam/admin/user
 * <p>
 * {@link ResourceKind#MENU}:
 * path = <菜单树节点Id>
 * e.g.
 * <p>
 * path = userMgr/batchImport ，表示 用户管理（userMgr）/批量导入（batchImport）
 * <p>
 * {@link ResourceKind#ELEMENT}:
 * path = 页面路径/元素Id
 * 或 path = 页面路径?<属性名>=<属性值>
 * e.g.
 * <p>
 * path = userMgr/userDelete ，表示 用户管理页面（userMgr）的删除按钮（id  = 'userDelete'）
 * path = userMgr?class=userDelete ，表示 用户管理页面（userMgr）的删除按钮（class = 'userDelete'）
 * <p>
 * {@link ResourceKind#RELDB}:
 * path = <表名>
 * 或 path = <表名>/fields/<字段名>
 * 或 path = <表名>/rows/<主键值>
 * 或 path = <表名>/rows?<字段名>=<字段值>
 * <p>
 * e.g.
 * <p>
 * path = user ，表示 user表
 * path = user/fields/idcard ，表示 user表idcard字段
 * path = user/rows/100 ，表示 user表主键为100
 * path = user/rows?idcard=331xxx ，user表身份证为331xxx
 * // TODO 动态值
 * <p>
 * {@link ResourceKind#CACHE}:
 * path = <Key名称>
 * <p>
 * e.g.
 * <p>
 * path = user:ids ，表示 user:ids 的Key
 * <p>
 * {@link ResourceKind#MQ}:
 * path = <Topic名称>
 * <p>
 * e.g.
 * <p>
 * path = addUser ，表示 addUser 主题
 * <p>
 * {@link ResourceKind#OBJECT}:
 * path = <Key名称>
 * <p>
 * e.g.
 * <p>
 * path = user/100 ，表示 user/100 的key
 * <p>
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_resource", indexes = {
        @Index(columnList = "uri", unique = true),
        @Index(columnList = "relTenantId,relAppId"),
        @Index(columnList = "parentId")
})
@org.hibernate.annotations.Table(appliesTo = "iam_resource",
        comment = "资源")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Resource extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(5000) comment 'URI'")
    private String uri;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '触发后的操作，多用于菜单链接'")
    private String action;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '资源名称'")
    private String name;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '资源图标（路径）'")
    private String icon;

    @Column(nullable = false,
            columnDefinition = "int comment '资源显示排序，asc'")
    private Integer sort;

    @Column(nullable = false,
            columnDefinition = "bigint comment '资源所属组Id'")
    private Long parentId;

    @Column(nullable = false,
            columnDefinition = "tinyint(1) comment '是否公开'")
    private Boolean expose;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联资源主体Id'")
    private Long relResourceSubject;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联应用Id'")
    private Long relAppId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联租户Id'")
    private Long relTenantId;

}
