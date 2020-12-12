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

package idealworld.dew.framework;

/**
 * 全局常量.
 *
 * @author gudaoxuri
 */
public class DewAuthConstant extends DewConstant{

    public static final String EVENT_NOTIFY_TOPIC_BY_IAM = "event:notify:iam";

    /**
     * 鉴权策略缓存
     */
    public static final String CACHE_AUTH_POLICY = "dew:iam:policy:";
    public static final String CACHE_APP_AK = "dew:iam:app:ak:";
    public static final String CACHE_APP_INFO = "dew:iam:app:info:";

    public static String GROUP_CODE_NODE_CODE_SPLIT = "#";

}
