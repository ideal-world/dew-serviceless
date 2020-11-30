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

package idealworld.dew.serviceless.iam.enumeration;

import idealworld.dew.serviceless.common.resp.StandardResp;

import java.util.Arrays;

/**
 * 开放级别枚举.
 *
 * @author gudaoxuri
 */
public enum ExposeKind {

    /**
     * 应用级.
     */
    APP("APP"),
    /**
     * 租户级.
     */
    TENANT("TENANT"),
    /**
     * 系统级.
     */
    GLOBAL("GLOBAL");

    private final String code;

    ExposeKind(String code) {
        this.code = code;
    }

    /**
     * Parse account ident kind.
     *
     * @param code the code
     * @return the account ident kind
     */
    public static ExposeKind parse(String code) {
        return Arrays.stream(ExposeKind.values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> StandardResp.e(
                        StandardResp.badRequest("BASIC",
                                "Expose kind {" + code + "} NOT exist.")));
    }

    @Override
    public String toString() {
        return code;
    }
}
