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

package idealworld.dew.serviceless.iam.interceptor;

import com.ecfront.dew.common.$;
import group.idealworld.dew.Dew;
import group.idealworld.dew.core.DewContext;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.serviceless.common.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Dew Servlet拦截器.
 *
 * @author gudaoxuri
 * @author gjason
 */
public class AuthHandlerInterceptor implements AsyncHandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuthHandlerInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var identOpt = $.json.toObject($.security.decodeBase64ToString(request.getHeader(Constant.REQUEST_IDENT_OPT_FLAG), StandardCharsets.UTF_8), IdentOptCacheInfo.class);
        DewContext context = new DewContext();
        context.setId($.field.createUUID());
        context.setSourceIP(Dew.Util.getRealIP(request));
        context.setRequestUri(request.getRequestURI());
        context.setTokenKind(identOpt.getToken());
        if (identOpt.getAppId() == null && identOpt.getToken() == null) {
            context.setToken(null);
            DewContext.setContext(context);
            Dew.context().setInnerOptInfo(Optional.empty());
        } else {
            context.setToken(identOpt.getTokenKind());
            DewContext.setContext(context);
            Dew.context().setInnerOptInfo(Optional.of(identOpt));
        }
        return true;
    }

}
