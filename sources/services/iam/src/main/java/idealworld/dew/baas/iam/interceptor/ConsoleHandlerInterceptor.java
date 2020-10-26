/*
 * Copyright 2020. the original author or authors.
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

package idealworld.dew.baas.iam.interceptor;

import com.ecfront.dew.common.StandardCode;
import group.idealworld.dew.Dew;
import group.idealworld.dew.core.web.error.ErrorController;
import idealworld.dew.baas.common.dto.IdentOptInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.security.auth.message.AuthException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * App Servlet拦截器.
 *
 * @author gudaoxuri
 */
@Component
@Slf4j
public class ConsoleHandlerInterceptor implements AsyncHandlerInterceptor {

    @Autowired
    private InterceptService interceptService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        var identOptInfo = (IdentOptInfo) Dew.context().optInfo().get();
        if (!interceptService.isTenantLegal(identOptInfo.getTenantId())
                || !interceptService.isAppLegal(identOptInfo.getAppId())) {
            ErrorController.error(request, response, Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                    "租户或应用不合法",
                    AuthException.class.getName());
            return false;
        }
        return preHandle(request, response, handler);
    }

}
