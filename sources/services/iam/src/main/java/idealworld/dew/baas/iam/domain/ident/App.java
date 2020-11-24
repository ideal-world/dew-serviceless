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

import idealworld.dew.baas.common.domain.SafeEntity;
import idealworld.dew.baas.common.enumeration.CommonStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 应用.
 * <p>
 * 面向业务系统，一般而言，一个应用对应于一个业务系统，但并不强制这种一对一的关系。
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_app", indexes = {
        @Index(columnList = "relTenantId,name", unique = true),
        @Index(columnList = "status")
})
@org.hibernate.annotations.Table(appliesTo = "iam_app",
        comment = "应用")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class App extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '应用名称'")
    private String name;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '应用图标（路径）'")
    private String icon;

    @Column(nullable = false,
            columnDefinition = "varchar(2000) comment '应用扩展信息，Json格式'")
    private String parameters;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '公钥'")
    private String pubKey;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '私钥'")
    private String priKey;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联租户Id'")
    private Long relTenantId;

    @Column(nullable = false,
            columnDefinition = "varchar(50) comment '应用状态'")
    private CommonStatus status;

}
