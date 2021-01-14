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

package idealworld.dew.framework.fun.httpserver;

import com.ecfront.dew.common.exception.RTIOException;
import idealworld.dew.framework.DewConfig;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
@Slf4j
public class FunHttpServer {

    private static final Map<String, FunHttpServer> HTTP_SERVER = new ConcurrentHashMap<>();
    private String code;
    private HttpServer httpServer;
    private Vertx vertx;
    private DewConfig.FunConfig.HttpServerConfig httpServerConfig;

    public static Future<Void> init(String code, Vertx vertx, DewConfig.FunConfig.HttpServerConfig httpServerConfig) {
        var httpServer = new FunHttpServer();
        httpServer.code = code;
        httpServer.vertx = vertx;
        httpServer.httpServerConfig = httpServerConfig;
        httpServer.httpServer = vertx.createHttpServer();
        HTTP_SERVER.put(code, httpServer);
        return Future.succeededFuture();
    }

    public static CompositeFuture destroy() {
        return CompositeFuture.all(HTTP_SERVER.values().stream()
                .map(server -> server.httpServer.close())
                .collect(Collectors.toList()));
    }

    public static FunHttpServer choose(String code) {
        return HTTP_SERVER.get(code);
    }

    public static Boolean contains(String code) {
        return HTTP_SERVER.containsKey(code);
    }

    public static void remove(String code) {
        HTTP_SERVER.remove(code);
    }

    public void addRoute(Route route) {
        addRoutes(new ArrayList<>() {
            {
                add(route);
            }
        });
    }

    public void addRoutes(List<Route> routes) {
        var router = Router.router(vertx);
        var corsHandler = CorsHandler.create(httpServerConfig.getAllowedOriginPattern());
        corsHandler.allowedHeader(httpServerConfig.getAllowedHeaders());
        corsHandler.allowedMethod(HttpMethod.OPTIONS);
        routes
                .forEach(route -> {
                    var currentRoute = router.route(route.getMethod(), route.getPath());
                    currentRoute.produces(route.getProduces());
                    if (route.getMethod() == HttpMethod.POST || route.getMethod() == HttpMethod.PUT) {
                        currentRoute.handler(BodyHandler.create(false));
                    }
                    route.getHandlers().forEach(currentRoute::handler);
                    currentRoute.failureHandler(ctx -> {
                        int statusCode = ctx.statusCode();
                        log.warn("[HTTP][{}]Request error [{}] {}", code, ctx.statusCode(), ctx.failure().getMessage(), ctx.failure());
                        ctx.response().setStatusCode(statusCode).end("请求发生内部错误");
                    });
                    corsHandler.allowedMethod(route.getMethod());
                });
        router.route().handler(corsHandler);
        httpServer.requestHandler(router)
                .listen(httpServerConfig.getPort(), httpResult -> {
                    if (httpResult.succeeded()) {
                        log.info("[HTTP][" + code + "]HTTP server started on port " + httpServerConfig.getPort());
                    } else {
                        log.error("[HTTP][" + code + "]HTTP server start failure", httpResult.cause());
                        throw new RTIOException("[HTTP][" + code + "]HTTP server start failure");
                    }
                });
    }


    @Data
    @Builder
    public static class Route {

        HttpMethod method;
        String path;
        @Builder.Default
        String produces = "application/json";
        List<KernelHttpHandler> handlers;

    }
}
