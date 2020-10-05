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

import idealworld.dew.baas.common.enumeration.CommonStatus;
import idealworld.dew.baas.common.service.domain.SafeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 账号.
 * <p>
 * 用户身份单位。
 * <p>
 * 隶属于租户，即便是同一个自然人在不同租户间也会有各自的账号，同一租户的不同应用共享账号信息。
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_account", indexes = {
        @Index(columnList = "relTenantId,status"),
        @Index(columnList = "openId", unique = true)
})
@org.hibernate.annotations.Table(appliesTo = "iam_account",
        comment = "账号")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Account extends SafeEntity {

    /**
     * OpenId是账号对外提供的主键，给业务方使用，用于标识账号唯一性的字段。
     */
    @Column(nullable = false,
            columnDefinition = "varchar(100) comment 'Open Id'")
    private String openId;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '账号名称'")
    private String name;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '账号头像（路径）'")
    private String avatar;

    @Column(nullable = false,
            columnDefinition = "varchar(2000) comment '账号扩展信息，Json格式'")
    private String parameters;

    /**
     * 父子账号可用于支持RAM（ Resource Access Management）用户功能。
     * <p>
     * 父子账号间OpenId相同。
     */
    @Column(nullable = false,
            columnDefinition = "bigint comment '父账号Id，不存在时为空'")
    private Long parentId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联租户Id'")
    private Long relTenantId;

    @Column(nullable = false,
            columnDefinition = "varchar(50) comment '账号状态'")
    private CommonStatus status;

}
