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

package idealworld.dew.serviceless.gateway.process;

import idealworld.dew.serviceless.common.auth.AuthenticationProcessor;
import idealworld.dew.serviceless.common.enumeration.AuthResultKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 鉴权策略.
 * <p>
 * Redis格式：
 * <p>
 * 资源类型:资源URI:资源操作类型 = {权限主体运算类型:{权限主体类型:[权限主体Id]}}
 *
 * @author gudaoxuri
 */
@Slf4j
public class GatewayAuthPolicy {

    public GatewayAuthPolicy(Integer resourceCacheExpireSec, Integer groupNodeLength) {
        AuthenticationProcessor.init(resourceCacheExpireSec, groupNodeLength);
    }

    public Future<AuthResultKind> authentication(
            String actionKind,
            URI resourceUri,
            Map<AuthSubjectKind, List<String>> subjectInfo
    ) {
        return AuthenticationProcessor.authentication(actionKind, resourceUri, subjectInfo);
    }


}
