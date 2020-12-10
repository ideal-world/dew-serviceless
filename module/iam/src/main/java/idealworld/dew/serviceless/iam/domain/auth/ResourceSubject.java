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

import idealworld.dew.framework.domain.SafeEntity;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 资源主体.
 * <p>
 * 所有三方调用都视为资源，需要配置资源主体，比如微信公众号、华为云等
 * <p>
 * code = <appId>.<kind>.<code | default>
 * <p>
 * {@link ResourceKind#MENU}:
 * uri = MENU路径
 * <p>
 * e.g.
 * <p>
 * uri = menu:///
 * <p>
 * {@link ResourceKind#ELEMENT}:
 * uri = 元素路径
 * <p>
 * e.g.
 * <p>
 * uri = element:///
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
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ResourceSubject extends SafeEntity {

    @Override
    public String tableName() {
        return "iam_"+super.tableName();
    }

    // 资源主体编码
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String code;
    // 资源类型名称
    @NotNull
    private ResourceKind kind;
    // 资源主体连接URI
    @NotNull
    @NotBlank
    @Size(max = 5000)
    private String uri;
    // 资源主体名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String name;
    // 资源主体显示排序，asc
    @NotNull
    private Integer sort;
    // AK，部分类型支持写到URI中
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String ak;
    // SK，部分类型支持写到URI中
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String sk;
    // 第三方平台账号名
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String platformAccount;
    // 第三方平台项目名，如华为云的ProjectId
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String platformProjectId;
    // 执行超时
    @NotNull
    private Long timeoutMS;
    // 关联应用Id
    @NotNull
    private Long relAppId;
    // 关联租户Id
    @NotNull
    private Long relTenantId;

}
