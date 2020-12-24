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

package idealworld.dew.serviceless.service.test;

import idealworld.dew.framework.dto.OptActionKind;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class TestServicelessApplication extends ITBasicTest {

    private static final Integer IAM_APP_ID = 1;
    private static final String IAM_USERNAME = "dew";
    private static final String DEW_PASSWORD = "TestPwd1d";

    @SneakyThrows
    @Disabled
    @Test
    public void testServer(Vertx vertx, VertxTestContext testContext) {
        var iamIdentOpt = req(OptActionKind.CREATE, "http://iam/common/login", new HashMap<String, Object>() {
            {
                put("ak", IAM_USERNAME);
                put("sk", DEW_PASSWORD);
                put("relAppId", IAM_APP_ID);
            }
        }, null, null, Map.class);
        var iamToken = iamIdentOpt.get("token").toString();
        // 应用管理员添加数据库资源主体
        req(OptActionKind.CREATE, "http://iam/console/app/resource/subject", new HashMap<String, Object>() {
            {
                put("codePostfix", "default");
                put("kind", "CACHE");
                put("name", "默认缓存");
                put("uri", "redis://localhost:" + redisConfig.getFirstMappedPort() + "/10");
            }
        }, iamToken, null, Long.class);
        req(OptActionKind.CREATE, "http://iam/console/app/resource/subject", new HashMap<String, Object>() {
            {
                put("codePostfix", "default");
                put("kind", "RELDB");
                put("name", "默认数据库");
                put("uri", "mysql://127.0.0.1:" + mysqlConfig.getFirstMappedPort() + "/" + mysqlConfig.getDatabaseName());
                put("ak", mysqlConfig.getUsername());
                put("sk", mysqlConfig.getPassword());
            }
        }, iamToken, null, Long.class);
        req(OptActionKind.CREATE, "http://iam/console/app/resource/subject", new HashMap<String, Object>() {
            {
                put("codePostfix", "httpbin");
                put("kind", "HTTP");
                put("name", "httpbin API");
                put("uri", "https://httpbin.org");
            }
        }, iamToken, null, Long.class);
        new CountDownLatch(1).await();
    }


}
