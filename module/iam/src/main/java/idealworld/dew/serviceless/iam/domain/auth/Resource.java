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

import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.serviceless.iam.domain.AppBasedEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 资源.
 * <p>
 * URI格式： resource kind://resource subject code/path?property key=property value
 * <p>
 * path 为空时 表示为（整个）该资源主体
 * <p>
 * {@link ResourceKind#HTTP}:
 * path = <API路径>
 * <p>
 * e.g.
 * <p>
 * path = admin/user
 * path = admin/**
 * <p>
 * 当 {@link ResourceSubject} 的 {@link ResourceSubject#getUri()} = http://10.20.0.10:8080/iam 时
 * 则 资源的真正URI = http://10.20.0.10:8080/iam/admin/user
 * <p>
 * {@link ResourceKind#MENU}:
 * path = <菜单树节点Id>
 * e.g.
 * <p>
 * path = userMgr/batchImport ，表示 用户管理（userMgr）/批量导入（batchImport）
 * <p>
 * {@link ResourceKind#ELEMENT}:
 * path = 页面路径/元素Id
 * 或 path = 页面路径?<属性名>=<属性值>
 * e.g.
 * <p>
 * path = userMgr/userDelete ，表示 用户管理页面（userMgr）的删除按钮（id  = 'userDelete'）
 * path = userMgr?class=userDelete ，表示 用户管理页面（userMgr）的删除按钮（class = 'userDelete'）
 * <p>
 * {@link ResourceKind#RELDB}:
 * path = <表名>
 * 或 path = <表名>/fields/<字段名>
 * 或 path = <表名>/rows/<主键值>
 * 或 path = <表名>/rows?<字段名>=<字段值>
 * <p>
 * e.g.
 * <p>
 * path = user ，表示 user表
 * path = user/fields/idcard ，表示 user表idcard字段
 * path = user/rows/100 ，表示 user表主键为100
 * path = user/rows?idcard=331xxx ，user表身份证为331xxx
 * // TODO 动态值
 * <p>
 * {@link ResourceKind#CACHE}:
 * path = <Key名称>
 * <p>
 * e.g.
 * <p>
 * path = user:ids ，表示 user:ids 的Key
 * <p>
 * {@link ResourceKind#MQ}:
 * path = <Topic名称>
 * <p>
 * e.g.
 * <p>
 * path = addUser ，表示 addUser 主题
 * <p>
 * {@link ResourceKind#OBJECT}:
 * path = <Key名称>
 * <p>
 * e.g.
 * <p>
 * path = user/100 ，表示 user/100 的key
 * <p>
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Resource extends AppBasedEntity {

    // URI
    @NotNull
    @NotBlank
    @Size(max = 5000)
    private String uri;
    // 资源名称
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String name;
    // 资源图标（路径）
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String icon;
    // 资源显示排序，asc
    @NotNull
    private Integer sort;
    // 触发后的操作，多用于菜单链接
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String action;
    // 是否是资源组
    @NotNull
    private Boolean resGroup;
    // 资源所属组Id
    @NotNull
    private Long parentId;
    // 关联资源主体Id
    @NotNull
    private Long relResourceSubjectId;

    @Override
    public String tableName() {
        return "iam_" + super.tableName();
    }

}
