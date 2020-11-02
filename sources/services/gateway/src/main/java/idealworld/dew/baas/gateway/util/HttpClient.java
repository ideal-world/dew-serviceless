package idealworld.dew.baas.gateway.util;

import idealworld.dew.baas.common.util.URIHelper;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;

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

    public static Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String url, Buffer body, Map<String, String> header, Integer timeout) {
        var request = webClient.requestAbs(httpMethod, url)
                .timeout(timeout);
        if (header != null) {
            MultiMap headerMap = MultiMap.caseInsensitiveMultiMap();
            headerMap.addAll(header);
            request.putHeaders(headerMap);
        }
        if (body != null) {
            return request.sendBuffer(body);
        }
        return request.send();
    }

    public static Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String host, Integer port, String path, String query,
                                                       Buffer body, Map<String, String> header, Integer timeout) {
        var request = webClient.request(httpMethod, port, host, path)
                .timeout(timeout);
        if (query != null) {
            URIHelper.getSingleValueQuery(query)
                    .forEach(request::addQueryParam);
        }
        if (header != null) {
            MultiMap headerMap = MultiMap.caseInsensitiveMultiMap();
            headerMap.addAll(header);
            request.putHeaders(headerMap);
        }
        if (body != null) {
            return request.sendBuffer(body);
        }
        return request.send();
    }

}
