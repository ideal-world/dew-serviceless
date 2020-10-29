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
 * 权限主体运算类型枚举.
 *
 * @author gudaoxuri
 */
public enum AuthSubjectOperatorKind {

    /**
     * 等于.
     */
    EQ("EQ"),
    /**
     * 包含.
     * <p>
     * 可用于群组当前及祖父节点
     */
    INCLUDE("INCLUDE"),
    /**
     * LIKE.
     * <p>
     * 用于群组当前及子孙节点
     */
    LIKE("LIKE");

    private final String code;

    AuthSubjectOperatorKind(String code) {
        this.code = code;
    }

    /**
     * Parse auth subject operator kind.
     *
     * @param code the code
     * @return the auth subject operator kind
     */
    public static AuthSubjectOperatorKind parse(String code) {
        return Arrays.stream(AuthSubjectOperatorKind.values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> StandardResp.e(
                        StandardResp.badRequest("BASIC",
                                "Auth Subject Operator kind {" + code + "} NOT exist.")));
    }

    @Override
    public String toString() {
        return code;
    }
}
