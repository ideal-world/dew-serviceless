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

package idealworld.dew.serviceless.iam.domain.ident;

import idealworld.dew.serviceless.common.domain.SafeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 租户凭证配置.
 * <p>
 * 用于指定当前租户凭证类型及个性化配置。
 * <p>
 * 一个账号可以有一个或多个凭证，每个凭证类型有各自可保留的版本数量。
 * <p>
 * 此模型用于处理单点、单终端、多终端同时登录的问题。
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_account_cert", indexes = {
        @Index(columnList = "relTenantId,category", unique = true)
})
@org.hibernate.annotations.Table(appliesTo = "iam_account_cert",
        comment = "租户凭证配置")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TenantCert extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '凭证类型名称'")
    private String category;

    @Column(nullable = false,
            columnDefinition = "tinyint comment '凭证保留的版本数量'")
    private Integer version;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联租户Id'")
    private Long relTenantId;


}
