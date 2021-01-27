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

package idealworld.dew.serviceless.task.domain;

import idealworld.dew.framework.domain.IdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 任务实例.
 *
 * @author gudaoxuri
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class TaskInst extends IdEntity {

    @Override
    public String tableName() {
        return "task_" + super.tableName();
    }

    // 执行开始时间
    @NotNull
    private Long startTime;
    // 执行结束时间
    @NotNull
    private Long endTime;
    // 执行结果
    @NotNull
    @NotBlank
    private Boolean success;
    // 执行结果说明
    @NotNull
    @NotBlank
    @Size(max = 1000)
    private String message;
    @NotNull
    @NotBlank
    @Size(max = 255)
    private String relTaskDefCode;

}
