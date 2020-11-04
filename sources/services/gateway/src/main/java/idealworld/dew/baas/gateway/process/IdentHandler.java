package idealworld.dew.baas.gateway.process;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.StandardCode;
import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.baas.common.enumeration.AuthActionKind;
import idealworld.dew.baas.common.enumeration.ResourceKind;
import idealworld.dew.baas.gateway.GatewayConfig;
import idealworld.dew.baas.gateway.util.RedisClient;
import io.vertx.core.http.HttpServerRequest;
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
public class IdentHandler extends GatewayHandler {

    private final GatewayConfig.Request request;
    private final GatewayConfig.Security security;

    public IdentHandler(GatewayConfig.Request request, GatewayConfig.Security security) {
        this.request = request;
        this.security = security;
    }

    @SneakyThrows
    @Override
    public void handle(RoutingContext ctx) {
        log.trace("[{}] {}{} from {}",
                ctx.request().method(),
                ctx.request().path(),
                ctx.request().query() == null ? "" : "?" + ctx.request().query(), getIP(ctx.request()));
        // checker
        if (ctx.request().query() == null || ctx.request().query().trim().isBlank()) {
            error(Integer.parseInt(StandardCode.BAD_REQUEST.toString()), "请求格式不合法，缺少query", ctx);
            return;
        }
        var queryMap = Arrays.stream(URLDecoder.decode(ctx.request().query().trim(), StandardCharsets.UTF_8).split("&"))
                .map(item -> item.split("="))
                .collect(Collectors.toMap(item -> item[0], item -> item.length > 1 ? item[1] : ""));
        if (!queryMap.containsKey(request.getResourceUriKey())
                || !queryMap.containsKey(request.getActionKey())) {
            error(Integer.parseInt(StandardCode.BAD_REQUEST.toString()), "请求格式不合法，缺少[" + request.getResourceUriKey() + "]或[" + request.getActionKey() + "]", ctx);
            return;
        }
        URI resourceUri;
        AuthActionKind actionKind;
        try {
            resourceUri = new URI(queryMap.get(request.getResourceUriKey()));
            ResourceKind.parse(resourceUri.getScheme().toLowerCase());
            actionKind = AuthActionKind.parse(queryMap.get(request.getActionKey()).toLowerCase());
        } catch (RTException e) {
            error(Integer.parseInt(StandardCode.BAD_REQUEST.toString()), "请求格式不合法，资源类型或操作类型不存在", ctx);
            return;
        }
        ctx.put(request.getResourceUriKey(), resourceUri);
        ctx.put(request.getActionKey(), actionKind);

        var token = ctx.request().headers().contains(security.getTokenFieldName())
                ? ctx.request().getHeader(security.getTokenFieldName()) : null;
        var authorization = ctx.request().headers().contains(security.getAkSkFieldName())
                ? ctx.request().getHeader(security.getAkSkFieldName()) : null;
        if (token == null || authorization == null) {
            ctx.put(CONTEXT_INFO, null);
            ctx.next();
        }

        // fetch token
        if (token != null) {
            RedisClient.get(security.getCacheTokenInfoKey() + token, security.getTokenCacheExpireSec())
                    .onSuccess(optInfo -> {
                        var identOptInfo = optInfo != null
                                ? $.json.toObject(optInfo, IdentOptCacheInfo.class)
                                : null;
                        if (optInfo == null) {
                            error(Integer.parseInt(StandardCode.UNAUTHORIZED.toString()), "Token不合法", ctx);
                            return;
                        }
                        ctx.put(CONTEXT_INFO, identOptInfo);
                        ctx.next();
                    });
        }
        // fetch ak/sk
        if (!authorization.contains(":")
                || authorization.split(":").length != 2) {
            error(Integer.parseInt(StandardCode.UNAUTHORIZED.toString()),
                    "认证错误，请检查 HTTP Header [" + security.getTokenFieldName() + "] 格式是否正确", ctx);
            return;
        }
        var ak = authorization.split(":")[0];
        var reqSignature = authorization.split(":")[1];
        var reqMethod = ctx.request().method();
        var reqDate = ctx.request().getHeader("Dew-Date");
        var sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        if (sdf.parse(reqDate).getTime() + security.getAppRequestDateOffsetMs() < System.currentTimeMillis()) {
            error(Integer.parseInt(StandardCode.UNAUTHORIZED.toString()), "请求时间已过期", ctx);
            return;
        }
        var reqPath = ctx.request().path();
        var reqQuery = ctx.request().query() != null ? ctx.request().query() : "";
        RedisClient.get(security.getCacheAkSkInfoKey() + ak, security.getTokenCacheExpireSec())
                .onSuccess(legalSkAndAppId -> {
                    if (legalSkAndAppId == null) {
                        error(Integer.parseInt(StandardCode.UNAUTHORIZED.toString()), "认证错误，请检查 HTTP Header [" + security.getAkSkFieldName() + "] 格式是否正确", ctx);
                        return;
                    }
                    var skAndAppIdSplit = legalSkAndAppId.split(":");
                    var sk = skAndAppIdSplit[0];
                    var tenantId = Long.parseLong(skAndAppIdSplit[1]);
                    var appId = Long.parseLong(skAndAppIdSplit[2]);
                    var calcSignature = $.security.encodeStringToBase64(
                            $.security.digest.digest((reqMethod + "\n" + reqDate + "\n" + reqPath + "\n" + reqQuery).toLowerCase(),
                                    sk, "HmacSHA1"),
                            StandardCharsets.UTF_8);
                    if (!reqSignature.equalsIgnoreCase(calcSignature)) {
                        error(Integer.parseInt(StandardCode.UNAUTHORIZED.toString()), "认证错误，请检查签名是否合法", ctx);
                        return;
                    }
                    var identOptInfo = new IdentOptCacheInfo();
                    identOptInfo.setAppId(appId);
                    identOptInfo.setTenantId(tenantId);
                    ctx.put(CONTEXT_INFO, identOptInfo);
                    ctx.next();
                });
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
