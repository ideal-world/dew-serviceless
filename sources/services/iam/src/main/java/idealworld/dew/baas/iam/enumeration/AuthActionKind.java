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

package idealworld.dew.baas.iam.enumeration;

import idealworld.dew.baas.common.resp.StandardResp;

import java.util.Arrays;

/**
 * 权限操作类型枚举.
 *
 * @author gudaoxuri
 */
public enum AuthActionKind {

    /**
     * 获取.
     */
    FETCH("FETCH"),
    /**
     * 创建.
     */
    CREATE("CREATE"),
    /**
     * 全量更新.
     */
    UPDATE("UPDATE"),
    /**
     * 局部更新.
     */
    PATCH("PATCH"),
    /**
     * 删除.
     */
    DELETE("DELETE");

    private final String code;

    AuthActionKind(String code) {
        this.code = code;
    }

    /**
     * Parse resource kind.
     *
     * @param code the code
     * @return the resource kind
     */
    public static AuthActionKind parse(String code) {
        return Arrays.stream(AuthActionKind.values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> StandardResp.e(
                        StandardResp.badRequest("BASIC",
                                "Resource kind {" + code + "} NOT exist.")));
    }

    @Override
    public String toString() {
        return code;
    }
}
