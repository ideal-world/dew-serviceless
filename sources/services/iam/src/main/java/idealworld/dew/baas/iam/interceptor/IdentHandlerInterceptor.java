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
import group.idealworld.dew.Dew;
import group.idealworld.dew.core.DewContext;
import group.idealworld.dew.core.auth.dto.OptInfo;
import group.idealworld.dew.core.web.error.ErrorController;
import idealworld.dew.baas.common.dto.IdentOptCacheInfo;
import idealworld.dew.baas.iam.IAMConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import javax.security.auth.message.AuthException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

/**
 * Ident Servlet拦截器.
 *
 * @author gudaoxuri
 * @author gjason
 */
@Slf4j
@Component
public class IdentHandlerInterceptor implements AsyncHandlerInterceptor {

    @Autowired
    private IAMConfig iamConfig;
    @Autowired
    private InterceptService interceptService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 配置跨域参数
        response.addHeader("Access-Control-Allow-Origin", Dew.dewConfig.getSecurity().getCors().getAllowOrigin());
        response.addHeader("Access-Control-Allow-Methods", Dew.dewConfig.getSecurity().getCors().getAllowMethods());
        response.addHeader("Access-Control-Allow-Headers", Dew.dewConfig.getSecurity().getCors().getAllowHeaders());
        response.addHeader("Access-Control-Max-Age", "3600000");
        response.addHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        if (request.getMethod().equalsIgnoreCase("OPTIONS") || request.getMethod().equalsIgnoreCase("HEAD")) {
            return true;
        }

        log.trace("[{}] {}{} from {}",
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString() == null ? "" : "?" + request.getQueryString(), Dew.Util.getRealIP(request));

        // fetch token
        String token;
        String tokenKind;
        if (Dew.dewConfig.getSecurity().isTokenInHeader()) {
            token = request.getHeader(Dew.dewConfig.getSecurity().getTokenFlag());
            tokenKind = request.getHeader(Dew.dewConfig.getSecurity().getTokenKindFlag());
        } else {
            token = request.getParameter(Dew.dewConfig.getSecurity().getTokenFlag());
            tokenKind = request.getParameter(Dew.dewConfig.getSecurity().getTokenKindFlag());
        }
        if (token != null) {
            token = URLDecoder.decode(token, StandardCharsets.UTF_8);
            if (Dew.dewConfig.getSecurity().isTokenHash()) {
                token = $.security.digest.digest(token, "MD5");
            }
        }
        if (tokenKind == null) {
            tokenKind = OptInfo.DEFAULT_TOKEN_KIND_FLAG;
        }
        if (token != null) {
            var identOptInfo = Dew.auth.getOptInfo(token);
            if (identOptInfo.isEmpty()) {
                ErrorController.error(request, response, Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                        "Token不合法",
                        AuthException.class.getName());
                return false;
            }
            var context = new DewContext();
            context.setId($.field.createUUID());
            context.setSourceIP(Dew.Util.getRealIP(request));
            context.setRequestUri(request.getRequestURI());
            context.setToken(token);
            context.setTokenKind(tokenKind);
            context.setInnerOptInfo(identOptInfo);
            DewContext.setContext(context);
            return true;
        }

        // fetch ak/sk
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
        if (!interceptService.isTenantLegal(tenantId) ||
                !interceptService.isAppLegal(appId)) {
            ErrorController.error(request, response, Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                    "租户或应用不合法",
                    AuthException.class.getName());
            return false;
        }
        var identOptInfo = new IdentOptCacheInfo();
        identOptInfo.setAppId(appId);
        identOptInfo.setTenantId(tenantId);
        var context = new DewContext();
        context.setId($.field.createUUID());
        context.setSourceIP(Dew.Util.getRealIP(request));
        context.setRequestUri(request.getRequestURI());
        context.setToken(null);
        context.setTokenKind(null);
        context.setInnerOptInfo(Optional.of(identOptInfo));
        DewContext.setContext(context);
        return true;
    }

}
