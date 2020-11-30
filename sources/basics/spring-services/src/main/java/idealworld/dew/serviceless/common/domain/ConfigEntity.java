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

package idealworld.dew.serviceless.common.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * Config entity.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "dew_config", indexes = {
        @Index(columnList = "k", unique = true)
})
@org.hibernate.annotations.Table(appliesTo = "dew_config",
        comment = "通用配置信息")
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfigEntity extends SafeEntity {

    /**
     * The Key.
     */
    @Column(nullable = false,
            columnDefinition = "varchar(255) comment 'Key'")
    protected String k;

    /**
     * The Value.
     */
    @Column(nullable = false,
            columnDefinition = "varchar(3000) comment 'Value'")
    protected String v;

}
