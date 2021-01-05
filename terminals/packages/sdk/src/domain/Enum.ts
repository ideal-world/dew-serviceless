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

export enum OptActionKind {
    /**
     * 是否存在.
     */
    EXISTS = 'EXISTS',
    /**
     * 获取.
     */
    FETCH = 'FETCH',
    /**
     * 创建.
     */
    CREATE = 'CREATE',
    /**
     * 更新.
     */
    MODIFY = 'MODIFY',
    /**
     * 局部更新（仅HTTP）.
     */
    PATCH = 'PATCH',
    /**
     * 删除.
     */
    DELETE = 'DELETE',
}

export enum CommonStatus {
    /**
     * 禁用
     */
    DISABLED = "DISABLED",

    /**
     * 启用
     */
    ENABLED = "ENABLED",
}


export enum AccountIdentKind {
    /**
     * 用户名 + 密码.
     */
    USERNAME = "USERNAME",
    /**
     * 租户间授权认证.
     */
    AUTH_IDENT = "AUTH-IDENT",
    /**
     * 手机号 + 验证码.
     */
    PHONE = "PHONE",
    /**
     * 邮箱 + 密码.
     */
    EMAIL = "EMAIL",
    /**
     * 微信小程序OAuth.
     */
    WECHAT_XCX = "WECHAT-XCX",
}

export enum ResourceKind {
    /**
     * 菜单.
     */
    MENU = "MENU",
    /**
     * 页面元素.
     */
    ELEMENT = "ELEMENT",
    /**
     * OAuth.
     */
    OAUTH = "OAUTH",
    /**
     * 关系数据库.
     */
    RELDB = "RELDB",
    /**
     * 缓存.
     */
    CACHE = "CACHE",
    /**
     * MQ.
     */
    MQ = "MQ",
    /**
     * 对象存储.
     */
    OBJECT = "OBJECT",
    /**
     * Task.
     */
    TASK = "TASK",
    /**
     * HTTP(s).
     */
    HTTP = "HTTP",
}

export enum AuthSubjectKind {
    /**
     * 租户.
     */
    TENANT = "TENANT",
    /**
     * 应用.
     */
    APP = "APP",
    /**
     * 角色.
     */
    ROLE = "ROLE",
    /**
     * 群组节点.
     */
    GROUP_NODE = "GROUP_NODE",
    /**
     * 账户.
     */
    ACCOUNT = "ACCOUNT",
}
