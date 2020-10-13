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

package idealworld.dew.baas.iam.utils;

import com.ecfront.dew.common.$;

/**
 * Key helper.
 *
 * @author gudaoxuri
 */
public class KeyHelper {

    /**
     * Generate token string.
     *
     * @return the string
     */
    public static String generateToken() {
        return $.security.digest.digest(
                $.field.createUUID().replaceAll("\\-", ""),
                "MD5"
        );
    }

    /**
     * Generate ak string.
     *
     * @return the string
     */
    public static String generateAK() {
        return $.field.createUUID().replaceAll("\\-", "");
    }

    /**
     * Generate sk string.
     *
     * @param key the key
     * @return the string
     */
    public static String generateSK(String key) {
        return $.security.digest.digest(
                key + $.field.createUUID().replaceAll("\\-", ""),
                "SHA1"
        );
    }

}
