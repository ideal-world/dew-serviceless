/*
 * Copyright 2020. the original author or authors.
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

package idealworld.dew.baas.common;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.exception.RTException;

import java.text.ParseException;
import java.util.Date;

/**
 * 全局常量.
 *
 * @author gudaoxuri
 */
public class Constant {

    /**
     * 配置同步间隔时间.
     */
    public static final String CONFIG_EVENT_CONFIG_SYNC_INTERVAL_SEC = "event:config:sync:intervalsec";
    /**
     * 事件通知Topic名称.
     */
    public static final String EVENT_NOTIFY_TOPIC_BY_IAM = "event:notify:iam";


    public static final String REQUEST_PATH_FLAG = "/exec";
    public static final String REQUEST_IDENT_OPT_FLAG = "Dew-Ident-Opt";
    public static final String REQUEST_RESOURCE_URI_FLAG = "Dew-Resource-Uri";
    public static final String REQUEST_RESOURCE_ACTION_FLAG = "Dew-Resource-Action";

    /**
     * 未定义对象的标识，多用于全局Id标识.
     */
    public static final long OBJECT_UNDEFINED = 0L;
    /**
     * 鉴权策略缓存
     */
    public static final String CACHE_AUTH_POLICY = "iam:auth:policy:";
    /**
     * 最小的时间.
     */
    public static Date MIN_TIME;
    /**
     * 最大的时间.
     */
    public static Date MAX_TIME;

    public static String GROUP_CODE_NODE_CODE_SPLIT = "#";

    static {
        try {
            MIN_TIME = $.time().yyyy_MM_dd.parse("1970-01-01");
            MAX_TIME = $.time().yyyy_MM_dd.parse("3000-01-01");
        } catch (ParseException e) {
            throw new RTException(e);
        }
    }

}
