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
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;

/**
 * 账号扩展字段配置.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_account_ext_field", indexes = {
        @Index(columnList = "relTenantId,relAppId"),
        @Index(columnList = "code")
})
@org.hibernate.annotations.Table(appliesTo = "iam_account_ext_field",
        comment = "账号扩展字段配置")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AccountExtField extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '字段编码，同租户、应用下唯一'")
    private String code;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '字段显示名称'")
    private String label;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '字段默认值'")
    private String defaultValue;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '字段数据类型'")
    private String dataType;

    @Column(nullable = false,
            columnDefinition = "tinyint(1) comment '字段是否必须'")
    private Boolean required;

    @Lob
    @Column(nullable = false,
            columnDefinition = "text comment '字段选项，Map格式'")
    private Boolean options;

    @Column(nullable = false,
            columnDefinition = "int comment '字段显示排序，asc'")
    private Integer sort;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联应用Id，为空表示租户级'")
    private Long relAppId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联租户Id'")
    private Long relTenantId;

}
