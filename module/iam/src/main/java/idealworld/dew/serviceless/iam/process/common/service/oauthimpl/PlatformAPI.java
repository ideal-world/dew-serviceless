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

package idealworld.dew.serviceless.iam.process.common.service.oauthimpl;

import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.process.common.service.OAuthProcessor;
import io.vertx.core.Future;


/**
 * Platform api.
 *
 * @author gudaoxuri
 */
public interface PlatformAPI {

    String getPlatformFlag();

    Future<OAuthProcessor.OAuthUserInfo> doGetUserInfo(String code, String ak, String sk, ProcessContext context);

    default Future<Tuple2<String, OAuthProcessor.OAuthUserInfo>> getUserInfo(String code, String ak, String sk, Long appId, ProcessContext context) {
        return doGetUserInfo(code, ak, sk, context)
                .compose(userInfo -> getAccessToken(ak, sk, appId, context)
                        .compose(accessToken -> context.helper.success(new Tuple2<>(accessToken, userInfo))));
    }

    Future<Tuple2<String, Long>> doGetAccessToken(String ak, String sk, ProcessContext context);

    default Future<String> getAccessToken(String ak, String sk, Long appId, ProcessContext context) {
        return context.fun.cache.get(IAMConstant.CACHE_ACCESS_TOKEN + appId + ":" + getPlatformFlag())
                .compose(accessToken -> {
                    if (accessToken != null) {
                        return context.helper.success(accessToken);
                    }
                    return doGetAccessToken(ak, sk, context)
                            .compose(getAccessToken ->
                                    context.fun.cache.setex(IAMConstant.CACHE_ACCESS_TOKEN + appId + ":" + getPlatformFlag(), getAccessToken._0, getAccessToken._1 - 10)
                                            .compose(resp -> context.helper.success(getAccessToken._0)));
                });
    }

}
