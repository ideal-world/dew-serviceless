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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.exception.RTException;

import java.text.ParseException;

/**
 * 全局常量.
 *
 * @author gudaoxuri
 */
public class DewConstant {

    public static final String REQUEST_RESOURCE_URI_FLAG = "Dew-Resource-Uri";
    public static final String REQUEST_RESOURCE_ACTION_FLAG = "Dew-Resource-Action";
    public static final String REQUEST_WITHOUT_RESP_FLAG = "Dew-Without-Resp";
    public static final String REQUEST_IDENT_OPT_FLAG = "Dew-Ident-Opt";

    public static final String REQUEST_INNER_PATH_PREFIX = "/eb/inner/";

    public static final String PARAM_DEFAULT_TOKEN_KIND_FLAG = "DEFAULT";

    public static final String PARAM_PROFILE_KEY = "dew.profile";
    public static final String PARAM_CONFIG = "dew.config";
    public static final String PARAM_CONFIG_ITEM_PREFIX = PARAM_CONFIG + ".";
    public static final String RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT = ".";

    /**
     * 未定义对象的标识，多用于全局Id标识.
     */
    public static final long OBJECT_UNDEFINED = 0L;

    /**
     * 鉴权策略缓存.
     */
    public static final String CACHE_AUTH_POLICY = "dew:iam:policy:";
    public static final String CACHE_APP_AK = "dew:iam:app:ak:";
    public static final String CACHE_APP_INFO = "dew:iam:app:info:";

    /**
     * 最小的时间.
     */
    public static Long MIN_TIME;
    /**
     * 最大的时间.
     */
    public static Long MAX_TIME;

    static {
        try {
            MIN_TIME = $.time().yyyy_MM_dd.parse("1970-01-01").getTime();
            MAX_TIME = $.time().yyyy_MM_dd.parse("3000-01-01").getTime();
        } catch (ParseException e) {
            throw new RTException(e);
        }
    }

}
