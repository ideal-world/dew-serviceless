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

    // 请求相关：资源URI标识
    public static final String REQUEST_RESOURCE_URI_FLAG = "Dew-Resource-Uri";
    // 请求相关：资源操作标识
    public static final String REQUEST_RESOURCE_ACTION_FLAG = "Dew-Resource-Action";
    // 请求相关：不需要返回的请求标识
    public static final String REQUEST_WITHOUT_RESP_FLAG = "Dew-Without-Resp";
    // 请求相关：内部事件URI前缀
    public static final String REQUEST_INNER_PATH_PREFIX = "/eb/inner/";
    // 请求相关：内部认证流转标识
    public static final String REQUEST_IDENT_OPT_FLAG = "Dew-Ident-Opt";

    // 参数相关：默认TOKEN类型参数名
    public static final String PARAM_DEFAULT_TOKEN_KIND = "DEFAULT";
    // 参数相关：环境参数名
    public static final String PARAM_PROFILE_KEY = "dew.profile";
    // 参数相关：CLI参数根名
    public static final String PARAM_CONFIG = "dew.config";
    // 参数相关：CLI参数名分隔符
    public static final String PARAM_CONFIG_ITEM_PREFIX = PARAM_CONFIG + ".";

    // 资源主题分隔符
    // TODO 是否可以去掉
    public static final String RESOURCE_SUBJECT_DEFAULT_CODE_SPLIT = ".";

    // 未定义对象的标识，多用于全局Id标识
    public static final long OBJECT_UNDEFINED = 0L;
    // 最小的时间
    public static Long MIN_TIME;
    // 最大的时间
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
