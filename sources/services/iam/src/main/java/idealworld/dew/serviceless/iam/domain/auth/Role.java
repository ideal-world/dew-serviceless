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

package idealworld.dew.serviceless.iam.domain.auth;

import idealworld.dew.serviceless.iam.domain.AppBasedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 角色.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_role", indexes = {
        @Index(columnList = "relRoleDefId,relGroupNodeId", unique = true),
        @Index(columnList = "exposeKind"),
})
@org.hibernate.annotations.Table(appliesTo = "iam_role",
        comment = "角色")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Role extends AppBasedEntity {

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联角色定义Id'")
    private Long relRoleDefId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联群组节点Id'")
    private Long relGroupNodeId;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '角色名称'")
    private String name;

    @Column(nullable = false,
            columnDefinition = "int comment '显示排序，asc'")
    private Integer sort;

}
