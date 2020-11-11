package idealworld.dew.baas.common.funs.mysql;

import idealworld.dew.baas.common.CommonConfig;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
@Slf4j
public class MysqlClient {

    private static final Map<String, MysqlClient> MYSQL_CLIENTS = new HashMap<>();

    private MySQLPool client;

    public static Future<Void> init(String code, Vertx vertx, CommonConfig.JDBCConfig config) {
        Promise<Void> promise = Promise.promise();
        var mysqlClient = new MysqlClient();
        var connectOptions = new MySQLConnectOptions()
                .setPort(config.getPort())
                .setHost(config.getHost())
                .setDatabase(config.getDb())
                .setUser(config.getUserName())
                .setPassword(config.getPassword())
                .setCharset(config.getCharset())
                .setCollation(config.getCollation());
        var poolOptions = new PoolOptions()
                .setMaxSize(config.getMaxPoolSize())
                .setMaxWaitQueueSize(config.getMaxPoolWaitQueueSize());
        mysqlClient.client = MySQLPool.pool(vertx, connectOptions, poolOptions);
        MYSQL_CLIENTS.put(code, mysqlClient);
        return promise.future();
    }

    public static MysqlClient choose(String code) {
        return MYSQL_CLIENTS.get(code);
    }

    public static void remove(String code) {
        MYSQL_CLIENTS.remove(code);
    }

    public Future<JsonArray> exec(String sql, Object... parameters) {
        if (parameters.length == 0) {
            return exec(sql, new ArrayList<>());
        }
        return exec(sql, new ArrayList<>() {
            {
                add(Arrays.asList(parameters));
            }
        });
    }

    public Future<JsonArray> exec(String sql, List<List<Object>> parameters) {
        var params = parameters.stream()
                .map(Tuple::tuple)
                .collect(Collectors.toList());
        Promise<JsonArray> promise = Promise.promise();
        client.getConnection(conn -> {
            if (conn.failed()) {
                log.error("[Mysql]Sql execute error: {}", conn.cause().getMessage(), conn.cause());
                promise.fail(conn.cause());
                return;
            }
            var preparedQuery = conn.result().preparedQuery(sql);
            Future<RowSet<Row>> executeResult;
            if (params.size() == 0) {
                executeResult = preparedQuery.execute();
            } else if (params.size() == 1) {
                executeResult = preparedQuery.execute(params.get(0));
            } else {
                executeResult = preparedQuery.executeBatch(params);
            }
            executeResult.onComplete(ar -> {
                if (ar.succeeded()) {
                    var result = new JsonArray();
                    var rows = ar.result();
                    for (Row row : rows) {
                        result.add(row.toJson());
                    }
                    promise.complete(result);
                } else {
                    log.error("[Mysql]Sql execute error: {}", ar.cause().getMessage(), ar.cause());
                    promise.fail(ar.cause());
                }
                conn.result().close();
            });
        });
        return promise.future();
    }

    // TODO 事务


}
