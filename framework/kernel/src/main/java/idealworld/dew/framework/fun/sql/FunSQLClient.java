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

package idealworld.dew.framework.fun.sql;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.domain.IdEntity;
import idealworld.dew.framework.domain.SafeEntity;
import idealworld.dew.framework.domain.SoftDelEntity;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.util.CaseFormatter;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
@Slf4j
public class FunSQLClient {

    private static final Map<String, FunSQLClient> SQL_CLIENTS = new HashMap<>();

    private SqlClient client;

    public static Future<Void> init(String code, Vertx vertx, DewConfig.FunConfig.SQLConfig config) {
        var mysqlClient = new FunSQLClient();
        var poolOptions = new PoolOptions()
                .setMaxSize(config.getMaxPoolSize())
                .setMaxWaitQueueSize(config.getMaxPoolWaitQueueSize());
        // TODO 修改成 MySQLConnectOptions 形式
        if (config.getUri() != null && !config.getUri().isBlank()) {
            mysqlClient.client = MySQLPool.pool(vertx, config.getUri().trim(), poolOptions);
        } else {
            var connectOptions = new MySQLConnectOptions()
                    .setPort(config.getPort())
                    .setHost(config.getHost())
                    .setDatabase(config.getDb())
                    .setUser(config.getUserName())
                    .setPassword(config.getPassword())
                    .setCharset(config.getCharset())
                    .setCollation(config.getCollation());
            mysqlClient.client = MySQLPool.pool(vertx, connectOptions, poolOptions);
        }
        SQL_CLIENTS.put(code, mysqlClient);
        return Future.succeededFuture();
    }

    public static CompositeFuture destroy() {
        return CompositeFuture.all(SQL_CLIENTS.values().stream()
                .map(client -> client.client.close())
                .collect(Collectors.toList()));
    }

    public static FunSQLClient choose(String code) {
        return SQL_CLIENTS.get(code);
    }

    public static Boolean contains(String code) {
        return SQL_CLIENTS.containsKey(code);
    }

    public static void remove(String code) {
        SQL_CLIENTS.remove(code);
    }

    public <E> Future<Resp<List<E>>> select(String sql, Map<String, Object> parameters, Class<E> returnClazz) {
        Promise<Resp<List<E>>> promise = Promise.promise();
        SqlTemplate
                .forQuery(client, sql)
                .mapTo(Row::toJson)
                .execute(parameters)
                .onSuccess(records -> promise.complete(
                        Resp.success($.fun.stream(records.iterator())
                                .map(rowJson -> CaseFormatter.snakeToCamel(rowJson).mapTo(returnClazz))
                                .collect(Collectors.toList()))))
                .onFailure(e -> {
                    log.error("[SQL]Select [{}] error: {}", sql, e.getMessage(), e);
                    promise.fail(e);
                });
        return promise.future();
    }

    private FunSQLClient txInstance(SqlConnection client) {
        var txClient = new FunSQLClient();
        txClient.client = client;
        return txClient;
    }

    public Future<Resp<Long>> ddl(String sql) {
        return updateHandler(SqlTemplate
                        .forUpdate(client, sql)
                        .execute(new HashMap<>()),
                sql);
    }

    public <E extends IdEntity> Future<Resp<Long>> insert(E entity, ProcessContext context) {
        var tableName = entity.tableName();
        addSafeInfo(entity, context);
        var caseEntity = CaseFormatter.camelToSnake(JsonObject.mapFrom(entity));
        var columns = caseEntity.stream().map(Map.Entry::getKey).collect(Collectors.joining(", "));
        var values = caseEntity.stream().map(j -> "#{" + j.getKey() + "}").collect(Collectors.joining(", "));
        var sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        return updateHandler(SqlTemplate
                        .forUpdate(client, sql)
                        .mapFrom(TupleMapper.jsonObject())
                        .execute(caseEntity),
                sql);
    }

    public <E extends IdEntity> Future<Resp<Long>> insert(List<E> entities, ProcessContext context) {
        var tableName = entities.get(0).tableName();
        entities.forEach(entity -> addSafeInfo(entity, context));
        var caseEntity = CaseFormatter.camelToSnake(JsonObject.mapFrom(entities.get(0)));
        var columns = caseEntity.stream().map(Map.Entry::getKey).collect(Collectors.joining(", "));
        var values = caseEntity.stream().map(j -> "#{" + j.getKey() + "}").collect(Collectors.joining(", "));
        var sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        return updateHandler(SqlTemplate
                        .forUpdate(client, sql)
                        .mapFrom(TupleMapper.jsonObject())
                        .executeBatch(entities.stream()
                                .map(entity -> CaseFormatter.camelToSnake(JsonObject.mapFrom(entity)))
                                .collect(Collectors.toList())),
                sql);
    }

    public Future<Resp<Long>> update(String sql, Map<String, Object> parameters) {
        return updateHandler(SqlTemplate
                        .forUpdate(client, sql)
                        .execute(parameters),
                sql);
    }

    public Future<Resp<Long>> update(String sql, List<Map<String, Object>> parameters) {
        return updateHandler(SqlTemplate
                        .forUpdate(client, sql)
                        .executeBatch(parameters),
                sql);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Resp<Long>> softDelete(String sql, Map<String, Object> parameters, Class<E> entityClazz, ProcessContext context) {
        Promise<Resp<Long>> promise = Promise.promise();
        var tableName = entityClazz.getDeclaredConstructor().newInstance().tableName();
        select(sql, parameters, Map.class)
                .compose(selectR -> {
                    Promise<List<Map<String, Object>>> insertP = Promise.promise();
                    var deleteObjs = selectR.getBody();
                    var softDelEntities = deleteObjs.stream()
                            .map(deleteObj -> SoftDelEntity.builder()
                                    .entityName(entityClazz.getSimpleName())
                                    .recordId(deleteObj.get("id").toString())
                                    .content(JsonObject.mapFrom(deleteObj).toString())
                                    .build())
                            .collect(Collectors.toList());
                    insert(softDelEntities, context)
                            .onSuccess(insertResult -> {
                                insertP.complete(deleteObjs.stream()
                                        .map(deleteObj -> {
                                            var item = new HashMap<String, Object>();
                                            item.put("id", deleteObj.get("id"));
                                            return item;
                                        })
                                        .collect(Collectors.toList()));
                            })
                            .onFailure(insertP::fail);
                    return insertP.future();
                })
                .compose(idR -> update("DELETE FROM " + tableName + " WHERE id = #{id}", idR))
                .onSuccess(promise::complete)
                .onFailure(promise::fail);
        return promise.future();
    }

    public Future<Resp<Long>> delete(String sql, Map<String, Object> parameters) {
        return update(sql, parameters);
    }

    public <E> Future<E> tx(Function<FunSQLClient, Future<E>> function) {
        Promise<E> promise = Promise.promise();
        ((MySQLPool) client).withTransaction(client -> function.apply(txInstance(client)))
                .onComplete(result -> {
                    if (result.failed()) {
                        log.error("[SQL]Transaction [{}] error: {}", result.cause().getMessage(), result.cause());
                        promise.fail(result.cause());
                    } else {
                        promise.complete(result.result());
                    }
                });
        return promise.future();
    }

    private Future<Resp<Long>> updateHandler(Future<SqlResult<Void>> handler, String sql) {
        Promise<Resp<Long>> promise = Promise.promise();
        handler
                .onSuccess(records -> {
                    if (sql.trim().toLowerCase().startsWith("insert into")) {
                        promise.complete(Resp.success(records.property(MySQLClient.LAST_INSERTED_ID)));
                    } else {
                        promise.complete(Resp.success((long) records.rowCount()));
                    }
                })
                .onFailure(e -> {
                    log.error("[SQL]Update [{}] error: {}", sql, e.getMessage(), e);
                    promise.fail(e);
                });
        return promise.future();
    }

    private <E extends IdEntity> void addSafeInfo(E entity, ProcessContext context) {
        if (context == null) {
            return;
        }
        if (entity instanceof SafeEntity) {
            ((SafeEntity) entity).setCreateUser(
                    context.req.identOptInfo.getAccountCode() != null
                            ? (String) context.req.identOptInfo.getAccountCode() :
                            "");
            ((SafeEntity) entity).setUpdateUser(
                    context.req.identOptInfo.getAccountCode() != null
                            ? (String) context.req.identOptInfo.getAccountCode() :
                            "");
        }
    }

}
