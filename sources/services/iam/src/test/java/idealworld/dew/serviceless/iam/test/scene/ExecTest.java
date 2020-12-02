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

package idealworld.dew.serviceless.iam.test.scene;

import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.ResourceAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.ResourceSubjectAddReq;
import idealworld.dew.serviceless.iam.test.BasicTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

public class ExecTest extends BasicTest {

    @Test
    public void testExec() throws InterruptedException {
        loginBySystemAdmin();

        // 添加当前应用的资源主体
        var resourceSubjectRelDBId = postToEntity("/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix(IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_POSTFIX)
                .kind(ResourceKind.RELDB)
                .name("h2")
                .uri("jdbc:h2:mem:test1")
                .build(), Long.class).getBody();
        var resourceSubjectCacheId = postToEntity("/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix(IAMConstant.RESOURCE_SUBJECT_DEFAULT_CODE_POSTFIX)
                .kind(ResourceKind.CACHE)
                .name("redis")
                .uri("redis://localhost:6379/10")
                .build(), Long.class).getBody();
        var resourceSubjectRestId = postToEntity("/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix("httpbin")
                .kind(ResourceKind.HTTP)
                .name("httpbin")
                .uri("https://httpbin.org")
                .build(), Long.class).getBody();

        // 添加当前应用的资源
        postToEntity("/console/app/resource", ResourceAddReq.builder()
                .name("h2")
                .pathAndQuery("/**")
                .relResourceSubjectId(resourceSubjectRelDBId)
                .build(), Long.class);
        postToEntity("/console/app/resource", ResourceAddReq.builder()
                .name("redis")
                .pathAndQuery("/**")
                .relResourceSubjectId(resourceSubjectCacheId)
                .build(), Long.class);
        postToEntity("/console/app/resource", ResourceAddReq.builder()
                .name("httpbin")
                .pathAndQuery("/**")
                .relResourceSubjectId(resourceSubjectRestId)
                .build(), Long.class);

        new CountDownLatch(1).await();
    }

}