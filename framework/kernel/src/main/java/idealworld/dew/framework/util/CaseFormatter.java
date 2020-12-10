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

package idealworld.dew.framework.util;

import io.vertx.core.json.JsonObject;

/**
 * @author gudaoxuri
 */
public class CaseFormatter {

    public static String camelToSnake(String camel) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char ch = camel.charAt(i);
            if (Character.isUpperCase(ch)) {
                result.append('_');
            }
            result.append(Character.toLowerCase(ch));
        }
        return result.toString();
    }

    public static String snakeToCamel(String snake) {
        StringBuilder result = new StringBuilder();
        boolean startWorld = false;
        for (int i = 0; i < snake.length(); i++) {
            char ch = snake.charAt(i);
            if (ch == '_') {
                startWorld = true;
            } else if (startWorld) {
                result.append(Character.toUpperCase(ch));
                startWorld = false;
            } else {
                result.append(Character.toLowerCase(ch));
            }
        }
        return result.toString();
    }

    public static JsonObject camelToSnake(JsonObject json) {
        var formattedJson = new JsonObject();
        json.forEach(j -> {
            var key = camelToSnake(j.getKey());
            var value = j.getValue();
            if (value instanceof JsonObject) {
                formattedJson.put(key, camelToSnake((JsonObject) value));
            } else {
                formattedJson.put(key, value);
            }
        });
        return formattedJson;
    }

    public static JsonObject snakeToCamel(JsonObject json) {
        var formattedJson = new JsonObject();
        json.forEach(j -> {
            var key = snakeToCamel(j.getKey());
            var value = j.getValue();
            if (value instanceof JsonObject) {
                formattedJson.put(key, snakeToCamel((JsonObject) value));
            } else {
                formattedJson.put(key, value);
            }
        });
        return formattedJson;
    }

}
