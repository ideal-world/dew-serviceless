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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 角色定义.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_role_def", indexes = {
        @Index(columnList = "relTenantId,relAppId,code", unique = true)
})
@org.hibernate.annotations.Table(appliesTo = "iam_role_def",
        comment = "角色定义")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class RoleDef extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '角色定义编码'")
    private String code;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '角色定义名称'")
    private String name;

    @Column(nullable = false,
            columnDefinition = "int comment '显示排序，asc'")
    private Integer sort;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联应用Id'")
    private Long relAppId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联租户Id'")
    private Long relTenantId;

}
