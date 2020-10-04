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
import idealworld.dew.baas.iam.enumeration.AuthActionKind;
import idealworld.dew.baas.iam.enumeration.AuthResultKind;
import idealworld.dew.baas.iam.enumeration.AuthSubjectKind;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

/**
 * 权限策略信息.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_auth_policy", indexes = {
        @Index(columnList = "relSubjectKind,relSubjectId,relResourceId,actionKind", unique = true),
        @Index(columnList = "relResourceId,actionKind")
})
@org.hibernate.annotations.Table(appliesTo = "iam_auth_policy",
        comment = "权限策略信息")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AuthPolicy extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(100) comment '关联权限主体类型名称'")
    @Enumerated(EnumType.STRING)
    private AuthSubjectKind relSubjectKind;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联权限主体Id'")
    private Long relSubjectId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联资源Id'")
    private Long relResourceId;

    @Column(nullable = false,
            columnDefinition = "varchar(100) comment '操作类型名称'")
    private AuthActionKind actionKind;

    @Column(nullable = false,
            columnDefinition = "varchar(100) comment '操作结果名称'")
    private AuthResultKind resultKind;

    @Column(nullable = false,
            columnDefinition = "varchar(5000) comment '操作结果为修正时的内容'")
    private String resultModifyContent;

    @Column(nullable = false,
            columnDefinition = "tinyint(1) comment '是否排他'")
    private Boolean exclusive;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联权限主体的应用Id'")
    private Long relSubjectAppId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联权限主体的租户Id'")
    private Long relSubjectTenantId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联资源的应用Id'")
    private Long relResourceAppId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联资源的租户Id'")
    private Long relResourceTenantId;
}
