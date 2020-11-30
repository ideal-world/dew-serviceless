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

import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.tuple.Tuple2;
import group.idealworld.dew.Dew;
import idealworld.dew.serviceless.common.resp.StandardResp;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.scene.common.service.OAuthService;

/**
 * Platform api.
 *
 * @author gudaoxuri
 */
public interface PlatformAPI {

    String getPlatformFlag();

    Resp<OAuthService.OAuthUserInfo> doGetUserInfo(String code, String ak, String sk);

    default Resp<Tuple2<String, OAuthService.OAuthUserInfo>> getUserInfo(String code, String ak, String sk, Long appId) {
        var userInfoR = doGetUserInfo(code, ak, sk);
        if (userInfoR.ok()) {
            var accessTokenR = getAccessToken(ak, sk, appId);
            if (accessTokenR.ok()) {
                return Resp.success(new Tuple2<>(accessTokenR.getBody(), userInfoR.getBody()));
            }
            return Resp.error(accessTokenR);
        }
        return Resp.error(userInfoR);
    }

    Resp<Tuple2<String, Long>> doGetAccessToken(String ak, String sk);

    default Resp<String> getAccessToken(String ak, String sk, Long appId) {
        var accessToken = Dew.cluster.cache.get(IAMConstant.CACHE_ACCESS_TOKEN + appId + ":" + getPlatformFlag());
        if (accessToken != null) {
            return StandardResp.success(accessToken);
        }
        var getR = doGetAccessToken(ak, sk);
        if (!getR.ok()) {
            return StandardResp.error(getR);
        }
        Dew.cluster.cache.setex(IAMConstant.CACHE_ACCESS_TOKEN + appId + ":" + getPlatformFlag(), getR.getBody()._0, getR.getBody()._1 - 10);
        return StandardResp.success(getR.getBody()._0);
    }

}
