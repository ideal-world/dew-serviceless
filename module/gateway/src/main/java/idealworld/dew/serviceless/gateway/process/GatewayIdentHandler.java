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

package idealworld.dew.serviceless.gateway.process;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.StandardCode;
import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.framework.DewAuthConstant;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.framework.fun.cache.FunCacheClient;
import idealworld.dew.framework.fun.httpserver.AuthHttpHandler;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.gateway.GatewayConfig;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 认证处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public class GatewayIdentHandler extends AuthHttpHandler {

    private final GatewayConfig.Security security;

    public GatewayIdentHandler(String moduleName, GatewayConfig.Security security) {
        super(moduleName);
        this.security = security;
    }

    @SneakyThrows
    @Override
    public void handle(RoutingContext ctx) {
        log.trace("[Process]Received {}:{}{} from {}",
                ctx.request().method(),
                ctx.request().path(),
                ctx.request().query() == null ? "" : "?" + ctx.request().query(), getIP(ctx.request()));
        // checker
        if (ctx.request().query() == null || ctx.request().query().trim().isBlank()) {
            error(StandardCode.BAD_REQUEST, GatewayIdentHandler.class, "请求格式不合法，缺少query", ctx);
            return;
        }
        var queryMap = Arrays.stream(ctx.request().query().trim().split("&"))
                .map(item -> item.split("="))
                .collect(Collectors.toMap(item -> item[0], item -> item.length > 1 ? item[1] : ""));
        if (!queryMap.containsKey(DewAuthConstant.REQUEST_RESOURCE_URI_FLAG)
                || queryMap.get(DewAuthConstant.REQUEST_RESOURCE_URI_FLAG).isBlank()
                || !queryMap.containsKey(DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG)
                || queryMap.get(DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG).isBlank()
        ) {
            error(StandardCode.BAD_REQUEST, GatewayIdentHandler.class, "请求格式不合法，缺少[" + DewAuthConstant.REQUEST_RESOURCE_URI_FLAG + "]或[" + DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG + "]", ctx);
            return;
        }
        URI resourceUri;
        OptActionKind actionKind;
        try {
            resourceUri = URIHelper.newURI(URLDecoder.decode(queryMap.get(DewAuthConstant.REQUEST_RESOURCE_URI_FLAG), StandardCharsets.UTF_8));
            if (resourceUri.getScheme() == null) {
                error(StandardCode.BAD_REQUEST, GatewayIdentHandler.class, "请求格式不合法，资源URI错误", ctx);
                return;
            }
            ResourceKind.parse(resourceUri.getScheme().toLowerCase());
            actionKind = OptActionKind.parse(queryMap.get(DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG).toLowerCase());
        } catch (RTException e) {
            error(StandardCode.BAD_REQUEST, GatewayIdentHandler.class, "请求格式不合法，资源类型或操作类型不存在", ctx);
            return;
        }
        ctx.put(DewAuthConstant.REQUEST_RESOURCE_URI_FLAG, resourceUri);
        ctx.put(DewAuthConstant.REQUEST_RESOURCE_ACTION_FLAG, actionKind);

        var token = ctx.request().headers().contains(security.getTokenFieldName())
                && !ctx.request().getHeader(security.getTokenFieldName()).trim().isBlank()
                ? ctx.request().getHeader(security.getTokenFieldName()).trim() : null;
        var authorization = ctx.request().headers().contains(security.getAkSkFieldName())
                && !ctx.request().getHeader(security.getAkSkFieldName()).trim().isBlank()
                ? ctx.request().getHeader(security.getAkSkFieldName()).trim() : null;

        if (token == null && authorization == null) {
            if (!ctx.request().headers().contains(security.getAppId())
                    || ctx.request().headers().get(security.getAppId()).trim().isBlank()) {
                // E.g. 注册租户
                var identOptCacheInfo = new IdentOptCacheInfo();
                identOptCacheInfo.setAppId(DewAuthConstant.OBJECT_UNDEFINED);
                identOptCacheInfo.setTenantId(DewAuthConstant.OBJECT_UNDEFINED);
                ctx.put(CONTEXT_INFO, identOptCacheInfo);
                ctx.next();
                return;
            }
            var appId = Long.parseLong(ctx.request().headers().get(security.getAppId().trim()));
            FunCacheClient.choose(getModuleName()).get(DewAuthConstant.CACHE_APP_INFO + appId, security.getAppInfoCacheExpireSec())
                    .onSuccess(appInfo -> {
                        if (appInfo == null) {
                            error(StandardCode.UNAUTHORIZED, GatewayIdentHandler.class, "认证错误，AppId不合法", ctx);
                            return;
                        }
                        var appInfoItems = appInfo.split("\n");
                        var tenantId = Long.parseLong(appInfoItems[0]);
                        var identOptCacheInfo = new IdentOptCacheInfo();
                        identOptCacheInfo.setAppId(appId);
                        identOptCacheInfo.setTenantId(tenantId);
                        ctx.put(CONTEXT_INFO, identOptCacheInfo);
                        ctx.next();
                    })
                    .onFailure(e -> error(StandardCode.INTERNAL_SERVER_ERROR, GatewayIdentHandler.class, "缓存服务错误", ctx, e));
            return;
        }

        // fetch token
        if (token != null) {
            FunCacheClient.choose(getModuleName()).get(DewAuthConstant.CACHE_TOKEN_INFO_FLAG + token, security.getTokenCacheExpireSec())
                    .onSuccess(optInfo -> {
                        var identOptInfo = optInfo != null
                                ? new JsonObject(optInfo).mapTo(IdentOptCacheInfo.class)
                                : null;
                        if (optInfo == null) {
                            error(StandardCode.UNAUTHORIZED, GatewayIdentHandler.class, "认证错误，Token不合法", ctx);
                            return;
                        }
                        identOptInfo.setToken(token);
                        ctx.put(CONTEXT_INFO, identOptInfo);
                        ctx.next();
                    })
                    .onFailure(e -> error(StandardCode.INTERNAL_SERVER_ERROR, GatewayIdentHandler.class, "缓存服务错误", ctx, e));
            return;
        }
        // fetch ak/sk
        if (!ctx.request().headers().contains(security.getAkSkDateFieldName())
                || ctx.request().headers().get(security.getAkSkDateFieldName()).trim().isBlank()) {
            error(StandardCode.BAD_REQUEST, GatewayIdentHandler.class,
                    "请求格式不合法，HTTP Header[" + security.getAkSkDateFieldName() + "]不存在", ctx);
            return;
        }
        if (!authorization.contains(":")
                || authorization.split(":").length != 2) {
            error(StandardCode.BAD_REQUEST, GatewayIdentHandler.class,
                    "请求格式不合法，HTTP Header[" + security.getAkSkFieldName() + "]格式错误", ctx);
            return;
        }
        var ak = authorization.split(":")[0];
        var reqSignature = authorization.split(":")[1];
        var reqMethod = ctx.request().method();
        var reqDate = ctx.request().getHeader(security.getAkSkDateFieldName());
        var sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (sdf.parse(reqDate).getTime() + security.getAppRequestDateOffsetMs() < System.currentTimeMillis()) {
            error(StandardCode.UNAUTHORIZED, GatewayIdentHandler.class, "认证错误，请求时间已过期", ctx);
            return;
        }
        var reqPath = ctx.request().path();
        var reqQuery = ctx.request().query() != null ? ctx.request().query() : "";
        var sortedReqQuery = Arrays.stream(reqQuery.split("&")).sorted(String::compareTo).collect(Collectors.joining("&"));
        FunCacheClient.choose(getModuleName()).get(DewAuthConstant.CACHE_APP_AK + ak, security.getTokenCacheExpireSec())
                .onSuccess(legalSkAndAppId -> {
                    if (legalSkAndAppId == null) {
                        error(StandardCode.UNAUTHORIZED, GatewayIdentHandler.class, "认证错误，AK不存在", ctx);
                        return;
                    }
                    var skAndAppIdSplit = legalSkAndAppId.split(":");
                    var sk = skAndAppIdSplit[0];
                    var tenantId = Long.parseLong(skAndAppIdSplit[1]);
                    var appId = Long.parseLong(skAndAppIdSplit[2]);
                    var calcSignature = $.security.encodeStringToBase64(
                            $.security.digest.digest((reqMethod + "\n" + reqDate + "\n" + reqPath + "\n" + sortedReqQuery).toLowerCase(),
                                    sk, "HmacSHA1"),
                            StandardCharsets.UTF_8);
                    if (!reqSignature.equalsIgnoreCase(calcSignature)) {
                        error(StandardCode.UNAUTHORIZED, GatewayIdentHandler.class, "认证错误，签名不合法", ctx);
                        return;
                    }
                    var identOptInfo = new IdentOptCacheInfo();
                    identOptInfo.setAppId(appId);
                    identOptInfo.setTenantId(tenantId);
                    ctx.put(CONTEXT_INFO, identOptInfo);
                    ctx.next();
                })
                .onFailure(e -> error(StandardCode.INTERNAL_SERVER_ERROR, GatewayIdentHandler.class, "缓存服务错误", ctx, e));
    }

    private String getIP(HttpServerRequest request) {
        for (Map.Entry<String, String> header : request.headers()) {
            switch (header.getKey().toLowerCase()) {
                case "x-forwarded-for":
                case "wl-proxy-client-ip":
                case "x-forwarded-host":
                    if (header.getValue() != null && !header.getValue().isBlank()) {
                        return header.getValue();
                    }
                    break;
            }
        }
        return request.remoteAddress().host();
    }
}
