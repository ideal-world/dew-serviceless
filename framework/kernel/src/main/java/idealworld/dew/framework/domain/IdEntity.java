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

package idealworld.dew.framework.domain;

import idealworld.dew.framework.util.CaseFormatter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Id entity.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class IdEntity implements Serializable {

    /**
     * The Id.
     */
    @NotNull
    protected Long id;

    public String tableName() {
        var clazzName = this.getClass().getSimpleName();
        clazzName = clazzName.substring(0, 1).toLowerCase() + clazzName.substring(1);
        return CaseFormatter.camelToSnake(clazzName);
    }

}
