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

package idealworld.dew.baas.common.service;

import com.ecfront.dew.common.Resp;
import com.querydsl.jpa.impl.JPAQueryFactory;
import idealworld.dew.baas.common.domain.ConfigEntity;
import idealworld.dew.baas.common.domain.QConfigEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Config service.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class ConfigService {

    /**
     * The Sql builder.
     */
    @Autowired
    protected JPAQueryFactory sqlBuilder;
    /**
     * The Entity manager.
     */
    @Autowired
    protected EntityManager entityManager;

    /**
     * Get resp.
     *
     * @param k the k
     * @return the resp
     */
    public Resp<String> get(String k) {
        var qConfigEntity = QConfigEntity.configEntity;
        return Resp.success(sqlBuilder.select(qConfigEntity.v)
                .from(qConfigEntity)
                .where(qConfigEntity.k.eq(k))
                .fetchOne());
    }

    /**
     * Find all.
     *
     * @return the resp
     */
    public Resp<Map<String, String>> findAll() {
        var qConfigEntity = QConfigEntity.configEntity;
        return Resp.success(sqlBuilder.selectFrom(qConfigEntity)
                .fetch()
                .stream()
                .collect(Collectors.toMap(ConfigEntity::getK, ConfigEntity::getV)));
    }

    /**
     * Set.
     *
     * @param k the k
     * @param v the v
     * @return the resp
     */
    public Resp<Void> set(String k, String v) {
        var qConfigEntity = QConfigEntity.configEntity;
        if (sqlBuilder.select(qConfigEntity.id)
                .from(qConfigEntity)
                .where(qConfigEntity.k.eq(k))
                .fetchCount() == 0) {
            entityManager.persist(ConfigEntity.builder()
                    .k(k)
                    .v(v)
                    .build());
        } else {
            sqlBuilder.update(qConfigEntity)
                    .set(qConfigEntity.v, v)
                    .where(qConfigEntity.k.eq(k))
                    .execute();
        }
        return Resp.success(null);
    }

    /**
     * Delete.
     *
     * @param k the k
     * @return the resp
     */
    public Resp<Void> delete(String k) {
        var qConfigEntity = QConfigEntity.configEntity;
        sqlBuilder.delete(qConfigEntity)
                .where(qConfigEntity.k.eq(k))
                .execute();
        return Resp.success(null);
    }

}
