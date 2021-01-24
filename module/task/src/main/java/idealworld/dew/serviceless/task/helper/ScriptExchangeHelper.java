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

package idealworld.dew.serviceless.task.helper;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.exception.RTScriptException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * 脚本语言交互辅助类.
 *
 * @author gudaoxuri
 */
@Slf4j
public class ScriptExchangeHelper {

    public static Object req(String url, Map header, Object data) {
        var result = $.http.post(url, data, header);
        log.trace("[Script]Http POST request {} response: {}", url, result);
        return result;
    }

    public static String currentTime() {
        var sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }

    public static String signature(String text,String key) {
        return  $.security.encodeStringToBase64($.security.digest.digest((text).toLowerCase(),key, "HmacSHA1"),StandardCharsets.UTF_8);
    }

    public static void info(String msg) {
        log.info("[Script]Info: {}", msg);
    }

    public static void error(String msg) {
        log.warn("[Script]Error: {}", msg);
        throw new RTScriptException(msg);
    }

}
