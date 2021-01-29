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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.serviceless.service.ServicelessApplication;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;

public class ITBasicTest extends DewTest {

    static {
        enableRedis();
        enableMysql();
    }

    private static final String gatewayServerUrl = "http://127.0.0.1:9000/exec";

    @BeforeAll
    public static void before(Vertx vertx, VertxTestContext testContext) {
        System.getProperties().put("dew.profile", "test");
        vertx.deployVerticle(new ServicelessApplication(), testContext.succeedingThenComplete());
    }

    private String request(OptActionKind optActionKind, String resourceUri, Object body, String token, Integer appId) {
        var header = new HashMap<String, String>();
        header.put("Dew-Token", token != null ? token : "");
        header.put("Dew-App-Id", appId != null ? appId + "" : "");
        var response = $.http.postWrap(gatewayServerUrl + "?"
                + DewConstant.REQUEST_RESOURCE_URI_FLAG + "=" + URLEncoder.encode(resourceUri, StandardCharsets.UTF_8)
                + "&"
                + DewConstant.REQUEST_RESOURCE_ACTION_FLAG + "=" + optActionKind.toString(), body == null || body instanceof String ?
                Buffer.buffer() : JsonObject.mapFrom(body).toString(), header);
        if (response.statusCode != 200) {
            throw new RTException("Request [" + optActionKind.toString() + "][" + resourceUri + "] error : [" + response.statusCode + "]");
        }
        var result = new JsonObject(response.result);
        if (!result.getString("code").equals("200")) {
            throw new RTException("Request [" + optActionKind.toString() + "][" + resourceUri + "] error : [" + result.getString("code") + "]:"
                    + result.getString("message"));
        }
        return response.result;
    }

    protected <E> E req(OptActionKind optActionKind, String resourceUri, Object body, String token, Integer appId, Class<E> bodyClazz) {
        return Resp.generic(request(optActionKind, resourceUri, body, token, appId), bodyClazz).getBody();
    }

    protected <E> List<E> reqList(OptActionKind optActionKind, String resourceUri, Object body, String token, Integer appId, Class<E> bodyClazz) {
        return Resp.genericList(request(optActionKind, resourceUri, body, token, appId), bodyClazz).getBody();
    }

}
