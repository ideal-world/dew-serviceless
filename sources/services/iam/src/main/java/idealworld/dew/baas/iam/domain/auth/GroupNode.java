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
 * 群组节点.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_group_node", indexes = {
        @Index(columnList = "relGroupId,code", unique = true),
        @Index(columnList = "parentId")
})
@org.hibernate.annotations.Table(appliesTo = "iam_group_node",
        comment = "群组节点")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class GroupNode extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '节点编码，同租户、应用下唯一'")
    private String code;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '业务编码'")
    private String busCode;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '节点名称'")
    private String name;

    @Column(nullable = false,
            columnDefinition = "varchar(2000) comment '节点扩展信息，Json格式'")
    private String parameters;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联群组Id'")
    private Long relGroupId;

}
