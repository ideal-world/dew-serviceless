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
import idealworld.dew.baas.common.resp.StandardResp;
import idealworld.dew.baas.common.service.domain.BasicSoftDelEntity;
import idealworld.dew.baas.common.service.domain.PkEntity;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

/**
 * The type Common service.
 *
 * @param <D> the type parameter
 * @param <P> the type parameter
 * @author gudaoxuri
 */
@Slf4j
public abstract class CommonService<D extends BasicSoftDelEntity, P extends Serializable> {

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
     * Save entity.
     *
     * @param pkEntity the pk entity
     * @return the resp
     */
    protected Resp<P> saveEntity(PkEntity<P> pkEntity) {
        entityManager.persist(pkEntity);
        return StandardResp.success(pkEntity.getId());
    }

    /**
     * Update entity.
     *
     * @param pkEntity the pk entity
     * @return the resp
     */
    protected Resp<P> updateEntity(PkEntity<P> pkEntity) {
        entityManager.merge(pkEntity);
        return StandardResp.success(pkEntity.getId());
    }

    /**
     * Update entity.
     *
     * @param updateClause the update clause
     * @return the resp
     */
    protected Resp<Void> updateEntity(JPAUpdateClause updateClause) {
        var modifyRowNum = updateClause.execute();
        if (modifyRowNum == 0) {
            log.warn("没有需要更新的记录 {}", updateClause.toString());
            return StandardResp.notFound("BASIC", "没有需要更新的记录");
        }
        return StandardResp.success(null);
    }

    /**
     * Update entities.
     *
     * @param updateClause the update clause
     * @return the resp
     */
    protected Resp<Long> updateEntities(JPAUpdateClause updateClause) {
        return StandardResp.success(updateClause.execute());
    }

    /**
     * Delete entity.
     *
     * @param deleteClause the delete clause
     * @return the resp
     */
    protected Resp<Void> deleteEntity(JPADeleteClause deleteClause) {
        log.info("Delete entity , cond : {}", deleteClause.toString());
        var modifyRowNum = deleteClause.execute();
        if (modifyRowNum == 0) {
            log.warn("没有需要删除的记录 {}", deleteClause.toString());
            return StandardResp.notFound("BASIC", "没有需要删除的记录");
        }
        return StandardResp.success(null);
    }

    /**
     * Delete entities.
     *
     * @param deleteClause the delete clause
     * @return the resp
     */
    protected Resp<Long> deleteEntities(JPADeleteClause deleteClause) {
        log.info("Delete entities , cond : {}", deleteClause.toString());
        return StandardResp.success(deleteClause.execute());
    }

    /**
     * Soft del entity.
     *
     * @param <E>      the type parameter
     * @param jpaQuery the jpa query
     * @return the resp
     */
    protected <E extends PkEntity<P>> Resp<Void> softDelEntity(JPAQuery<E> jpaQuery) {
        var entity = jpaQuery.fetchOne();
        if (entity == null) {
            log.warn("没有需要软删的记录 {}", jpaQuery.toString());
            return StandardResp.notFound("BASIC", "没有需要软删的记录");
        }
        log.info("Soft Delete entity {} , cond : {}", jpaQuery.getType().getSimpleName(), jpaQuery.toString());
        BasicSoftDelEntity basicSoftDelEntity = softDelPackage(entity);
        basicSoftDelEntity.setKind(softDelGetKind());
        basicSoftDelEntity.setEntityName(jpaQuery.getType().getSimpleName());
        basicSoftDelEntity.setRecordId(entity.getId() + "");
        basicSoftDelEntity.setContent($.json.toJsonString(entity));
        entityManager.persist(basicSoftDelEntity);
        entityManager.remove(entity);
        return StandardResp.success(null);
    }

    /**
     * Soft del entities.
     *
     * @param <E>      the type parameter
     * @param jpaQuery the jpa query
     * @return the resp
     */
    protected <E extends PkEntity<P>> Resp<Long> softDelEntities(JPAQuery<E> jpaQuery) {
        log.info("Soft Delete entities {} , cond : {}", jpaQuery.getType().getSimpleName(), jpaQuery.toString());
        var deleteCounts = jpaQuery.fetch()
                .stream()
                .map(entity -> {
                    BasicSoftDelEntity basicSoftDelEntity = softDelPackage(entity);
                    basicSoftDelEntity.setKind(softDelGetKind());
                    basicSoftDelEntity.setEntityName(jpaQuery.getType().getSimpleName());
                    basicSoftDelEntity.setRecordId(entity.getId() + "");
                    basicSoftDelEntity.setContent($.json.toJsonString(entity));
                    entityManager.persist(basicSoftDelEntity);
                    entityManager.remove(entity);
                    return entity.getId();
                })
                .count();
        return StandardResp.success(deleteCounts);
    }

    /**
     * Soft del get kind string.
     *
     * @return the string
     */
    protected abstract String softDelGetKind();

    /**
     * Soft del package del.
     *
     * @param <E>          the type parameter
     * @param deleteEntity the delete entity
     * @return the del
     */
    protected abstract <E extends PkEntity<P>> D softDelPackage(E deleteEntity);

    /**
     * Gets dto.
     *
     * @param <E>      the type parameter
     * @param jpaQuery the jpa query
     * @return the dto
     */
    protected <E> Resp<E> getDTO(JPAQuery<E> jpaQuery) {
        var obj = jpaQuery.fetchOne();
        if (obj == null) {
            log.warn("没有获取到记录 {}", jpaQuery.toString());
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
    protected <E> Resp<List<E>> findDTOs(JPAQuery<E> jpaQuery) {
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
    protected <E> Resp<Page<E>> pageDTOs(JPAQuery<E> jpaQuery, Long pageNumber, Integer pageSize) {
        if (jpaQuery.getMetadata().getGroupBy().size() > 1) {
            // Fixed https://github.com/querydsl/querydsl/pull/2605/files
            // Get JPQLSerializer
            var serializer = (JPQLSerializer) $.bean.invoke(jpaQuery,
                    JPAQueryBase.class.getDeclaredMethod("serialize", boolean.class), false);
            String hql = serializer.toString();
            // HQL to Native SQL
            var translatorFactory = new ASTQueryTranslatorFactory();
            var hibernateSession = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
            var translator = translatorFactory
                    .createQueryTranslator("", hql, Collections.EMPTY_MAP, (SessionFactoryImplementor) hibernateSession, null);
            translator.compile(Collections.EMPTY_MAP, false);
            var sql = translator.getSQLString();
            // Package COUNT
            var countSql = "select count(1) from (" + sql + ") _tmp_" + System.currentTimeMillis();
            var nativeQuery = entityManager.createNativeQuery(countSql);
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
    protected <E> Resp<Long> countQuery(JPAQuery<E> jpaQuery) {
        return StandardResp.success(jpaQuery.fetchCount());
    }

    /**
     * Exist query.
     *
     * @param <E>      the type parameter
     * @param jpaQuery the jpa query
     * @return the resp
     */
    protected <E> Resp<Boolean> existQuery(JPAQuery<E> jpaQuery) {
        return StandardResp.success(jpaQuery.fetchCount() != 0);
    }

}
