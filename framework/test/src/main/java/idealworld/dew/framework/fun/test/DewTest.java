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

package idealworld.dew.framework.fun.test;

import com.ecfront.dew.common.tuple.Tuple2;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.Future;
import io.vertx.junit5.VertxExtension;
import lombok.SneakyThrows;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.util.Map;

/**
 * 测试基础类.
 *
 * @author gudaoxuri
 */
@Testcontainers
@ExtendWith(VertxExtension.class)
public abstract class DewTest {

    protected static GenericContainer redisConfig;
    protected static JdbcDatabaseContainer mysqlConfig;

    static {
        System.getProperties().put("dew.profile", "test");
        ObjectMapper mapper = io.vertx.core.json.jackson.DatabindCodec.mapper();
        mapper.registerModule(new JavaTimeModule());
    }

    protected static void enableRedis() {
        redisConfig = new GenericContainer("redis:6-alpine")
                .withExposedPorts(6379);
        redisConfig.start();
        System.out.println("Test Redis port: " + redisConfig.getFirstMappedPort());
        System.getProperties().put("dew.config.funs.cache.uri", "redis://localhost:" + redisConfig.getFirstMappedPort());
    }

    protected static void enableMysql() {
        var scriptPath = ClassLoader.getSystemResource("").getPath() + "/sql/init.sql";
        mysqlConfig = new MySQLContainer("mysql:8");
        if (new File(scriptPath).exists()) {
            mysqlConfig.withInitScript("sql/init.sql");
        }
        mysqlConfig.withCommand("--max_allowed_packet=10M");
        mysqlConfig.start();
        System.out.println("Test mysql port: " + mysqlConfig.getFirstMappedPort() + ", username: " + mysqlConfig.getUsername() + ", password: " + mysqlConfig.getPassword());
        System.getProperties().put("dew.config.funs.sql.host", mysqlConfig.getHost());
        System.getProperties().put("dew.config.funs.sql.port", mysqlConfig.getFirstMappedPort());
        System.getProperties().put("dew.config.funs.sql.db", mysqlConfig.getDatabaseName());
        System.getProperties().put("dew.config.funs.sql.userName", mysqlConfig.getUsername());
        System.getProperties().put("dew.config.funs.sql.password", mysqlConfig.getPassword());
    }

    @SneakyThrows
    protected static <E> Tuple2<E, Throwable> await(Future<E> future) {
        while (!future.isComplete()) {
            Thread.sleep(10);
        }
        if (future.succeeded()) {
            return new Tuple2<>(future.result(), null);
        }
        return new Tuple2<>(null, future.cause());
    }

    @SneakyThrows
    protected static <E> Tuple2<E, Throwable> awaitRequest(Future<Tuple2<E, Map<String, String>>> future) {
        while (!future.isComplete()) {
            Thread.sleep(10);
        }
        if (future.succeeded()) {
            return new Tuple2<>(future.result()._0, null);
        }
        return new Tuple2<>(null, future.cause());
    }

}
