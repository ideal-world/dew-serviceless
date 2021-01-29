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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.AuthCacheProcessor;
import idealworld.dew.framework.fun.eventbus.FunEventBus;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.IAMModule;
import idealworld.dew.serviceless.iam.domain.ident.App;
import idealworld.dew.serviceless.iam.process.common.dto.account.AccountLoginReq;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeAll;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * IAM测试基础类.
 *
 * @author gudaoxuri
 */
public class IAMBasicTest extends DewTest {

    protected static final String MODULE_NAME = new IAMModule().getModuleName();
    protected static String iamAppCode;
    private static IAMConfig iamConfig;
    private static ProcessContext context;

    static {
        enableRedis();
        enableMysql();
    }

    private String token = null;

    @BeforeAll
    public static void init(Vertx vertx, VertxTestContext testContext) {
        var iamApplicationTest = new IAMApplicationTest();
        var mapIamConfig = (Map) iamApplicationTest.loadConfig().getModules().get(0).getConfig();
        iamConfig = mapIamConfig != null ? JsonObject.mapFrom(mapIamConfig).mapTo(IAMConfig.class) : IAMConfig.builder().build();
        vertx.deployVerticle(iamApplicationTest, event -> {
            context = ProcessContext.builder().moduleName(MODULE_NAME).build().init(IdentOptExchangeInfo.builder().build());
            context.sql.getOne(1L, App.class)
                    .onSuccess(app -> {
                        iamAppCode = app.getOpenId();
                        testContext.completeNow();
                    })
                    .onFailure(testContext::failNow);
        });
    }

    protected <E> Tuple2<Page<E>, Throwable> reqPage(String pathAndQuery, Long pageNumber, Long pageSize, Class<E> returnClazz) {
        if (!pathAndQuery.contains("?")) {
            pathAndQuery += "?";
        } else {
            pathAndQuery += "&";
        }
        var result = request(OptActionKind.FETCH, pathAndQuery + "pageNumber=" + pageNumber + "&pageSize=" + pageSize, null);
        if (result._1 != null) {
            return new Tuple2<>(null, result._1);
        }
        var page = result._0.toJsonObject();
        return new Tuple2<>(Page.build(page.getLong("pageNumber"), page.getLong("pageSize"), page.getLong("recordTotal"),
                page.getJsonArray("objects")
                        .stream()
                        .map(item -> (E) parseBody(item, returnClazz))
                        .collect(Collectors.toList())), null);
    }

    protected <E> Tuple2<List<E>, Throwable> reqList(String pathAndQuery, Class<E> returnClazz) {
        var result = request(OptActionKind.FETCH, pathAndQuery, null);
        if (result._1 != null) {
            return new Tuple2<>(null, result._1);
        }
        return new Tuple2<>(result._0.toJsonArray()
                .stream().map(item -> (E) parseBody(item, returnClazz))
                .collect(Collectors.toList()), null);
    }

    protected <E> Tuple2<E, Throwable> req(OptActionKind actionKind, String pathAndQuery, Object body, Class<E> returnClazz) {
        var result = request(actionKind, pathAndQuery, body);
        if (result._1 != null) {
            return new Tuple2<>(null, result._1);
        }
        return new Tuple2<>((E) parseBody(result._0, returnClazz), null);
    }

    private <E> Object parseBody(Object body, Class<E> returnClazz) {
        if (Long.class == returnClazz) {
            return Long.parseLong(body instanceof Buffer ? body.toString() : (String) body);
        } else if (Integer.class == returnClazz) {
            return Integer.parseInt(body instanceof Buffer ? body.toString() : (String) body);
        } else if (Float.class == returnClazz) {
            return Float.parseFloat(body instanceof Buffer ? body.toString() : (String) body);
        } else if (Double.class == returnClazz) {
            return Double.parseDouble(body instanceof Buffer ? body.toString() : (String) body);
        } else if (String.class == returnClazz) {
            return body instanceof Buffer ? body.toString() : (String) body;
        } else if (Boolean.class == returnClazz) {
            return Boolean.parseBoolean(body instanceof Buffer ? body.toString() : (String) body);
        } else if (JsonObject.class == returnClazz) {
            return body instanceof Buffer ? ((Buffer) body).toJsonObject() : body;
        } else if (JsonArray.class == returnClazz) {
            return body instanceof Buffer ? ((Buffer) body).toJsonArray() : body;
        } else if (Void.class == returnClazz) {
            return null;
        } else {
            return body instanceof Buffer
                    ? ((Buffer) body).toJsonObject().mapTo(returnClazz)
                    : body instanceof JsonObject
                    ? ((JsonObject) body).mapTo(returnClazz)
                    : new JsonObject((String) body).mapTo(returnClazz);
        }
    }

    @SneakyThrows
    private Tuple2<Buffer, Throwable> request(OptActionKind actionKind, String pathAndQuery, Object body) {
        var bufBody = body == null
                ? Buffer.buffer()
                : body instanceof String
                ? Buffer.buffer((String) body, "utf-8")
                : JsonObject.mapFrom(body).toBuffer();
        var header = new HashMap<String, String>();
        var identOptInfo = token == null
                ? IdentOptExchangeInfo.builder().build() : await(AuthCacheProcessor.getOptInfo(token, context))._0.get();
        header.put(DewAuthConstant.REQUEST_IDENT_OPT_FLAG,
                $.security.encodeStringToBase64(JsonObject.mapFrom(identOptInfo).toString(), StandardCharsets.UTF_8));
        var req = FunEventBus.choose(MODULE_NAME)
                .request(MODULE_NAME,
                        actionKind,
                        "http://" + MODULE_NAME + pathAndQuery,
                        bufBody,
                        header);
        return awaitRequest(req);
    }

    protected void loginBySystemAdmin() {
        loginBySystemAdmin(iamAppCode);
    }

    protected void loginBySystemAdmin(String appCode) {
        token = req(OptActionKind.CREATE, "/common/login", AccountLoginReq.builder()
                .ak(iamConfig.getApp().getIamAdminName())
                .sk(iamConfig.getApp().getIamAdminPwd())
                .relAppCode(appCode)
                .build(), IdentOptInfo.class)._0.getToken();
    }

    protected void setToken(String token) {
        this.token = token;
    }

    protected void removeToken() {
        token = null;
    }

}
