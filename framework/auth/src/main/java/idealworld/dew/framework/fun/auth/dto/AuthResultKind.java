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

package idealworld.dew.framework.fun.auth.dto;

import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.exception.RTException;
import io.vertx.core.json.JsonObject;

import java.util.Arrays;

/**
 * 权限结果类型枚举.
 *
 * @author gudaoxuri
 */
public enum AuthResultKind {

    /**
     * 接受.
     */
    ACCEPT("ACCEPT"),
    /**
     * 拒绝.
     */
    REJECT("REJECT");

    private final String code;

    AuthResultKind(String code) {
        this.code = code;
    }

    /**
     * Parse auth result kind.
     *
     * @param code the code
     * @return the auth result kind
     */
    public static AuthResultKind parse(String code) {
        return Arrays.stream(AuthResultKind.values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new RTException(JsonObject.mapFrom(Resp.badRequest("Auth Result Kind {" + code + "} NOT exist.")).toString()));
    }

    @Override
    public String toString() {
        return code;
    }
}
