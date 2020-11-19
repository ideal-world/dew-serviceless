package idealworld.dew.baas.common.funs.httpclient;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.dto.IdentOptCacheInfo;
import idealworld.dew.baas.common.util.URIHelper;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gudaoxuri
 */
@Slf4j
public class HttpClient {

    private static WebClient webClient;

    public static void init(Vertx vertx) {
        webClient = WebClient.create(vertx);
    }

    public static Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String url) {
        return request(httpMethod, url, null, null, null);
    }

    public static Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String url, Buffer body) {
        return request(httpMethod, url, body, null, null);
    }

    public static Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String url, Buffer body, Map<String, String> header) {
        return request(httpMethod, url, body, header, null);
    }

    public static Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String url, Buffer body, Map<String, String> header, Integer timeout) {
        var request = webClient.requestAbs(httpMethod, url);
        if (timeout != null) {
            request.timeout(timeout);
        }
        if (header == null) {
            header = new HashMap<>();
        }
        if (!header.containsKey("Content-Type")) {
            header.put("Content-Type", "application/json; charset=utf-8");
        }
        MultiMap headerMap = MultiMap.caseInsensitiveMultiMap();
        headerMap.addAll(header);
        request.putHeaders(headerMap);
        if (body != null) {
            return request.sendBuffer(body);
        }
        return request.send();
    }

    public static Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String host, Integer port, String path, String query,
                                                       Buffer body, Map<String, String> header, Integer timeout) {
        var request = webClient.request(httpMethod, port, host, path);
        if (timeout != null) {
            request.timeout(timeout);
        }
        if (query != null) {
            URIHelper.getSingleValueQuery(query)
                    .forEach(request::addQueryParam);
        }
        if (header == null) {
            header = new HashMap<>();
        }
        if (!header.containsKey("Content-Type")) {
            header.put("Content-Type", "application/json; charset=utf-8");
        }
        MultiMap headerMap = MultiMap.caseInsensitiveMultiMap();
        headerMap.addAll(header);
        request.putHeaders(headerMap);
        if (body != null) {
            return request.sendBuffer(body);
        }
        return request.send();
    }

    public static Map<String, String> getIdentOptHeader(Long appId, Long tenantId) {
        var identOptInfo = IdentOptCacheInfo.builder()
                .tenantId(tenantId)
                .appId(appId)
                .build();
        return getIdentOptHeader(identOptInfo);
    }

    public static Map<String, String> getIdentOptHeader(IdentOptCacheInfo identOptCacheInfo) {
        return new HashMap<>() {
            {
                put(Constant.REQUEST_IDENT_OPT_FLAG, $.security.encodeStringToBase64($.json.toJsonString(identOptCacheInfo), StandardCharsets.UTF_8));
            }
        };
    }

}
