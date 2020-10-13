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

import idealworld.dew.baas.common.domain.SafeEntity;
import idealworld.dew.baas.iam.domain.AppBasedEntity;
import idealworld.dew.baas.iam.enumeration.GroupKind;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

/**
 * 群组.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_group", indexes = {
        @Index(columnList = "relTenantId,relAppId,name", unique = true)
})
@org.hibernate.annotations.Table(appliesTo = "iam_group",
        comment = "群组")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Group extends AppBasedEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(100) comment '群组类型名称'")
    @Enumerated(EnumType.STRING)
    private GroupKind kind;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '群组名称'")
    private String name;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '群组图标（路径）'")
    private String icon;

    @Column(nullable = false,
            columnDefinition = "int comment '显示排序，asc'")
    private Integer sort;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联群组Id，用于多树合成'")
    private Long relGroupId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联群起始组节点Id，用于多树合成'")
    private Long relGroupNodeId;

}
