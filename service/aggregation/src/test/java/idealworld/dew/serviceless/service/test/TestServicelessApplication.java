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

package idealworld.dew.serviceless.service.test;

import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.serviceless.iam.IAMModule;
import idealworld.dew.serviceless.iam.domain.ident.App;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class TestServicelessApplication extends ITBasicTest {

    private static final String IAM_USERNAME = "dew";
    private static final String DEW_PASSWORD = "TestPwd1d";
    private static String iamAppCode;

    @SneakyThrows
    @Disabled
    @Test
    public void testServer(Vertx vertx, VertxTestContext testContext) {
        var context = ProcessContext.builder().moduleName(new IAMModule().getModuleName()).build().init(IdentOptExchangeInfo.builder().build());
        iamAppCode = await(context.sql.getOne(1L, App.class))._0.getOpenId();
        var iamIdentOpt = req(OptActionKind.CREATE, "http://iam.http.iam/common/login", new HashMap<String, Object>() {
            {
                put("ak", IAM_USERNAME);
                put("sk", DEW_PASSWORD);
                put("relAppCode", iamAppCode);
            }
        }, null, null, Map.class);
        var iamToken = iamIdentOpt.get("token").toString();
        // 应用管理员添加数据库资源主体
        req(OptActionKind.CREATE, "http://iam.http.iam/console/app/resource/subject", new HashMap<String, Object>() {
            {
                put("codePostfix", "default");
                put("kind", "CACHE");
                put("name", "默认缓存");
                put("uri", "redis://localhost:" + redisConfig.getFirstMappedPort() + "/10");
            }
        }, iamToken, null, Long.class);
        req(OptActionKind.CREATE, "http://iam.http.iam/console/app/resource/subject", new HashMap<String, Object>() {
            {
                put("codePostfix", "default");
                put("kind", "RELDB");
                put("name", "默认数据库");
                put("uri", "mysql://127.0.0.1:" + mysqlConfig.getFirstMappedPort() + "/" + mysqlConfig.getDatabaseName());
                put("ak", mysqlConfig.getUsername());
                put("sk", mysqlConfig.getPassword());
            }
        }, iamToken, null, Long.class);
        req(OptActionKind.CREATE, "http://iam.http.iam/console/app/resource/subject", new HashMap<String, Object>() {
            {
                put("codePostfix", "httpbin");
                put("kind", "HTTP");
                put("name", "httpbin API");
                put("uri", "https://httpbin.org");
            }
        }, iamToken, null, Long.class);

        log.info("\n====================\n" +
                "iam appId(OpenId): " + iamAppCode + "\n" +
                "=====================");
        new CountDownLatch(1).await();
    }

}
