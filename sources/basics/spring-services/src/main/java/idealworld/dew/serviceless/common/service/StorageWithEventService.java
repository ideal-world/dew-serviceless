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

package idealworld.dew.serviceless.common.service;

import com.ecfront.dew.common.Resp;
import com.querydsl.jpa.impl.JPADeleteClause;
import com.querydsl.jpa.impl.JPAUpdateClause;
import idealworld.dew.serviceless.common.domain.PkEntity;

import java.io.Serializable;

public interface StorageWithEventService<P extends Serializable> extends StorageService<P>, EventService {

    boolean notifyByMQ(String entityName);

    @Override
    default <E extends PkEntity<P>> Resp<Void> preSaveEntity(E entity) {
        return checkEventByJVM(ActionKind.CREATE, entity.getClass().getName(), null);
    }

    @Override
    default <E extends PkEntity<P>> void postSaveEntity(E entity) {
        notifyEventByJVM(ActionKind.CREATE, entity.getClass().getName(), entity.getId());
        if (notifyByMQ(entity.getClass().getName())) {
            notifyEventByMQ(ActionKind.CREATE, entity.getClass().getName(), entity.getId());
        }
    }

    @Override
    default <E extends PkEntity<P>> Resp<Void> preUpdateEntity(E entity) {
        return checkEventByJVM(ActionKind.MODIFY, entity.getClass().getName(), entity.getId());
    }

    @Override
    default <E extends PkEntity<P>> void postUpdateEntity(E entity) {
        notifyEventByJVM(ActionKind.MODIFY, entity.getClass().getName(), entity.getId());
        if (notifyByMQ(entity.getClass().getName())) {
            notifyEventByMQ(ActionKind.MODIFY, entity.getClass().getName(), entity.getId());
        }
    }

    @Override
    default Resp<Void> preUpdateEntity(JPAUpdateClause updateClause) {
        // TODO
        return Resp.success(null);
    }

    @Override
    default void postUpdateEntity(JPAUpdateClause updateClause) {

    }

    @Override
    default Resp<Void> preUpdateEntities(JPAUpdateClause updateClause) {
        // TODO
        return Resp.success(null);
    }

    @Override
    default void postUpdateEntities(JPAUpdateClause updateClause) {

    }

    @Override
    default Resp<Void> preDeleteEntity(JPADeleteClause deleteClause) {
        // TODO
        return Resp.success(null);
    }

    @Override
    default void postDeleteEntity(JPADeleteClause deleteClause) {

    }

    @Override
    default Resp<Void> preDeleteEntities(JPADeleteClause deleteClause) {
        // TODO
        return Resp.success(null);
    }

    @Override
    default void postDeleteEntities(JPADeleteClause deleteClause) {

    }

    @Override
    default <E extends PkEntity<P>> Resp<Void> preSoftDeleteEntity(E entity) {
        return checkEventByJVM(ActionKind.DELETE, entity.getClass().getName(), entity.getId());
    }

    @Override
    default <E extends PkEntity<P>> void postSoftDeleteEntity(E entity) {
        notifyEventByJVM(ActionKind.DELETE, entity.getClass().getName(), entity.getId());
        if (notifyByMQ(entity.getClass().getName())) {
            notifyEventByMQ(ActionKind.DELETE, entity.getClass().getName(), entity.getId());
        }
    }
}
