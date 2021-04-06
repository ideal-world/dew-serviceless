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

package idealworld.dew.framework.fun.sql;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.domain.IdEntity;
import idealworld.dew.framework.domain.SoftDelEntity;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.util.CaseFormatter;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mysqlclient.MySQLClient;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.templates.SqlTemplate;
import io.vertx.sqlclient.templates.TupleMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * SQL操作入口.
 *
 * @author gudaoxuri
 */
@Slf4j
public class FunSQLClient {

    private static final Map<String, FunSQLClient> SQL_CLIENTS = new ConcurrentHashMap<>();
    public Consumer addEntityByInsertFun;
    public Consumer addEntityByUpdateFun;
    private String code;
    private SqlClient client;

    public static Future<Void> init(String code, Vertx vertx, DewConfig.FunConfig.SQLConfig config) {
        var mysqlClient = new FunSQLClient();
        mysqlClient.code = code;
        var poolOptions = new PoolOptions()
                .setMaxSize(config.getMaxPoolSize())
                .setMaxWaitQueueSize(config.getMaxPoolWaitQueueSize());
        MySQLConnectOptions connectOptions;
        if (config.getUri() != null && !config.getUri().isBlank()) {
            connectOptions = MySQLConnectOptions.fromUri(config.getUri());
        } else {
            connectOptions = new MySQLConnectOptions()
                    .setHost(config.getHost())
                    .setPort(config.getPort())
                    .setDatabase(config.getDb());
        }
        if (config.getUserName() != null) {
            connectOptions.setUser(config.getUserName());
        }
        if (config.getPassword() != null) {
            connectOptions.setPassword(config.getPassword());
        }
        if (config.getCharset() != null) {
            connectOptions.setCharset(config.getCharset());
        }
        if (config.getCollation() != null) {
            connectOptions.setCollation(config.getCollation());
        }
        mysqlClient.client = MySQLPool.pool(vertx, connectOptions, poolOptions);
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

    public Future<Long> ddl(String sql) {
        return updateHandler(SqlTemplate.forUpdate(client, sql).execute(new HashMap<>()), sql);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<List<E>> list(Map<String, Object> whereParameters, Class<E> returnClazz) {
        var sql = packageWhere("SELECT * FROM " + returnClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return list(sql, whereParameters, returnClazz);
    }

    public Future<List<JsonObject>> list(String sql, Map<String, Object> parameters) {
        return list(sql, parameters, JsonObject.class);
    }

    public <E> Future<List<E>> list(String sql, Map<String, Object> parameters, Class<E> returnClazz) {
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]List\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    parameters.entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        Promise<List<E>> promise = Promise.promise();
        var executeF = SqlTemplate
                .forQuery(client, sql)
                .mapTo(Row::toJson)
                .execute(parameters);
        executeF.onSuccess(records -> {
            try {
                if (!records.iterator().hasNext()) {
                    promise.complete(new ArrayList<>());
                } else {
                    var result = $.fun.stream(records.iterator())
                            .map(rowJson -> returnClazz == JsonObject.class
                                    ? (E) rowJson
                                    : CaseFormatter.snakeToCamel(rowJson).mapTo(returnClazz))
                            .collect(Collectors.toList());
                    promise.complete(result);
                }
            } catch (Exception e) {
                log.error("[SQL][{}]Select [{}] convert error: {}", code, sql, e.getMessage(), e);
                promise.fail(e);
            }
        });
        executeF.onFailure(e -> {
            log.error("[SQL][{}]Select [{}] error: {}", code, sql, e.getMessage(), e);
            promise.fail(e);
        });
        return promise.future();
    }

    @SneakyThrows
    public <E extends IdEntity> Future<E> getOne(Object id, Class<E> returnClazz) {
        return getOne(new HashMap<>() {
            {
                put("id", id);
            }
        }, returnClazz);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<E> getOne(Map<String, Object> whereParameters, Class<E> returnClazz) {
        var sql = packageWhere("SELECT * FROM " + returnClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return getOne(sql, whereParameters, returnClazz);
    }

    public Future<JsonObject> getOne(String sql, Map<String, Object> parameters) {
        return getOne(sql, parameters, JsonObject.class);
    }

    public <E> Future<E> getOne(String sql, Map<String, Object> parameters, Class<E> returnClazz) {
        return list(sql, parameters, returnClazz)
                .compose(resp -> {
                    if (resp.size() > 1) {
                        return Future.failedFuture(new BadRequestException("Query to multiple records."));
                    } else if (resp.isEmpty()) {
                        return Future.succeededFuture(null);
                    }
                    return Future.succeededFuture(resp.get(0));
                });
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Boolean> exists(Map<String, Object> whereParameters, Class<E> entityClazz) {
        var sql = packageWhere("SELECT 1 FROM " + entityClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return count(sql, whereParameters)
                .compose(resp -> Future.succeededFuture(resp > 0));
    }

    public Future<Boolean> exists(String sql, Map<String, Object> parameters) {
        return count(sql, parameters)
                .compose(resp -> Future.succeededFuture(resp > 0));
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Long> count(Map<String, Object> whereParameters, Class<E> entityClazz) {
        var sql = packageWhere("SELECT 1 FROM " + entityClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return count(sql, whereParameters);
    }

    public Future<Long> count(String sql, Map<String, Object> parameters) {
        var countSql = "SELECT COUNT(1) AS _count FROM (" + sql + ") AS _" + System.currentTimeMillis();
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Count\r\n--------------\r\n{}\r\n{}",
                    code, countSql,
                    parameters.entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        Promise<Long> promise = Promise.promise();
        SqlTemplate
                .forQuery(client, countSql)
                .mapTo(Row::toJson)
                .execute(parameters)
                .onSuccess(records -> promise.complete(records.iterator().next().getLong("_count")))
                .onFailure(e -> {
                    log.error("[SQL][{}]Count [{}] error: {}", code, countSql, e.getMessage(), e);
                    promise.fail(e);
                });
        return promise.future();
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Page<E>> page(Map<String, Object> whereParameters, Long pageNumber, Long pageSize, Class<E> returnClazz) {
        var sql = packageWhere("SELECT * FROM " + returnClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return page(sql, whereParameters, pageNumber, pageSize, returnClazz);
    }

    public Future<Page<JsonObject>> page(String sql, Map<String, Object> parameters, Long pageNumber, Long pageSize) {
        return page(sql, parameters, pageNumber, pageSize, JsonObject.class);
    }

    public <E> Future<Page<E>> page(String sql, Map<String, Object> parameters, Long pageNumber, Long pageSize, Class<E> returnClazz) {
        var pageSql = sql + " LIMIT " + (pageNumber - 1) * pageSize + ", " + pageSize;
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Page\r\n--------------\r\n{}\r\n{}",
                    code, pageSql,
                    parameters.entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        Promise<Page<E>> promise = Promise.promise();
        Page<E> page = new Page<>();
        page.setPageNumber(pageNumber);
        page.setPageSize(pageSize);
        list(pageSql, parameters, returnClazz)
                .onSuccess(selectResp -> {
                    page.setObjects(selectResp);
                    count(sql, parameters)
                            .onSuccess(countResp -> {
                                page.setRecordTotal(countResp);
                                page.setPageTotal((page.getRecordTotal() + pageSize - 1) / pageSize);
                                promise.complete(page);
                            })
                            .onFailure(promise::fail);
                })
                .onFailure(promise::fail);
        return promise.future();
    }

    public <E extends IdEntity> Future<Long> save(E entity) {
        var tableName = entity.tableName();
        var caseEntity = CaseFormatter.camelToSnake(convertToJson(entity, true));
        var columns = caseEntity.stream().map(Map.Entry::getKey).collect(Collectors.joining(", "));
        var values = caseEntity.stream().map(j -> "#{" + j.getKey() + "}").collect(Collectors.joining(", "));
        var sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Save\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    caseEntity.stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return updateHandler(SqlTemplate
                .forUpdate(client, sql)
                .mapFrom(TupleMapper.jsonObject())
                .execute(caseEntity), sql);
    }

    public <E extends IdEntity> Future<Long> save(List<E> entities) {
        var tableName = entities.get(0).tableName();
        var jsonEntities = entities.stream().map(entity -> convertToJson(entity, true)).collect(Collectors.toList());
        var caseEntity = CaseFormatter.camelToSnake(jsonEntities.get(0));
        var columns = caseEntity.stream().map(Map.Entry::getKey).collect(Collectors.joining(", "));
        var values = caseEntity.stream().map(j -> "#{" + j.getKey() + "}").collect(Collectors.joining(", "));
        var sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Save Batch(" + jsonEntities.size() + ") , first record\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    caseEntity.stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return updateHandler(SqlTemplate.forUpdate(client, sql)
                .mapFrom(TupleMapper.jsonObject())
                .executeBatch(jsonEntities.stream()
                        .map(CaseFormatter::camelToSnake)
                        .collect(Collectors.toList())), sql);
    }

    public Future<Long> save(String sql, Map<String, Object> parameters) {
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Save\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    parameters.entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return updateHandler(SqlTemplate.forUpdate(client, sql).execute(parameters), sql);
    }

    public Future<Long> save(String sql, List<Map<String, Object>> parameters) {
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Save Batch(" + parameters.size() + ") , first record\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    parameters.get(0).entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return updateHandler(SqlTemplate.forUpdate(client, sql).executeBatch(parameters), sql);
    }

    public <E extends IdEntity> Future<Void> update(E entity) {
        return update(entity.getId(), entity);
    }

    public Future<Void> update(String sql, Map<String, Object> parameters) {
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Update\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    parameters.entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return updateHandler(SqlTemplate.forUpdate(client, sql).execute(parameters), sql)
                .compose(resp -> Future.succeededFuture());
    }

    public Future<Void> update(String sql, List<Map<String, Object>> parameters) {
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Update Batch(" + parameters.size() + ") , first record\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    parameters.get(0).entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return updateHandler(SqlTemplate.forUpdate(client, sql).executeBatch(parameters), sql)
                .compose(resp -> Future.succeededFuture());
    }

    public <E extends IdEntity> Future<Void> update(Object id, E setItems) {
        return update(new HashMap<>() {
            {
                put("id", id);
            }
        }, setItems);
    }

    public <E extends IdEntity> Future<Void> update(Map<String, Object> whereParameters, E setItems) {
        var tableName = setItems.tableName();
        var caseEntity = CaseFormatter.camelToSnake(convertToJson(setItems, false))
                .stream()
                .filter(j -> j.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var sets = caseEntity.keySet()
                .stream()
                .map(o -> o + " = #{" + o + "}").collect(Collectors.joining(", "));
        var sql = packageWhere("UPDATE " + tableName + " SET " + sets, whereParameters);
        caseEntity.putAll(whereParameters);
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Update\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    caseEntity.entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return updateHandler(SqlTemplate.forUpdate(client, sql).execute(caseEntity), sql)
                .compose(resp -> Future.succeededFuture());
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Void> delete(Object id, Class<E> entityClazz) {
        return delete(new HashMap<>() {
            {
                put("id", id);
            }
        }, entityClazz);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Void> delete(Map<String, Object> whereParameters, Class<E> entityClazz) {
        var sql = packageWhere("DELETE FROM " + entityClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return delete(sql, whereParameters);
    }

    public Future<Void> delete(String sql, Map<String, Object> parameters) {
        return update(sql, parameters);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Void> softDelete(Object id, Class<E> entityClazz) {
        return softDelete(new HashMap<>() {
            {
                put("id", id);
            }
        }, entityClazz);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Void> softDelete(Map<String, Object> whereParameters, Class<E> entityClazz) {
        var sql = packageWhere("SELECT * FROM " + entityClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return softDelete(sql, whereParameters, entityClazz);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Void> softDelete(String sql, Map<String, Object> parameters, Class<E> entityClazz) {
        var tableName = entityClazz.getDeclaredConstructor().newInstance().tableName();
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]SoftDelete\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    parameters.entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return tx(client ->
                client.list(sql, parameters, Map.class)
                        .compose(selectResp -> {
                            if (selectResp.isEmpty()) {
                                return Future.succeededFuture(null);
                            }
                            Promise<List<Map<String, Object>>> insertP = Promise.promise();
                            var softDelEntities = selectResp.stream()
                                    .map(deleteObj -> SoftDelEntity.builder()
                                            .entityName(entityClazz.getSimpleName())
                                            .recordId(deleteObj.get("id").toString())
                                            .content(JsonObject.mapFrom(deleteObj).toString())
                                            .build())
                                    .collect(Collectors.toList());
                            client.save(softDelEntities)
                                    .onSuccess(insertResult ->
                                            insertP.complete(selectResp.stream()
                                                    .map(deleteObj -> {
                                                        var item = new HashMap<String, Object>();
                                                        item.put("id", deleteObj.get("id"));
                                                        return item;
                                                    })
                                                    .collect(Collectors.toList())))
                                    .onFailure(insertP::fail);
                            return insertP.future();
                        })
                        .compose(ids -> {
                            if (ids == null) {
                                return Future.succeededFuture();
                            }
                            return client.update("DELETE FROM " + tableName + " WHERE id = #{id}", ids);
                        })
        );
    }

    public <E> Future<E> tx(ProcessContext context, Supplier<Future<E>> function) {
        Promise<E> promise = Promise.promise();
        if (client instanceof SqlConnection) {
            function.get()
                    .onComplete(result -> {
                        if (result.failed()) {
                            log.error("[SQL][{}]Transaction [{}] error: {}", code, result.cause().getMessage(), result.cause());
                            promise.fail(result.cause());
                        } else {
                            promise.complete(result.result());
                        }
                    });
        } else {
            ((Pool) client).withTransaction(client -> {
                context.sql = txInstance(client);
                return function.get();
            })
                    .onComplete(result -> {
                        context.sql = this;
                        if (result.failed()) {
                            log.error("[SQL][{}]Transaction [{}] error: {}", code, result.cause().getMessage(), result.cause());
                            promise.fail(result.cause());
                        } else {
                            promise.complete(result.result());
                        }
                    });
        }
        return promise.future();
    }

    public <E> Future<E> tx(Function<FunSQLClient, Future<E>> function) {
        Promise<E> promise = Promise.promise();
        if (client instanceof SqlConnection) {
            function.apply(this)
                    .onComplete(result -> {
                        if (result.failed()) {
                            log.error("[SQL][{}]Transaction [{}] error: {}", code, result.cause().getMessage(), result.cause());
                            promise.fail(result.cause());
                        } else {
                            promise.complete(result.result());
                        }
                    });
        } else {
            ((Pool) client).withTransaction(client -> function.apply(txInstance(client)))
                    .onComplete(result -> {
                        if (result.failed()) {
                            log.error("[SQL][{}]Transaction [{}] error: {}", code, result.cause().getMessage(), result.cause());
                            promise.fail(result.cause());
                        } else {
                            promise.complete(result.result());
                        }
                    });
        }
        return promise.future();
    }

    private FunSQLClient txInstance(SqlConnection client) {
        var txClient = new FunSQLClient();
        txClient.client = client;
        txClient.addEntityByInsertFun = this.addEntityByInsertFun;
        txClient.addEntityByUpdateFun = this.addEntityByUpdateFun;
        return txClient;
    }

    private String packageWhere(String sqlWithoutWhere, Map<String, Object> whereParameters) {
        var sql = sqlWithoutWhere + " WHERE 1 = 1";
        var optKeys = new ArrayList<String>();
        sql += whereParameters.keySet().stream()
                .map(params -> {
                    if (params.startsWith("!")) {
                        optKeys.add(params);
                        return " AND " + params.substring(1) + " != #{" + params.substring(1) + "}";
                    }
                    if (params.startsWith("%")) {
                        optKeys.add(params);
                        return " AND " + params.substring(1) + " like #{" + params.substring(1) + "}";
                    }
                    return " AND " + params + " = #{" + params + "}";
                })
                .collect(Collectors.joining());
        optKeys.forEach(neKey -> {
            whereParameters.put(neKey.substring(1), whereParameters.get(neKey));
            whereParameters.remove(neKey);
        });
        return sql;
    }

    private Future<Long> updateHandler(Future<SqlResult<Void>> handler, String sql) {
        Promise<Long> promise = Promise.promise();
        handler
                .onSuccess(records -> {
                    if (sql.trim().toLowerCase().startsWith("insert into")) {
                        promise.complete(records.property(MySQLClient.LAST_INSERTED_ID));
                    } else {
                        promise.complete((long) records.rowCount());
                    }
                })
                .onFailure(e -> {
                    log.error("[SQL][{}]SaveOrUpdate [{}] error: {}", code, sql, e.getMessage(), e);
                    promise.fail(e);
                });
        return promise.future();
    }

    public Future<JsonArray> rawExec(String sql, List parameters) {
        Promise<JsonArray> promise = Promise.promise();
        client.preparedQuery(sql)
                .execute(Tuple.from(parameters))
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        var result = new JsonArray();
                        var rows = ar.result();
                        for (Row row : rows) {
                            result.add(row.toJson());
                        }
                        promise.complete(result);
                    } else {
                        log.error("[SQL][{}]Raw Execute error: {}", code, ar.cause().getMessage(), ar.cause());
                        promise.fail(ar.cause());
                    }
                });
        return promise.future();
    }

    private <E extends IdEntity> JsonObject convertToJson(E entity, Boolean insert) {
        if (insert && addEntityByInsertFun != null) {
            addEntityByInsertFun.accept(entity);
        } else if (addEntityByUpdateFun != null) {
            addEntityByUpdateFun.accept(entity);
        }
        var json = JsonObject.mapFrom(entity);
        json.remove("createTime");
        json.remove("updateTime");
        return json;
    }

}
