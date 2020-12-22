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

import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.process.appconsole.dto.resource.ResourceAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.resource.ResourceSubjectAddReq;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

public class ResourceTest extends IAMBasicTest {

    @Test
    public void testResource(Vertx vertx, VertxTestContext testContext) {
        loginBySystemAdmin();

        // 添加当前应用的资源主体
        var resourceSubjectRelDBId = req(OptActionKind.CREATE, "/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix(IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_POSTFIX)
                .kind(ResourceKind.RELDB)
                .name("mysql")
                .uri("mysql://root:123456@127.0.0.1:3306/test")
                .build(), Long.class)._0;
        var resourceSubjectCacheId = req(OptActionKind.CREATE, "/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix(IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_POSTFIX)
                .kind(ResourceKind.CACHE)
                .name("redis")
                .uri("redis://localhost:6379/10")
                .build(), Long.class)._0;
        var resourceSubjectRestId = req(OptActionKind.CREATE, "/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix("httpbin")
                .kind(ResourceKind.HTTP)
                .name("httpbin")
                .uri("https://httpbin.org")
                .build(), Long.class)._0;

        // 添加当前应用的资源
        req(OptActionKind.CREATE, "/console/app/resource", ResourceAddReq.builder()
                .name("h2")
                .pathAndQuery("/**")
                .relResourceSubjectId(resourceSubjectRelDBId)
                .build(), Long.class);
        req(OptActionKind.CREATE, "/console/app/resource", ResourceAddReq.builder()
                .name("redis")
                .pathAndQuery("/**")
                .relResourceSubjectId(resourceSubjectCacheId)
                .build(), Long.class);
        req(OptActionKind.CREATE, "/console/app/resource", ResourceAddReq.builder()
                .name("httpbin")
                .pathAndQuery("/**")
                .relResourceSubjectId(resourceSubjectRestId)
                .build(), Long.class);

        testContext.completeNow();
    }

}
