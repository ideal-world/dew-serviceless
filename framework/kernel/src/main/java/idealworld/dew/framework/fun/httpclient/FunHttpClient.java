/*
 * Copyright 2021. gudaoxuri
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

package idealworld.dew.framework.fun.httpclient;

import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.util.URIHelper;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author gudaoxuri
 */
@Slf4j
public class FunHttpClient {

    private static final Map<String, FunHttpClient> HTTP_CLIENTS = new ConcurrentHashMap<>();
    private WebClient webClient;
    private DewConfig.FunConfig.HttpClientConfig httpClientConfig;

    public static Future<Void> init(String code, Vertx vertx, DewConfig.FunConfig.HttpClientConfig httpClientConfig) {
        var httpClient = new FunHttpClient();
        httpClient.webClient = WebClient.create(vertx);
        httpClient.httpClientConfig = httpClientConfig;
        HTTP_CLIENTS.put(code, httpClient);
        return Future.succeededFuture();
    }

    public static Future<Void> destroy() {
        HTTP_CLIENTS.forEach((key, value) -> {
            value.webClient.close();
        });
        return Future.succeededFuture();
    }

    public static FunHttpClient choose(String code) {
        return HTTP_CLIENTS.get(code);
    }

    public static Boolean contains(String code) {
        return HTTP_CLIENTS.containsKey(code);
    }

    public static void remove(String code) {
        HTTP_CLIENTS.remove(code);
    }

    public Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String url) {
        return request(httpMethod, url, null, null, null);
    }

    public Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String url, Buffer body) {
        return request(httpMethod, url, body, null, null);
    }

    public Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String url, Buffer body, Map<String, String> header) {
        return request(httpMethod, url, body, header, null);
    }

    public Future<HttpResponse<Buffer>> requestByResourceSubject(HttpMethod httpMethod, String pathAndQuery, Buffer body, Map<String, String> header) {
        return request(httpMethod, URIHelper.formatUri(httpClientConfig.getUri(), pathAndQuery), body, header, httpClientConfig.getTimeoutMs());
    }

    public Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String url, Buffer body, Map<String, String> header, Long timeoutMs) {
        var request = webClient.requestAbs(httpMethod, url);
        if (timeoutMs != null) {
            request.timeout(timeoutMs);
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

    public Future<HttpResponse<Buffer>> request(HttpMethod httpMethod, String host, Integer port, String path, String query,
                                                Buffer body, Map<String, String> header, Long timeoutMs) {
        var request = webClient.request(httpMethod, port, host, path);
        if (timeoutMs != null) {
            request.timeout(timeoutMs);
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

}
