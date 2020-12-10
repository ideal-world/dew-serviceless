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

package idealworld.dew.serviceless.iam.scene.common.service.oauthimpl;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.HttpHelper;
import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.tuple.Tuple2;
import com.fasterxml.jackson.databind.JsonNode;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.scene.common.service.IAMBasicService;
import idealworld.dew.serviceless.iam.scene.common.service.OAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Wechat mini program service.
 *
 * @author gudaoxuri
 */
@Service
@Slf4j
public class WechatXCXAPI extends IAMBasicService implements PlatformAPI {

    @Override
    public String getPlatformFlag() {
        return AccountIdentKind.WECHAT_XCX.toString();
    }

    @Override
    public Resp<OAuthService.OAuthUserInfo> doGetUserInfo(String code, String ak, String sk) {
        HttpHelper.ResponseWrap response = $.http.postWrap(
                "https://api.weixin.qq.com/sns/jscode2session?appid="
                        + ak + "&secret="
                        + sk + "&js_code="
                        + code
                        + "&grant_type=authorization_code", "", "application/json");
        if (response.statusCode != 200) {
            return StandardResp.custom(String.valueOf(response.statusCode), BUSINESS_OAUTH, "微信接口调用异常");
        }
        log.trace("Wechat response : {}", response.result);
        var userInfoResp = $.json.toJson(response.result);
        // 0成功，-1系统繁忙，40029 code无效，45011 访问次数限制（100次/分钟）
        if (userInfoResp.has("errcode")
                && !userInfoResp.get("errcode").asText().equalsIgnoreCase("0")) {
            return StandardResp.custom(userInfoResp.get("errcode").asText(), BUSINESS_OAUTH, userInfoResp.get("errmsg").asText());
        }
        return StandardResp.success($.json.toObject(response.result, OAuthService.OAuthUserInfo.class));
    }

    @Override
    public Resp<Tuple2<String, Long>> doGetAccessToken(String ak, String sk) {
        HttpHelper.ResponseWrap response = $.http.getWrap(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                        + ak
                        + "&secret="
                        + sk);
        if (response.statusCode != 200) {
            return StandardResp.custom(String.valueOf(response.statusCode), BUSINESS_OAUTH, "微信接口调用异常");
        }
        JsonNode jsonNode = $.json.toJson(response.result);
        if (jsonNode.has("access_token")) {
            var accessToken = jsonNode.get("access_token").asText();
            var expiresIn = jsonNode.get("expires_in").asLong();
            return StandardResp.success(new Tuple2<>(accessToken, expiresIn));
        } else {
            return StandardResp.custom(jsonNode.get("errcode").asText(), BUSINESS_OAUTH, "微信接口调用异常");
        }
    }

}
