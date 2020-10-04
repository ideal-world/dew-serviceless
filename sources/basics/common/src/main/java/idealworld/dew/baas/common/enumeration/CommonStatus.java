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

package idealworld.dew.baas.common.enumeration;

import idealworld.dew.baas.common.resp.StandardResp;

import java.util.Arrays;

/**
 * 公共状态枚举.
 *
 * @author gudaoxuri
 */
public enum CommonStatus {

    /**
     * 禁用.
     */
    DISABLED("DISABLED"),
    /**
     * 启用.
     */
    ENABLED("ENABLED");

    private final String code;

    CommonStatus(String code) {
        this.code = code;
    }

    /**
     * Parse common status.
     *
     * @param code the code
     * @return the common status
     */
    public static CommonStatus parse(String code) {
        return Arrays.stream(CommonStatus.values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> StandardResp.e(
                        StandardResp.badRequest("BASIC",
                                "Common status {" + code + "} NOT exist.")));
    }

    @Override
    public String toString() {
        return code;
    }
}
