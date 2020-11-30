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

import group.idealworld.dew.Dew;
import idealworld.dew.serviceless.common.Constant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.util.StringUtils;

import javax.persistence.*;
import java.util.Date;

/**
 * Basic soft del entity.
 *
 * @author gudaoxuri
 */
@Entity
@Table(name = "dew_del_record", indexes = {
        @Index(columnList = "entityName,recordId",unique = true)
})
@org.hibernate.annotations.Table(appliesTo = "dew_del_record",
        comment = "记录删除信息")
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SoftDelEntity extends IdEntity {

    /**
     * The Entity name.
     */
    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '对象名称'")
    protected String entityName;

    /**
     * The Record id.
     */
    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '记录Id'")
    protected String recordId;

    /**
     * The Content.
     */
    @Lob
    @Column(nullable = false,
            columnDefinition = "text comment '删除内容，Json格式'")
    protected String content;

    /**
     * The Create time.
     */
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(columnDefinition = "timestamp default CURRENT_TIMESTAMP comment '删除时间'")
    protected Date deleteTime;

    /**
     * The Create user.
     */
    @Column(nullable = false,
            columnDefinition = "varchar(255) comment '删除者OpenId'")
    protected String deleteUser;

    /**
     * Add user.
     */
    @PrePersist
    public void addUser() {
        Dew.auth.getOptInfo().ifPresent(optInfo -> {
            if (StringUtils.isEmpty(this.getDeleteUser())) {
                this.setDeleteUser((String) optInfo.getAccountCode());
            }
        });
        if (StringUtils.isEmpty(this.getDeleteUser())) {
            this.setDeleteUser(Constant.OBJECT_UNDEFINED + "");
        }
    }

}
