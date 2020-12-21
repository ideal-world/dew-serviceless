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

package idealworld.dew.serviceless.iam.process.common.oauthimpl;

import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.exception.ServiceException;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.serviceless.iam.dto.AccountIdentKind;
import idealworld.dew.serviceless.iam.process.common.OAuthProcessor;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

/**
 * Wechat mini program service.
 *
 * @author gudaoxuri
 */
@Slf4j
public class WechatXCXAPI implements PlatformAPI {

    @Override
    public String getPlatformFlag() {
        return AccountIdentKind.WECHAT_XCX.toString();
    }

    @Override
    public Future<OAuthProcessor.OAuthUserInfo> doGetUserInfo(String code, String ak, String sk, ProcessContext context) {
        return context.http.request(HttpMethod.POST, "https://api.weixin.qq.com/sns/jscode2session?appid="
                + ak + "&secret="
                + sk + "&js_code="
                + code
                + "&grant_type=authorization_code", Buffer.buffer(""))
                .compose(response -> {
                    if (response.statusCode() != 200) {
                        context.helper.error(new ServiceException("微信接口调用异常"));
                    }
                    var body = response.body().toString();
                    log.trace("Wechat response : {}", body);
                    var userInfoResp = new JsonObject(body);
                    // 0成功，-1系统繁忙，40029 code无效，45011 访问次数限制（100次/分钟）
                    if (userInfoResp.containsKey("errcode")
                            && !userInfoResp.getString("errcode").equalsIgnoreCase("0")) {
                        context.helper.error(new ServiceException("[" + userInfoResp.getString("errcode") + "]" + userInfoResp.getString("errmsg")));
                    }
                    return context.helper.success(userInfoResp.mapTo(OAuthProcessor.OAuthUserInfo.class));
                });
    }

    @Override
    public Future<Tuple2<String, Long>> doGetAccessToken(String ak, String sk, ProcessContext context) {
        return context.http.request(HttpMethod.GET, "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
                + ak
                + "&secret="
                + sk)
                .compose(response -> {
                    if (response.statusCode() != 200) {
                        context.helper.error(new ServiceException("微信接口调用异常"));
                    }
                    var accountToken = new JsonObject(response.body());
                    if (!accountToken.containsKey("access_token")) {
                        context.helper.error(new ServiceException("[" + accountToken.getString("errcode") + "]微信接口调用异常"));
                    }
                    var accessToken = accountToken.getString("access_token");
                    var expiresIn = accountToken.getLong("expires_in");
                    return context.helper.success(new Tuple2<>(accessToken, expiresIn));
                });
    }

}
