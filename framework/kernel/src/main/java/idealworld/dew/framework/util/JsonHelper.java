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

package idealworld.dew.framework.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;


/**
 * JSON辅助类.
 *
 * @author gudaoxuri
 */
public class JsonHelper {

    public static JsonObject appendBuffer(JsonObject json, String key, Buffer value) {
        if (value.length() == 0) {
            return json.put(key, null);
        }
        var strValue = value.toString("utf-8").trim();
        if (strValue.charAt(0) == '[' && strValue.charAt(strValue.length() - 1) == ']') {
            return json.put(key, value.toJsonArray());
        }
        if (strValue.charAt(0) == '{' && strValue.charAt(strValue.length() - 1) == '}') {
            return json.put(key, value.toJsonObject());
        }
        if (strValue.equalsIgnoreCase("true")
                || strValue.equalsIgnoreCase("false")
                || strValue.chars().allMatch(Character::isDigit)) {
            return json.put(key, value.toJson());
        }
        // 使用没有trim的数据
        return json.put(key, value.toString("utf-8"));
    }

}
