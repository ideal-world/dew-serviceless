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

package idealworld.dew.serviceless.iam.dto;

import idealworld.dew.framework.exception.BadRequestException;

import java.util.Arrays;

/**
 * 群组类型枚举.
 *
 * @author gudaoxuri
 */
public enum GroupKind {

    /**
     * 行政.
     */
    ADMINISTRATION("ADMINISTRATION"),
    /**
     * 虚拟.
     */
    VIRTUAL("VIRTUAL");

    private final String code;

    GroupKind(String code) {
        this.code = code;
    }

    /**
     * Parse group kind.
     *
     * @param code the code
     * @return the group kind
     */
    public static GroupKind parse(String code) {
        return Arrays.stream(GroupKind.values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Group kind {" + code + "} NOT exist."));
    }

    @Override
    public String toString() {
        return code;
    }
}
