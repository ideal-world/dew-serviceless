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

    /**
     * 鉴权策略缓存
     */
    public static final String CACHE_AUTH_POLICY = "dew:iam:policy:";
    public static final String CACHE_APP_AK = "dew:iam:app:ak:";
    public static final String CACHE_APP_INFO = "dew:iam:app:info:";
    // token存储key : <token>:<opt info>
    public static final String CACHE_TOKEN_INFO_FLAG = "dew:iam:token:info:";
    // AccountCode 关联 Tokens : <account code>:<token kind##current time>:<token>
    public static final String CACHE_TOKEN_ID_REL_FLAG = "dew:iam:token:id:rel:";
    public static final String MODULE_IAM_NAME = "iam";
    public static final String GROUP_CODE_NODE_CODE_SPLIT = "#";
    public static final String AK_SK_IDENT_ACCOUNT_FLAG = "AK-SK-LOGIN";

}
