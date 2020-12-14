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
import com.ecfront.dew.common.Page;
import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.domain.IdEntity;
import idealworld.dew.framework.domain.SafeEntity;
import idealworld.dew.framework.domain.SoftDelEntity;
import idealworld.dew.framework.exception.BadRequestException;
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

import java.util.ArrayList;
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

    public Future<Long> ddl(String sql, Object... formatArgs) {
        return updateHandler(SqlTemplate
                        .forUpdate(client, String.format(sql, formatArgs))
                        .execute(new HashMap<>()),
                sql);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<List<E>> list(Map<String, Object> whereParameters, Class<E> returnClazz, ProcessContext context) {
        var sql = packageWhere("SELECT * FROM " + returnClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return list(sql, whereParameters, returnClazz, context);
    }

    public Future<List<JsonObject>> list(String sql, Map<String, Object> parameters, ProcessContext context, Object... formatArgs) {
        return list(sql, parameters, JsonObject.class, context, formatArgs);
    }

    public <E> Future<List<E>> list(String sql, Map<String, Object> parameters, Class<E> returnClazz, ProcessContext context, Object... formatArgs) {
        var selectSql = String.format(sql, formatArgs);
        Promise<List<E>> promise = Promise.promise();
        SqlTemplate
                .forQuery(client, selectSql)
                .mapTo(Row::toJson)
                .execute(parameters)
                .onSuccess(records -> promise.complete(
                        $.fun.stream(records.iterator())
                                .map(rowJson -> returnClazz == JsonObject.class
                                        ? (E) CaseFormatter.snakeToCamel(rowJson)
                                        : CaseFormatter.snakeToCamel(rowJson).mapTo(returnClazz))
                                .collect(Collectors.toList())))
                .onFailure(e -> {
                    log.error("[SQL]Select [{}] error: {}", selectSql, e.getMessage(), e);
                    promise.fail(e);
                });
        return promise.future();
    }

    @SneakyThrows
    public <E extends IdEntity> Future<E> getOne(Object id, Class<E> returnClazz, ProcessContext context) {
        return getOne(new HashMap<>() {
            {
                put("id", id);
            }
        }, returnClazz, context);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<E> getOne(Map<String, Object> whereParameters, Class<E> returnClazz, ProcessContext context) {
        var sql = packageWhere("SELECT * FROM " + returnClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return getOne(sql, whereParameters, returnClazz, context);
    }

    public Future<JsonObject> getOne(String sql, Map<String, Object> parameters, ProcessContext context, Object... formatArgs) {
        return getOne(sql, parameters, JsonObject.class, context, formatArgs);
    }

    public <E> Future<E> getOne(String sql, Map<String, Object> parameters, Class<E> returnClazz, ProcessContext context, Object... formatArgs) {
        return list(sql, parameters, returnClazz, context, formatArgs)
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
    public <E extends IdEntity> Future<Boolean> exists(Map<String, Object> whereParameters, Class<E> entityClazz, ProcessContext context) {
        var sql = packageWhere("SELECT 1 FROM " + entityClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return count(sql, whereParameters, context)
                .compose(resp -> Future.succeededFuture(resp > 0));
    }

    public Future<Boolean> exists(String sql, Map<String, Object> parameters, ProcessContext context, Object... formatArgs) {
        return count(sql, parameters, context, formatArgs)
                .compose(resp -> Future.succeededFuture(resp > 0));
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Long> count(Map<String, Object> whereParameters, Class<E> entityClazz, ProcessContext context) {
        var sql = packageWhere("SELECT 1 FROM " + entityClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return count(sql, whereParameters, context);
    }

    public Future<Long> count(String sql, Map<String, Object> parameters, ProcessContext context, Object... formatArgs) {
        var countSql = "SELECT COUNT(1) _count FROM (" + String.format(sql, formatArgs) + ") _" + System.currentTimeMillis();
        Promise<Long> promise = Promise.promise();
        SqlTemplate
                .forQuery(client, countSql)
                .mapTo(Row::toJson)
                .execute(parameters)
                .onSuccess(records -> promise.complete(records.iterator().next().getLong("_count")))
                .onFailure(e -> {
                    log.error("[SQL]Select [{}] error: {}", countSql, e.getMessage(), e);
                    promise.fail(e);
                });
        return promise.future();
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Page<E>> page(Map<String, Object> whereParameters, Class<E> returnClazz, ProcessContext context) {
        var sql = packageWhere("SELECT * FROM " + returnClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return page(sql, whereParameters, returnClazz, context);
    }

    public Future<Page<JsonObject>> page(String sql, Map<String, Object> parameters, ProcessContext context, Object... formatArgs) {
        return page(sql, parameters, JsonObject.class, context, formatArgs);
    }

    public <E> Future<Page<E>> page(String sql, Map<String, Object> parameters, Class<E> returnClazz, ProcessContext context, Object... formatArgs) {
        var selectSql = String.format(sql, formatArgs);
        var pageNumber = Long.parseLong(context.req.params.get("pageNumber"));
        var pageSize = Long.parseLong(context.req.params.get("pageSize"));
        var pageSql = selectSql + " LIMIT " + (pageNumber - 1) * pageSize + ", " + pageSize;
        Promise<Page<E>> promise = Promise.promise();
        Page<E> page = new Page<>();
        page.setPageNumber(pageNumber);
        page.setPageSize(pageSize);
        list(pageSql, parameters, returnClazz, context, formatArgs)
                .onSuccess(selectResp -> {
                    page.setObjects(selectResp);
                    count(selectSql, parameters, context)
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

    public <E extends IdEntity> Future<Long> save(E entity, ProcessContext context) {
        var tableName = entity.tableName();
        addSafeInfo(entity, context, true);
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

    public <E extends IdEntity> Future<Long> save(List<E> entities, ProcessContext context) {
        var tableName = entities.get(0).tableName();
        entities.forEach(entity -> addSafeInfo(entity, context, true));
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

    public Future<Long> update(String sql, Map<String, Object> parameters, ProcessContext context, Object... formatArgs) {
        return updateHandler(SqlTemplate
                        .forUpdate(client, String.format(sql, formatArgs))
                        .execute(parameters),
                sql);
    }

    public Future<Long> update(String sql, List<Map<String, Object>> parameters, ProcessContext context, Object... formatArgs) {
        return updateHandler(SqlTemplate
                        .forUpdate(client, String.format(sql, formatArgs))
                        .executeBatch(parameters),
                sql);
    }

    public <E extends IdEntity> Future<Long> update(Object id, E setItems, ProcessContext context) {
        return update(new HashMap<>() {
            {
                put("id", id);
            }
        }, setItems, context);
    }

    public <E extends IdEntity> Future<Long> update(Map<String, Object> whereParameters, E setItems, ProcessContext context) {
        var tableName = setItems.tableName();
        addSafeInfo(setItems, context, false);
        var caseEntity = CaseFormatter.camelToSnake(JsonObject.mapFrom(setItems))
                .stream()
                .filter(j -> j.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        var sets = caseEntity.keySet()
                .stream()
                .map(o -> "SET " + o + " = #{" + o + "}").collect(Collectors.joining(", "));
        var sql = packageWhere("UPDATE " + tableName + " " + sets, whereParameters);
        caseEntity.putAll(whereParameters);
        return updateHandler(SqlTemplate
                        .forUpdate(client, sql)
                        .execute(caseEntity),
                sql);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Long> delete(Object id, Class<E> entityClazz, ProcessContext context) {
        return delete(new HashMap<>() {
            {
                put("id", id);
            }
        }, entityClazz, context);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Long> delete(Map<String, Object> whereParameters, Class<E> entityClazz, ProcessContext context) {
        var sql = packageWhere("DELETE FROM " + entityClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return delete(sql, whereParameters, context);
    }


    public Future<Long> delete(String sql, Map<String, Object> parameters, ProcessContext context, Object... formatArgs) {
        return update(String.format(sql, formatArgs), parameters, context);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Long> softDelete(Map<String, Object> whereParameters, Class<E> entityClazz, ProcessContext context) {
        var sql = packageWhere("SELECT * FROM " + entityClazz.getDeclaredConstructor().newInstance().tableName(), whereParameters);
        return softDelete(sql, whereParameters, entityClazz, context);
    }

    @SneakyThrows
    public <E extends IdEntity> Future<Long> softDelete(String sql, Map<String, Object> parameters, Class<E> entityClazz, ProcessContext context, Object... formatArgs) {
        var tableName = entityClazz.getDeclaredConstructor().newInstance().tableName();
        return tx(client ->
                client.list(String.format(sql, formatArgs), parameters, Map.class, context)
                        .compose(selectResp -> {
                            Promise<List<Map<String, Object>>> insertP = Promise.promise();
                            var softDelEntities = selectResp.stream()
                                    .map(deleteObj -> SoftDelEntity.builder()
                                            .entityName(entityClazz.getSimpleName())
                                            .recordId(deleteObj.get("id").toString())
                                            .content(JsonObject.mapFrom(deleteObj).toString())
                                            .build())
                                    .collect(Collectors.toList());
                            client.save(softDelEntities, context)
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
                        .compose(idR -> client.update("DELETE FROM " + tableName + " WHERE id = #{id}", idR, context))
        );
    }

    public <E> Future<E> tx(Function<FunSQLClient, Future<E>> function) {
        Promise<E> promise = Promise.promise();
        if (client instanceof SqlConnection) {
            function.apply(this)
                    .onComplete(result -> {
                        if (result.failed()) {
                            log.error("[SQL]Transaction [{}] error: {}", result.cause().getMessage(), result.cause());
                            promise.fail(result.cause());
                        } else {
                            promise.complete(result.result());
                        }
                    });
        } else {
            ((Pool) client).withTransaction(client -> function.apply(txInstance(client)))
                    .onComplete(result -> {
                        if (result.failed()) {
                            log.error("[SQL]Transaction [{}] error: {}", result.cause().getMessage(), result.cause());
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
        return txClient;
    }

    private String packageWhere(String sqlWithoutWhere, Map<String, Object> whereParameters) {
        var sql = sqlWithoutWhere + " WHERE 1 = 1";
        var neKeys = new ArrayList<String>();
        sql += whereParameters.keySet().stream()
                .map(params -> {
                    if (!params.startsWith("-")) {
                        return " AND " + params + " = #{" + params + "}";
                    } else {
                        neKeys.add(params);
                        return " AND " + params.substring(1) + " != #{" + params.substring(1) + "}";
                    }
                })
                .collect(Collectors.joining());
        neKeys.forEach(neKey -> {
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
                    log.error("[SQL]Update [{}] error: {}", sql, e.getMessage(), e);
                    promise.fail(e);
                });
        return promise.future();
    }

    private <E extends IdEntity> void addSafeInfo(E entity, ProcessContext context, Boolean insert) {
        if (context == null) {
            return;
        }
        if (entity instanceof SafeEntity) {
            if (insert) {
                ((SafeEntity) entity).setCreateUser(
                        context.req.identOptInfo.getAccountCode() != null
                                ? (String) context.req.identOptInfo.getAccountCode() :
                                "");
            }
            ((SafeEntity) entity).setUpdateUser(
                    context.req.identOptInfo.getAccountCode() != null
                            ? (String) context.req.identOptInfo.getAccountCode() :
                            "");
        }
    }

}
