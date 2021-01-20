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

package idealworld.dew.framework.test;

import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.framework.util.CaseFormatter;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CaseFormatterTest extends DewTest {

    @SneakyThrows
    @Test
    public void testHttp(Vertx vertx, VertxTestContext testContext) {
        Assertions.assertEquals("rsa_kind", CaseFormatter.camelToSnake("rsaKind"));
        Assertions.assertEquals("rsa_kind_opt", CaseFormatter.camelToSnake("rsaKindOpt"));
        Assertions.assertEquals("_rsa_kind_opt", CaseFormatter.camelToSnake("RsaKindOpt"));
        Assertions.assertEquals("_r_s_a_kind_opt", CaseFormatter.camelToSnake("RSAKindOpt"));
        Assertions.assertEquals("rsaKind", CaseFormatter.snakeToCamel("rsa_kind"));
        Assertions.assertEquals("rsaKindOpt", CaseFormatter.snakeToCamel("rsa_kind_opt"));
        Assertions.assertEquals("RsaKindOpt", CaseFormatter.snakeToCamel("_rsa_kind_opt"));
        Assertions.assertEquals("RSAKindOpt", CaseFormatter.snakeToCamel("_r_s_a_kind_opt"));

        Assertions.assertEquals(new JsonObject()
                        .put("rsa_kind", "v1")
                        .put("rsa_kind_opt", new JsonObject().put("_rsa_kind_opt", "v2")).toString(),
                CaseFormatter.camelToSnake(
                        new JsonObject()
                                .put("rsaKind", "v1")
                                .put("rsaKindOpt", new JsonObject().put("RsaKindOpt", "v2"))).toString()
                );
        Assertions.assertEquals(new JsonObject()
                        .put("rsaKind", "v1")
                        .put("rsaKindOpt", new JsonObject().put("RsaKindOpt", "v2")).toString(),
                CaseFormatter.snakeToCamel(
                        new JsonObject()
                                .put("rsa_kind", "v1")
                                .put("rsa_kind_opt", new JsonObject().put("_rsa_kind_opt", "v2"))).toString()
                );

        testContext.completeNow();
    }

}
