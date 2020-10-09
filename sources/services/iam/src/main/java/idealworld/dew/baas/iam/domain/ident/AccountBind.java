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

package idealworld.dew.baas.iam.domain.ident;

import idealworld.dew.baas.common.service.domain.SafeEntity;
import idealworld.dew.baas.iam.enumeration.AccountIdentKind;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

/**
 * 账号绑定.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_account_bind", indexes = {
        @Index(columnList = "fromAccountId,toAccountId", unique = true),
        @Index(columnList = "fromTenantId"),
        @Index(columnList = "toTenantId")
})
@org.hibernate.annotations.Table(appliesTo = "iam_account_bind",
        comment = "账号绑定")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AccountBind extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "bigint comment '源租户Id'")
    private Long fromTenantId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '源租户账号Id'")
    private Long fromAccountId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '目标租户Id'")
    private Long toTenantId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '目标户账号Id'")
    private Long toAccountId;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '绑定使用的账号认证类型名称'")
    @Enumerated(EnumType.STRING)
    private AccountIdentKind identKind;

}
