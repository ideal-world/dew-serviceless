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

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Yaml Helper
 *
 * @author gudaoxuri
 */
public class YamlHelper {

    private static Yaml yaml;

    static {
        DumperOptions options = new DumperOptions();
        options.setCanonical(false);
        options.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        options.setIndent(2);
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        yaml = new Yaml(representer, options);
    }

    public static <T> T toObject(String content) {
        return yaml.load(content);
    }

    public static <T> T toObject(Class<T> clazz, String content) {
        return yaml.loadAs(content, clazz);
    }

    public static <T> T toObject(Class<T> clazz, String... contents) {
        String mergedContent = String.join("\r\n", contents);
        return yaml.loadAs(mergedContent, clazz);
    }

    public static String toString(Object content) {
        String str = yaml.dump(content);
        if (str.startsWith("!!")) {
            return str.substring(str.indexOf('\n') + 1);
        }
        return str;
    }

}
