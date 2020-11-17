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
import idealworld.dew.baas.common.enumeration.AuthResultKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.baas.common.enumeration.OptActionKind;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;
import java.util.Date;

/**
 * 权限策略.
 * <p>
 * 支持跨应用、租户的权限分配（发布--订阅模式）.
 * <p>
 * 仅资源所有者可以分配自己的资源权限.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_auth_policy", indexes = {
        @Index(columnList = "relResourceId,actionKind,relSubjectKind,relSubjectIds,subjectOperator,effectiveTime,expiredTime", unique = true)
})
@org.hibernate.annotations.Table(appliesTo = "iam_auth_policy",
        comment = "权限策略")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AuthPolicy extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(20) comment '关联权限主体类型名称'")
    @Enumerated(EnumType.STRING)
    private AuthSubjectKind relSubjectKind;

    @Column(nullable = false,
            columnDefinition = "varchar(10000) comment '关联权限主体Ids,有多个时逗号分隔,注意必须存在最后一个逗号'")
    private String relSubjectIds;

    @Column(nullable = false,
            columnDefinition = "varchar(20) comment '关联权限主体运算类型名称'")
    private AuthSubjectOperatorKind subjectOperator;

    @Column(nullable = false,
            columnDefinition = "timestamp comment '生效时间'")
    protected Date effectiveTime;

    @Column(nullable = false,
            columnDefinition = "timestamp comment '失效时间'")
    protected Date expiredTime;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联资源Id'")
    private Long relResourceId;

    @Column(nullable = false,
            columnDefinition = "varchar(100) comment '操作类型名称'")
    @Enumerated(EnumType.STRING)
    private OptActionKind actionKind;

    @Column(nullable = false,
            columnDefinition = "varchar(100) comment '操作结果名称'")
    @Enumerated(EnumType.STRING)
    private AuthResultKind resultKind;

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
            columnDefinition = "bigint comment '关联应用Id'")
    private Long relAppId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联租户Id'")
    private Long relTenantId;

}
