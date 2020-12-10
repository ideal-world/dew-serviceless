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

package idealworld.dew.framework.fun.test;

import io.vertx.junit5.VertxExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;

@Testcontainers
@ExtendWith(VertxExtension.class)
public abstract class DewTest {

    protected static void enableRedis() {
        redisConfig = new GenericContainer("redis:6-alpine")
                .withExposedPorts(6379);
        redisConfig.start();
        System.getProperties().put("dew.config.funs.redis.uri", "redis://localhost:" + redisConfig.getFirstMappedPort());
    }

    protected static void enableMysql() {
        var scriptPath = ClassLoader.getSystemResource("").getPath() + "/sql/init.sql";
        mysqlConfig = new MySQLContainer("mysql:8");
        if (new File(scriptPath).exists()) {
            mysqlConfig.withInitScript("sql/init.sql");
        }
        mysqlConfig.start();
        System.getProperties().put("dew.config.funs.sql.host", mysqlConfig.getHost());
        System.getProperties().put("dew.config.funs.sql.port", mysqlConfig.getFirstMappedPort());
        System.getProperties().put("dew.config.funs.sql.db", mysqlConfig.getDatabaseName());
        System.getProperties().put("dew.config.funs.sql.userName", mysqlConfig.getUsername());
        System.getProperties().put("dew.config.funs.sql.password", mysqlConfig.getPassword());
    }

    protected static GenericContainer redisConfig;
    protected static JdbcDatabaseContainer mysqlConfig;

}
