/*
 * Copyright 2020. gudaoxuri
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

package idealworld.dew.serviceless.iam.domain.auth;

import idealworld.dew.serviceless.common.domain.SafeEntity;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.*;


/**
 * 资源主体.
 * <p>
 * 所有三方调用都视为资源，需要配置资源主体，比如微信公众号、华为云等
 *
 * <p>
 * {@link ResourceKind#MENU} 及 {@link ResourceKind#ELEMENT}
 * uri = <空>
 * <p>
 * {@link ResourceKind#HTTP}:
 * uri = API路径
 * <p>
 * e.g.
 * <p>
 * uri = http://10.20.0.10:8080/iam
 * uri = https://iam-service/iam
 * <p>
 * {@link ResourceKind#RELDB}:
 * uri = 数据库连接地址
 * <p>
 * e.g.
 * <p>
 * uri = mysql://user1:92njc93nt39n@192.168.0.100:3306/test?useUnicode=true&characterEncoding=utf-8&rewriteBatchedStatements=true
 * uri = h2:./xyy.db;AUTO_SERVER=TRUE
 * <p>
 * {@link ResourceKind#CACHE}:
 * uri = 缓存连接地址
 * <p>
 * e.g.
 * <p>
 * uri = redis://:diwn9234@localhost:6379/1
 * <p>
 * {@link ResourceKind#MQ}:
 * uri = MQ连接地址
 * <p>
 * e.g.
 * <p>
 * uri = amqp://user1:onsw3223@localhost:10000/vhost1
 * <p>
 * {@link ResourceKind#OBJECT}:
 * uri = 对象存储连接地址
 * <p>
 * e.g.
 * <p>
 * uri = https://test-bucket.obs.cn-north-4.myhuaweicloud.com/test-object?acl
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_resource_subject", indexes = {
        @Index(columnList = "relTenantId,relAppId,code", unique = true),
        @Index(columnList = "relTenantId,relAppId,uri", unique = true),
        @Index(columnList = "relTenantId,relAppId,kind"),
        @Index(columnList = "defaultByApp")
})
@org.hibernate.annotations.Table(appliesTo = "iam_resource_subject",
        comment = "资源主体")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ResourceSubject extends SafeEntity {

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '资源主体编码'")
    private String code;

    @Column(nullable = false,
            columnDefinition = "varchar(100) comment '资源类型名称'")
    @Enumerated(EnumType.STRING)
    private ResourceKind kind;

    @Column(nullable = false,
            columnDefinition = "varchar(5000) comment '资源主体连接URI'")
    private String uri;

    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '资源主体名称'")
    private String name;

    @Column(nullable = false,
            columnDefinition = "int comment '资源主体显示排序，asc'")
    private Integer sort;

    @Column(nullable = false,
            columnDefinition = "tinyint(1) comment '是否默认'")
    private Boolean defaultByApp;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment 'AK，部分类型支持写到URI中'")
    private String ak;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment 'SK，部分类型支持写到URI中'")
    private String sk;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '第三方平台账号名'")
    private String platformAccount;

    @Column(nullable = false,
            columnDefinition = "varchar(1000) comment '第三方平台项目名，如华为云的ProjectId'")
    private String platformProjectId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联应用Id'")
    private Long relAppId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联租户Id'")
    private Long relTenantId;

}
