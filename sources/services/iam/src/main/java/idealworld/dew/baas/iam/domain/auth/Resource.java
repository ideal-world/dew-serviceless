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

import javax.persistence.*;

/**
 * 资源信息.
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
        comment = "资源信息")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Resource extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(100) comment '资源类型名称'")
    @Enumerated(EnumType.STRING)
    private ResourceKind kind;

    @Column(nullable = false,
            columnDefinition = "varchar(5000) comment 'URI'")
    private String uri;

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

    // 为空表示是系统或租户控制台资源
    @Column(nullable = false,
            columnDefinition = "bigint comment '关联应用Id'")
    private Long relAppId;

    // 为空表示是系统或租户控制台资源
    @Column(nullable = false,
            columnDefinition = "bigint comment '关联租户Id'")
    private Long relTenantId;

}
