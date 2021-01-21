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

package idealworld.dew.serviceless.iam.test;

import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.serviceless.iam.process.systemconsole.dto.TenantAddReq;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 系统控制台下的租户控制器测试.
 *
 * @author gudaoxuri
 */
public class SystemConsoleTest extends IAMBasicTest {

    @Test
    public void testAddTenant(Vertx vertx, VertxTestContext testContext) {
        loginBySystemAdmin();
        // 租户注册
        var tenantId = req(OptActionKind.CREATE, "/console/system/tenant", TenantAddReq.builder()
                .name("xyy")
                .build(), Long.class);
        Assertions.assertNotNull(tenantId);
        testContext.completeNow();
    }

}
