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
import com.ecfront.dew.common.tuple.Tuple2;
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

import java.util.*;
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
    private static final Map<Class<?>, String> TABLE_NAMES = new ConcurrentHashMap<>();
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

    public <E extends IdEntity> Future<List<E>> list(Class<E> returnClazz, Map<String, Object> whereParameters) {
        var sql = packageWhere("SELECT * FROM " + getTableName(returnClazz), whereParameters);
        return list(returnClazz, sql, whereParameters);
    }

    public Future<List<JsonObject>> list(String sql, Object... parameters) {
        var parseResult = parseSqlAndParameters(sql, parameters);
        return list(JsonObject.class, parseResult._0, parseResult._1);
    }

    public <E> Future<List<E>> list(Class<E> returnClazz, String sql, Object... parameters) {
        var parseResult = parseSqlAndParameters(sql, parameters);
        return list(returnClazz, parseResult._0, parseResult._1);
    }

    private <E> Future<List<E>> list(Class<E> returnClazz, String sql, Map<String, Object> parameters) {
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

    public <E extends IdEntity> Future<E> getOne(Class<E> returnClazz, Object id) {
        return getOne(returnClazz,
                new HashMap<>() {
                    {
                        put("id", id);
                    }
                });
    }

    public <E extends IdEntity> Future<E> getOne(Class<E> returnClazz, Map<String, Object> whereParameters) {
        var sql = packageWhere("SELECT * FROM " + getTableName(returnClazz), whereParameters);
        return getOne(returnClazz, sql, whereParameters);
    }

    public Future<JsonObject> getOne(String sql, Object... parameters) {
        var parseResult = parseSqlAndParameters(sql, parameters);
        return getOne(JsonObject.class, parseResult._0, parseResult._1);
    }

    public <E> Future<E> getOne(Class<E> returnClazz, String sql, Object... parameters) {
        var parseResult = parseSqlAndParameters(sql, parameters);
        return getOne(returnClazz, parseResult._0, parseResult._1);
    }

    private <E> Future<E> getOne(Class<E> returnClazz, String sql, Map<String, Object> parameters) {
        return list(returnClazz, sql, parameters)
                .compose(resp -> {
                    if (resp.size() > 1) {
                        return Future.failedFuture(new BadRequestException("Query to multiple records."));
                    } else if (resp.isEmpty()) {
                        return Future.succeededFuture(null);
                    }
                    return Future.succeededFuture(resp.get(0));
                });
    }

    public <E extends IdEntity> Future<Boolean> exists(Class<E> entityClazz, Map<String, Object> whereParameters) {
        var sql = packageWhere("SELECT 1 FROM " + getTableName(entityClazz), whereParameters);
        return count(sql, whereParameters)
                .compose(resp -> Future.succeededFuture(resp > 0));
    }

    public Future<Boolean> exists(String sql, Object... parameters) {
        var parseResult = parseSqlAndParameters(sql, parameters);
        return exists(parseResult._0, parseResult._1);
    }

    private Future<Boolean> exists(String sql, Map<String, Object> parameters) {
        return count(sql, parameters)
                .compose(resp -> Future.succeededFuture(resp > 0));
    }

    public <E extends IdEntity> Future<Long> count(Class<E> entityClazz, Map<String, Object> whereParameters) {
        var sql = packageWhere("SELECT 1 FROM " + getTableName(entityClazz), whereParameters);
        return count(sql, whereParameters);
    }

    public Future<Long> count(String sql, Object... parameters) {
        var parseResult = parseSqlAndParameters(sql, parameters);
        return count(parseResult._0, parseResult._1);
    }

    private Future<Long> count(String sql, Map<String, Object> parameters) {
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

    public <E extends IdEntity> Future<Page<E>> page(Class<E> returnClazz, Long pageNumber, Long pageSize, Map<String, Object> whereParameters) {
        var sql = packageWhere("SELECT * FROM " + getTableName(returnClazz), whereParameters);
        return page(returnClazz, sql, pageNumber, pageSize, whereParameters);
    }

    public Future<Page<JsonObject>> page(String sql, Long pageNumber, Long pageSize, Object... parameters) {
        var parseResult = parseSqlAndParameters(sql, parameters);
        return page(JsonObject.class, parseResult._0, pageNumber, pageSize, parseResult._1);
    }

    public <E> Future<Page<E>> page(Class<E> returnClazz, String sql, Long pageNumber, Long pageSize, Object... parameters) {
        var parseResult = parseSqlAndParameters(sql, parameters);
        return page(returnClazz, parseResult._0, pageNumber, pageSize, parseResult._1);
    }

    private <E> Future<Page<E>> page(Class<E> returnClazz, String sql, Long pageNumber, Long pageSize, Map<String, Object> parameters) {
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
        list(returnClazz, pageSql, parameters)
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
        var tableName = getTableName(entity.getClass());
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
        var tableName = getTableName(entities.get(0).getClass());
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

    public <E extends IdEntity> Future<Void> update(E entity) {
        return update(entity, entity.getId());
    }


    public <E extends IdEntity> Future<Void> update(E setItems, Object id) {
        return update(setItems,
                new HashMap<>() {
                    {
                        put("id", id);
                    }
                });
    }

    public <E extends IdEntity> Future<Void> update(E setItems, Map<String, Object> whereParameters) {
        var tableName = getTableName(setItems.getClass());
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

    public <E extends IdEntity> Future<Void> delete(Class<E> entityClazz, Object id) {
        return delete(entityClazz,
                new HashMap<>() {
                    {
                        put("id", id);
                    }
                });
    }

    public <E extends IdEntity> Future<Void> delete(Class<E> entityClazz, Map<String, Object> whereParameters) {
        var sql = packageWhere("DELETE FROM " + getTableName(entityClazz), whereParameters);
        return execute(sql, whereParameters)
                .compose(resp -> Future.succeededFuture());
    }

    public <E extends IdEntity> Future<Void> softDelete(Class<E> entityClazz, Object id) {
        return softDelete(entityClazz,
                new HashMap<>() {
                    {
                        put("id", id);
                    }
                });
    }

    public <E extends IdEntity> Future<Void> softDelete(Class<E> entityClazz, Map<String, Object> whereParameters) {
        var sql = packageWhere("SELECT * FROM " + getTableName(entityClazz), whereParameters);
        return softDelete(entityClazz, sql, whereParameters);
    }

    public <E extends IdEntity> Future<Void> softDelete(Class<E> entityClazz, String sql, Object... parameters) {
        var parseResult = parseSqlAndParameters(sql, parameters);
        return softDelete(entityClazz, parseResult._0, parseResult._1);
    }

    private <E extends IdEntity> Future<Void> softDelete(Class<E> entityClazz, String sql, Map<String, Object> parameters) {
        var tableName = getTableName(entityClazz);
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]SoftDelete\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    parameters.entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return tx(client ->
                client.list(Map.class, sql, parameters)
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
                            return client.execute("DELETE FROM " + tableName + " WHERE id = #{id}", ids)
                                    .compose(resp -> Future.succeededFuture());
                        })
        );
    }

    public Future<Long> execute(String sql, Object... parameters) {
        var parseResult = parseSqlAndParameters(sql, parameters);
        return execute(parseResult._0, parseResult._1);
    }

    public Future<Long> executeBatch(String sql, List<Object[]> parameters) {
        if (parameters.isEmpty()) {
            return Future.succeededFuture(0L);
        }
        var parseResult = parseSqlAndParameters(sql, parameters.get(0));
        var parameterMap = parameters.stream()
                .map(paramItems -> {
                    Map<String, Object> params = new HashMap<>();
                    var idx = 0;
                    for (String key : parseResult._1.keySet()) {
                        params.put(key, paramItems[idx++]);
                    }
                    return params;
                })
                .collect(Collectors.toList());
        return execute(parseResult._0, parameterMap);
    }

    private Future<Long> execute(String sql, Map<String, Object> parameters) {
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Execute\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    parameters.entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return updateHandler(SqlTemplate.forUpdate(client, sql).execute(parameters), sql);
    }

    private Future<Long> execute(String sql, List<Map<String, Object>> parameters) {
        if (log.isTraceEnabled()) {
            log.trace("[SQL][{}]Execute Batch(" + parameters.size() + ") , first record\r\n--------------\r\n{}\r\n{}",
                    code, sql,
                    parameters.get(0).entrySet().stream()
                            .map(entry -> entry.getKey() + " = " + entry.getValue())
                            .collect(Collectors.joining("\r\n", "-------\r\n", "\r\n--------------")));
        }
        return updateHandler(SqlTemplate.forUpdate(client, sql).executeBatch(parameters), sql);
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

    private Tuple2<String, Map<String, Object>> parseSqlAndParameters(String sql, Object... parameters) {
        Map<String, Object> parameterMap = new LinkedHashMap<>();
        if (parameters.length == 0) {
            return new Tuple2<>(sql, parameterMap);
        }
        if(parameters.length==1 && parameters[0] instanceof Collection){
            parameters = ((Collection)parameters[0]).toArray();
        }
        var idx = 0;
        while (sql.contains("%s") || sql.contains("?")) {
            if (sql.contains("%s") && sql.indexOf("%s") < sql.indexOf("?")) {
                // 替换 %s
                if (parameters[idx] instanceof Class<?> && IdEntity.class.isAssignableFrom((Class<?>) parameters[idx])) {
                    sql = sql.replaceFirst("%s", getTableName((Class<IdEntity>) parameters[idx]));
                } else {
                    sql = sql.replaceFirst("%s", parameters[idx].toString());
                }
            } else {
                // 替换 ?
                sql = sql.replaceFirst("\\?", "#{p_" + idx + "}");
                parameterMap.put("p_" + idx, parameters[idx]);
            }
            idx++;
        }
        return new Tuple2<>(sql, parameterMap);
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
                    if (sql.trim().toLowerCase().startsWith("insert")) {
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

    @SneakyThrows
    private <E extends IdEntity> String getTableName(Class<E> entityClazz) {
        if (!TABLE_NAMES.containsKey(entityClazz)) {
            var obj = entityClazz.getDeclaredConstructor().newInstance();
            var method = $.bean.getMethods(entityClazz).stream().filter(m -> m.getName().equals("tableName")).findFirst().get();
            var tableName = $.bean.getValue(obj, method);
            TABLE_NAMES.put(entityClazz, (String) tableName);
        }
        return TABLE_NAMES.get(entityClazz);
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
