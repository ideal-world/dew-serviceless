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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import com.querydsl.jpa.JPAQueryBase;
import com.querydsl.jpa.JPQLSerializer;
import com.querydsl.jpa.impl.*;
import idealworld.dew.baas.common.domain.PkEntity;
import idealworld.dew.baas.common.domain.SoftDelEntity;
import idealworld.dew.baas.common.resp.StandardResp;
import lombok.SneakyThrows;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.slf4j.Logger;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

public interface StorageService<P extends Serializable> {

    JPAQueryFactory sqlBuilder();

    EntityManager entityManager();

    Logger log();

    default <E extends PkEntity<P>> Resp<Void> preSaveEntity(E entity) {
        return Resp.success(null);
    }

    default <E extends PkEntity<P>> void postSaveEntity(E entity) {
    }

    default Resp<P> saveEntity(PkEntity<P> pkEntity) {
        var preSaveEntityR = preSaveEntity(pkEntity);
        if (!preSaveEntityR.ok()) {
            return Resp.error(preSaveEntityR);
        }
        entityManager().persist(pkEntity);
        postSaveEntity(pkEntity);
        return StandardResp.success(pkEntity.getId());
    }

    default <E extends PkEntity<P>> Resp<Void> preUpdateEntity(E entity) {
        return Resp.success(null);
    }

    default <E extends PkEntity<P>> void postUpdateEntity(E entity) {
    }

    default Resp<Void> updateEntity(PkEntity<P> pkEntity) {
        var preUpdateEntityR = preUpdateEntity(pkEntity);
        if (!preUpdateEntityR.ok()) {
            return Resp.error(preUpdateEntityR);
        }
        entityManager().merge(pkEntity);
        postUpdateEntity(pkEntity);
        return StandardResp.success(null);
    }

    default Resp<Void> preUpdateEntity(JPAUpdateClause updateClause) {
        return Resp.success(null);
    }

    default void postUpdateEntity(JPAUpdateClause updateClause) {
    }

    default Resp<Void> updateEntity(JPAUpdateClause updateClause) {
        var preUpdateEntityR = preUpdateEntity(updateClause);
        if (!preUpdateEntityR.ok()) {
            return Resp.error(preUpdateEntityR);
        }
        var modifyRowNum = updateClause.execute();
        if (modifyRowNum == 0) {
            log().warn("没有需要更新的记录 {}", updateClause.toString());
            return StandardResp.notFound("BASIC", "没有需要更新的记录");
        }
        postUpdateEntity(updateClause);
        return StandardResp.success(null);
    }

    default Resp<Void> preUpdateEntities(JPAUpdateClause updateClause) {
        return Resp.success(null);
    }

    default void postUpdateEntities(JPAUpdateClause updateClause) {
    }

    default Resp<Long> updateEntities(JPAUpdateClause updateClause) {
        var preUpdateEntitiesR = preUpdateEntities(updateClause);
        if (!preUpdateEntitiesR.ok()) {
            return Resp.error(preUpdateEntitiesR);
        }
        var updatedNum = updateClause.execute();
        postUpdateEntities(updateClause);
        return StandardResp.success(updatedNum);
    }

    default Resp<Void> preDeleteEntity(JPADeleteClause deleteClause) {
        return Resp.success(null);
    }

    default void postDeleteEntity(JPADeleteClause deleteClause) {
    }

    default Resp<Void> deleteEntity(JPADeleteClause deleteClause) {
        log().info("Delete entity , cond : {}", deleteClause.toString());
        var preDeleteEntityR = preDeleteEntity(deleteClause);
        if (!preDeleteEntityR.ok()) {
            return Resp.error(preDeleteEntityR);
        }
        var modifyRowNum = deleteClause.execute();
        if (modifyRowNum == 0) {
            log().warn("没有需要删除的记录 {}", deleteClause.toString());
            return StandardResp.notFound("BASIC", "没有需要删除的记录");
        }
        postDeleteEntity(deleteClause);
        return StandardResp.success(null);
    }

    default Resp<Void> preDeleteEntities(JPADeleteClause deleteClause) {
        return Resp.success(null);
    }

    default void postDeleteEntities(JPADeleteClause deleteClause) {
    }

    default Resp<Long> deleteEntities(JPADeleteClause deleteClause) {
        log().info("Delete entities , cond : {}", deleteClause.toString());
        var preDeleteEntitiesR = preDeleteEntities(deleteClause);
        if (!preDeleteEntitiesR.ok()) {
            return Resp.error(preDeleteEntitiesR);
        }
        var deletedRum = deleteClause.execute();
        postDeleteEntities(deleteClause);
        return StandardResp.success(deletedRum);
    }

    default <E extends PkEntity<P>> Resp<Void> preSoftDeleteEntity(E entity) {
        return Resp.success(null);
    }

    default <E extends PkEntity<P>> void postSoftDeleteEntity(E entity) {
    }

    default <E extends PkEntity<P>> Resp<Void> softDelEntity(JPAQuery<E> jpaQuery) {
        var entity = jpaQuery.fetchOne();
        if (entity == null) {
            log().warn("没有需要软删的记录 {}", jpaQuery.toString());
            return StandardResp.notFound("BASIC", "没有需要软删的记录");
        }
        return softDelEntity(entity);
    }

    default <E extends PkEntity<P>> Resp<Void> softDelEntity(E entity) {
        var preSoftDeleteEntityR = preSoftDeleteEntity(entity);
        if (!preSoftDeleteEntityR.ok()) {
            return Resp.error(preSoftDeleteEntityR);
        }
        log().info("Soft Delete entity {} , cond : {}", entity.getClass().getSimpleName(), entity.getId());
        var softDelEntity = SoftDelEntity.builder()
                .entityName(entity.getClass().getSimpleName())
                .recordId(entity.getId() + "")
                .content($.json.toJsonString(entity))
                .build();
        entityManager().persist(softDelEntity);
        entityManager().remove(entity);
        postSoftDeleteEntity(entity);
        return StandardResp.success(null);
    }

    default <E extends PkEntity<P>> Resp<Long> softDelEntities(JPAQuery<E> jpaQuery) {
        var deleteEntities = jpaQuery.fetch();
        for (E entity : deleteEntities) {
            var preSoftDeleteEntityR = preSoftDeleteEntity(entity);
            if (!preSoftDeleteEntityR.ok()) {
                return Resp.error(preSoftDeleteEntityR);
            }
        }
        log().info("Soft Delete entities {} , cond : {}", jpaQuery.getType().getSimpleName(), jpaQuery.toString());
        var deleteCounts = deleteEntities
                .stream()
                .map(entity -> {
                    var softDelEntity = SoftDelEntity.builder()
                            .entityName(jpaQuery.getType().getSimpleName())
                            .recordId(entity.getId() + "")
                            .content($.json.toJsonString(entity))
                            .build();
                    entityManager().persist(softDelEntity);
                    entityManager().remove(entity);
                    postSoftDeleteEntity(entity);
                    return entity.getId();
                })
                .count();
        return StandardResp.success(deleteCounts);
    }

    /**
     * Gets dto.
     *
     * @param <E>      the type parameter
     * @param jpaQuery the jpa query
     * @return the dto
     */
    default <E> Resp<E> getDTO(JPAQuery<E> jpaQuery) {
        var obj = jpaQuery.fetchOne();
        if (obj == null) {
            log().warn("没有获取到记录 {}", jpaQuery.toString());
            return StandardResp.notFound("BASIC", "没有获取到记录");
        }
        return StandardResp.success(obj);
    }

    /**
     * Find dt os.
     *
     * @param <E>      the type parameter
     * @param jpaQuery the jpa query
     * @return the resp
     */
    default <E> Resp<List<E>> findDTOs(JPAQuery<E> jpaQuery) {
        var obj = jpaQuery.fetch();
        return StandardResp.success(obj);
    }

    /**
     * Page dt os.
     *
     * @param <E>        the type parameter
     * @param jpaQuery   the jpa query
     * @param pageNumber the page number
     * @param pageSize   the page size
     * @return the resp
     */
    @SneakyThrows
    default <E> Resp<Page<E>> pageDTOs(JPAQuery<E> jpaQuery, Long pageNumber, Integer pageSize) {
        if (jpaQuery.getMetadata().getGroupBy().size() > 1) {
            // Fixed https://github.com/querydsl/querydsl/pull/2605/files
            // Get JPQLSerializer
            var serializer = (JPQLSerializer) $.bean.invoke(jpaQuery,
                    JPAQueryBase.class.getDeclaredMethod("serialize", boolean.class), false);
            String hql = serializer.toString();
            // HQL to Native SQL
            var translatorFactory = new ASTQueryTranslatorFactory();
            var hibernateSession = entityManager().getEntityManagerFactory().unwrap(SessionFactory.class);
            var translator = translatorFactory
                    .createQueryTranslator("", hql, Collections.EMPTY_MAP, (SessionFactoryImplementor) hibernateSession, null);
            translator.compile(Collections.EMPTY_MAP, false);
            var sql = translator.getSQLString();
            // Package COUNT
            var countSql = "select count(1) from (" + sql + ") _tmp_" + System.currentTimeMillis();
            var nativeQuery = entityManager().createNativeQuery(countSql);
            // Add sql parameters
            JPAUtil.setConstants(nativeQuery, serializer.getConstantToAllLabels(), jpaQuery.getMetadata().getParams());
            // Fetch total number
            var totalNumber = ((BigInteger) (nativeQuery.getSingleResult())).longValue();
            // Fetch paginated records
            var objs = jpaQuery
                    .limit(pageSize)
                    .offset(pageNumber == 1 ? 0 : (pageNumber - 1) * pageSize)
                    .fetch();
            return StandardResp.success(Page.build(pageNumber, pageSize, totalNumber, objs));
        } else {
            var objs = jpaQuery
                    .limit(pageSize)
                    .offset(pageNumber == 1 ? 0 : (pageNumber - 1) * pageSize)
                    .fetchResults();
            return StandardResp.success(Page.build(pageNumber, pageSize, objs.getTotal(), objs.getResults()));
        }
    }

    /**
     * Count query.
     *
     * @param <E>      the type parameter
     * @param jpaQuery the jpa query
     * @return the resp
     */
    default <E> Resp<Long> countQuery(JPAQuery<E> jpaQuery) {
        return StandardResp.success(jpaQuery.fetchCount());
    }

    /**
     * Exist query.
     *
     * @param <E>      the type parameter
     * @param jpaQuery the jpa query
     * @return the resp
     */
    default <E> Resp<Boolean> existQuery(JPAQuery<E> jpaQuery) {
        return StandardResp.success(jpaQuery.fetchCount() != 0);
    }

}
