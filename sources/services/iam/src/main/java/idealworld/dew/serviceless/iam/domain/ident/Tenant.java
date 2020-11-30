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

import idealworld.dew.serviceless.common.enumeration.CommonStatus;
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
 * 租户.
 * <p>
 * 数据隔离单位，不同租户间的数据不能直接共享。
 * <p>
 * 一个租户可以有多个应用。
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_tenant", indexes = {
        @Index(columnList = "status")
})
@org.hibernate.annotations.Table(appliesTo = "iam_tenant",
        comment = "租户")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Tenant extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '租户名称'")
    private String name;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '租户图标（路径）'")
    private String icon;

    @Column(nullable = false,
            columnDefinition = "tinyint(1) comment '是否开放账号注册'")
    private Boolean allowAccountRegister;

    @Column(nullable = false,
            columnDefinition = "varchar(5000) comment '租户扩展信息，Json格式'")
    private String parameters;

    @Column(nullable = false,
            columnDefinition = "varchar(50) comment '租户状态'")
    private CommonStatus status;

}
