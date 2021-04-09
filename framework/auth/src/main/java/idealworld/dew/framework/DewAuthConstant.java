/*
 * Copyright 2021. gudaoxuri
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

package idealworld.dew.framework;

/**
 * 权限全局常量.
 *
 * @author gudaoxuri
 */
public class DewAuthConstant extends DewConstant {

    // 缓存相关：鉴权策略缓存
    public static final String CACHE_AUTH_POLICY = "dew:iam:policy:";
    // 缓存相关：AK缓存
    public static final String CACHE_APP_AK = "dew:iam:app:ak:";
    // 缓存相关：应用信息缓存
    public static final String CACHE_APP_INFO = "dew:iam:app:info:";
    // 缓存相关：token存储key : <token>:<opt info>
    public static final String CACHE_TOKEN_INFO_FLAG = "dew:iam:token:info:";
    // 缓存相关：AccountCode 关联 Tokens : <account code>:<token>:<token kind##current time>
    public static final String CACHE_TOKEN_ID_REL_FLAG = "dew:iam:token:id:rel:";

    // IAM模块名称
    public static final String MODULE_IAM_NAME = "iam";
    // 群组与节点分隔标识，用于缓存
    // TODO
    public static final String GROUP_CODE_NODE_CODE_SPLIT = "#";
    // AKSK虚拟账号
    // TODO
    public static final Long AK_SK_IDENT_ACCOUNT_FLAG = -1L;

}
