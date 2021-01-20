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

package idealworld.dew.serviceless.iam;

import idealworld.dew.framework.DewAuthConstant;

/**
 * 全局常量.
 *
 * @author gudaoxuri
 */
public class IAMConstant extends DewAuthConstant {

    public static final String CACHE_ACCESS_TOKEN = "dew:iam:oauth:access-token:";
    public static final String CACHE_ACCOUNT_VCODE_TMP_REL = "dew:iam:account:vocde:tmprel:";
    public static final String CACHE_ACCOUNT_VCODE_ERROR_TIMES = "dew:iam:account:vocde:errortimes:";

    public static final String RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT = ".";
    public static final String RESOURCE_SUBJECT_DEFAULT_CODE_POSTFIX = "default";

}
