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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.StandardCode;
import com.ecfront.dew.common.tuple.Tuple2;
import group.idealworld.dew.core.web.error.ErrorController;
import idealworld.dew.baas.iam.IAMConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.security.auth.message.AuthException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * App Servlet拦截器.
 *
 * @author gudaoxuri
 */
@Component
@Slf4j
public class AppHandlerInterceptor implements AsyncHandlerInterceptor {

    private static final ThreadLocal<Tuple2<Long, Long>> CONTEXT_TENANT_AND_APP_ID = new ThreadLocal<>();

    @Autowired
    private IAMConfig iamConfig;
    @Autowired
    private InterceptService interceptService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (request.getMethod().equalsIgnoreCase("OPTIONS") || request.getMethod().equalsIgnoreCase("HEAD")) {
            return preHandle(request, response, handler);
        }
        var authorization = request.getHeader(iamConfig.getApp().getAuthFieldName());
        if (StringUtils.isEmpty(authorization)
                || !authorization.contains(":")
                || authorization.split(":").length != 2) {
            ErrorController.error(request, response, Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                    "认证错误，请检查 HTTP Header [" + iamConfig.getApp().getAuthFieldName() + "] 格式是否正确",
                    AuthException.class.getName());
            return false;
        }
        var ak = authorization.split(":")[0];
        var legalSkAndAppIdR = interceptService.getAppIdentByAk(ak);
        if (!legalSkAndAppIdR.ok()) {
            ErrorController.error(request, response, Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                    "认证错误，请检查AK是否合法",
                    AuthException.class.getName());
            return false;
        }
        var reqSignature = authorization.split(":")[1];
        var reqMethod = request.getMethod();
        var reqDate = request.getHeader("Dew-Date");
        var sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (sdf.parse(reqDate).getTime() + iamConfig.getSecurity().getAppRequestDateOffsetMs() < System.currentTimeMillis()) {
            ErrorController.error(request, response, Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                    "请求时间已过期",
                    AuthException.class.getName());
            return false;
        }
        var reqPath = request.getRequestURI();
        var reqQuery = request.getQueryString() != null ? request.getQueryString() : "";
        var sk = legalSkAndAppIdR.getBody()._0;
        var tenantId = legalSkAndAppIdR.getBody()._1;
        var appId = legalSkAndAppIdR.getBody()._2;
        var calcSignature = $.security.encodeStringToBase64(
                $.security.digest.digest((reqMethod + "\n" + reqDate + "\n" + reqPath + "\n" + reqQuery).toLowerCase(),
                        sk, "HmacSHA1"),
                StandardCharsets.UTF_8);
        if (!reqSignature.equalsIgnoreCase(calcSignature)) {
            ErrorController.error(request, response, Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                    "认证错误，请检查签名是否合法",
                    AuthException.class.getName());
            return false;
        }
        if (!interceptService.checkTenantStatus(tenantId) ||
                !interceptService.checkAppStatus(appId)) {
            ErrorController.error(request, response, Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                    "租户或应用不合法",
                    AuthException.class.getName());
            return false;
        }
        CONTEXT_TENANT_AND_APP_ID.set(new Tuple2<>(tenantId, appId));
        return preHandle(request, response, handler);
    }

    public Tuple2<Long, Long> getCurrentTenantAndAppId() {
        return CONTEXT_TENANT_AND_APP_ID.get();
    }

}
