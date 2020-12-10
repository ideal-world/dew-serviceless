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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import group.idealworld.dew.Dew;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.IdentOptInfo;
import idealworld.dew.framework.util.ResponseProcessor;
import idealworld.dew.serviceless.iam.IAMConfig;
import idealworld.dew.serviceless.iam.scene.common.dto.account.AccountLoginReq;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Basic test.
 *
 * @author gudaoxuri
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestDewIAMApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public abstract class BasicTest extends ResponseProcessor {

    @Value("${server.port:8080}")
    private int serverPort;

    @Autowired
    private IAMConfig iamConfig;

    private String token;

    protected void loginBySystemAdmin() {
        loginBySystemAdmin(1L);
    }

    protected void loginBySystemAdmin(Long appId) {
        token = postToEntity("/common/login", AccountLoginReq.builder()
                .ak(iamConfig.getApp().getIamAdminName())
                .sk(iamConfig.getApp().getIamAdminPwd())
                .relAppId(appId)
                .build(), IdentOptInfo.class).getBody().getToken();
    }

    protected void setToken(String token) {
        this.token = token;
    }

    protected void removeToken() {
        token = null;
    }

    @Override
    public Resp<?> exchange(String method, String url, Object body, Map<String, String> header) {
        var identOptCacheInfo = token != null ? Dew.auth.getOptInfo(token).get() : new IdentOptCacheInfo();
        header.put(Constant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64($.json.toJsonString(identOptCacheInfo), StandardCharsets.UTF_8));
        var result = $.http.request(method, formatUrl(url), body, header,
                null, null,
                -1).result;
        return $.json.toObject(result, Resp.class);
    }

    private String formatUrl(String url) {
        if (url.toLowerCase().startsWith("http://")) {
            return url;
        }
        if (!url.startsWith("/")) {
            url += "/";
        }
        return "http://127.0.0.1:" + serverPort + url;
    }
}
