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
     * 角色分隔线.
     */
    public static final String ROLE_SPLIT = "-";
    /**
     * 未定义对象的标识，多用于全局Id标识.
     */
    public static final long OBJECT_UNDEFINED = 0L;

    /**
     * 最小的时间.
     */
    public static Date MIN_TIME;
    /**
     * 最大的时间.
     */
    public static Date MAX_TIME;

    static {
        try {
            MIN_TIME = $.time().yyyy_MM_dd.parse("1970-01-01");
            MAX_TIME = $.time().yyyy_MM_dd.parse("3000-01-01");
        } catch (ParseException e) {
            throw new RTException(e);
        }
    }

}
