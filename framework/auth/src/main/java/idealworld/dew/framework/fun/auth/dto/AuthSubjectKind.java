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

package idealworld.dew.framework.fun.auth.dto;

import idealworld.dew.framework.exception.BadRequestException;

import java.util.Arrays;

/**
 * 权限主体类型枚举.
 *
 * @author gudaoxuri
 */
public enum AuthSubjectKind {

    /**
     * 租户.
     */
    TENANT("TENANT"),
    /**
     * 应用.
     */
    APP("APP"),
    /**
     * 角色.
     */
    ROLE("ROLE"),
    /**
     * 群组节点.
     */
    GROUP_NODE("GROUP_NODE"),
    /**
     * 账户.
     */
    ACCOUNT("ACCOUNT");

    private final String code;

    AuthSubjectKind(String code) {
        this.code = code;
    }

    /**
     * Parse auth subject kind.
     *
     * @param code the code
     * @return the auth subject kind
     */
    public static AuthSubjectKind parse(String code) {
        return Arrays.stream(AuthSubjectKind.values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Auth Subject kind {" + code + "} NOT exist."));
    }

    @Override
    public String toString() {
        return code;
    }
}
