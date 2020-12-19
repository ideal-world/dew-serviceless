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

package idealworld.dew.serviceless.iam.test;

import com.ecfront.dew.common.Resp;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.serviceless.iam.IAMModule;
import idealworld.dew.serviceless.iam.domain.ident.Tenant;
import idealworld.dew.serviceless.iam.process.systemconsole.dto.TenantAddReq;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class IAMEventBusTest extends DewTest {

    static {
        enableRedis();
        enableMysql();
    }

    private static String moduleName = new IAMModule().getModuleName();

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        System.getProperties().put("dew.profile", "test");
        vertx.deployVerticle(new IAMApplicationTest(), testContext.succeedingThenComplete());
    }

    @Test
    public void testAddTenant(Vertx vertx, VertxTestContext testContext) {
        FunEventBus.choose(moduleName).request(moduleName, OptActionKind.CREATE, "http://iam/console/system/tenant",
                JsonObject.mapFrom(TenantAddReq.builder()
                        .name(" 租户1 ")
                        .build()).toBuffer(), new HashMap<>())
                .onSuccess(resp -> {
                    Assertions.assertEquals(1, Resp.generic(resp._0.toString(StandardCharsets.UTF_8), Long.class).getBody());
                    FunSQLClient.choose(moduleName).list("select * from " + new Tenant().tableName(), new HashMap<>(), Tenant.class)
                            .onSuccess(tenants -> {
                                Assertions.assertEquals(1, tenants.size());
                                Assertions.assertEquals("租户1", tenants.get(0).getName());
                                testContext.completeNow();
                            });
                });
    }

}
