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

package idealworld.dew.serviceless.it;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleIT {

    private static final String gatewayServerUrl = "http://127.0.0.1:9000";
    private static final String iamUrl = "http://127.0.0.1:8081";

    private String req(String resUri, OptActionKind optActionKind, Object body, String token, String appId) {
        var header = new HashMap<String, String>();
        if (token != null) {
            header.put("Dew-Token", token);
        }
        if (appId != null) {
            header.put("Dew-App-Id", appId);
        }
        var response = $.http.postWrap(gatewayServerUrl + Constant.REQUEST_PATH_FLAG + "?"
                + Constant.REQUEST_RESOURCE_URI_FLAG + "=" + resUri
                + "&"
                + Constant.REQUEST_RESOURCE_ACTION_FLAG + "=" + optActionKind.toString(), JsonObject.mapTo(body).toString(), header);
        if (response.statusCode != 200) {
            Assertions.fail("Request [" + optActionKind.toString() + "][" + resUri + "] error : [" + response.statusCode + "]");
        }
        var result = $.json.toJson(response.result);
        if (!result.get("code").asText().equals("200")) {
            Assertions.fail("Request [" + optActionKind.toString() + "][" + resUri + "] error : [" + result.get("code").asText() + "]:" + result.get("message").asText());
        }
        return response.result;
    }

    private <E> E reqToObj(String resUri, OptActionKind optActionKind, Object body, String token, String appId, Class<E> bodyClazz) {
        return Resp.generic(req(resUri, optActionKind, body, token, appId), bodyClazz).getBody();
    }

    private <E> List<E> reqToList(String resUri, OptActionKind optActionKind, Object body, String token, String appId, Class<E> bodyClazz) {
        return Resp.genericList(req(resUri, optActionKind, body, token, appId), bodyClazz).getBody();
    }

    private <E> Page<E> reqToPage(String resUri, OptActionKind optActionKind, Object body, String token, String appId, Class<E> bodyClazz) {
        return Resp.genericPage(req(resUri, optActionKind, body, token, appId), bodyClazz).getBody();
    }

    @Test
    public void testIAM() {
        // 添加租户
        var identOpt = reqToObj(iamUrl + "/common/tenant", OptActionKind.CREATE, new HashMap<String, Object>() {
            {
                put("tenantName", "测试租户1");
                put("appName", "测试应用");
                put("accountUserName", "gudaoxuri");
                put("accountPassword", "o2dd^!fdd");
            }
        }, null, null, Map.class);
        Assertions.assertNotNull(identOpt);
        var token = identOpt.get("token").toString();
        // TODO
    }

}
