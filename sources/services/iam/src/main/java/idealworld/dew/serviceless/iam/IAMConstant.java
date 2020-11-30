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

package idealworld.dew.serviceless.iam;

import idealworld.dew.serviceless.common.Constant;

/**
 * 常量池.
 *
 * @author gudaoxuri
 */
public class IAMConstant extends Constant {

    public static final String CACHE_APP_AK = "dew:auth:app:ak:";
    public static final String CACHE_APP_INFO = "dew:auth:app:";
    public static final String CACHE_ACCESS_TOKEN = "dew:auth:oauth:access-token:";
    public static final String CACHE_ACCOUNT_VCODE_TMP_REL = "dew:auth:account:vocde:tmprel:";
    public static final String CACHE_ACCOUNT_VCODE_ERROR_TIMES = "dew:auth:account:vocde:errortimes:";

    public static final String CONFIG_TENANT_REGISTER_ALLOW = "tenant:register:allow";
    public static final String CONFIG_ACCOUNT_VCODE_EXPIRE_SEC = "account:vcode:expiresec";
    public static final String CONFIG_ACCOUNT_VCODE_ERROR_TIMES = "account:vcode:errortimes";

    public static final String CONFIG_AUTH_POLICY_MAX_FETCH_COUNT = "iam:auth:policy:fetchcount:max";
    public static final String CONFIG_AUTH_POLICY_EXPIRE_CLEAN_INTERVAL_SEC = "iam:auth:policy:expire:clean:intervalsec";


}


