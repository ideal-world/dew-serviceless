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

package idealworld.dew.serviceless.iam.domain.auth;

import idealworld.dew.serviceless.common.domain.SafeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

/**
 * 账号群组.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "iam_account_group", indexes = {
        @Index(columnList = "relAccountId,relGroupNodeId", unique = true),
        @Index(columnList = "relGroupNodeId")
})
@org.hibernate.annotations.Table(appliesTo = "iam_account_group",
        comment = "账号群组关联")
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AccountGroup extends SafeEntity {

     @Column(nullable = false,
            columnDefinition = "bigint comment '关联群组节点Id'")
    private Long relGroupNodeId;

    @Column(nullable = false,
            columnDefinition = "bigint comment '关联账号Id'")
    private Long relAccountId;


}
