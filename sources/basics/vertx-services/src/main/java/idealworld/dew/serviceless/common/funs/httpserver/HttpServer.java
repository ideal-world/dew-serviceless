package idealworld.dew.serviceless.common.funs.httpserver;

import idealworld.dew.serviceless.common.CommonConfig;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
@Slf4j
public class HttpServer {

    public static Future<Void> init(Vertx vertx, CommonConfig.HttpServerConfig config, List<Route> routes) {
        Promise<Void> promise = Promise.promise();
        var router = Router.router(vertx);

        var corsHandler = CorsHandler.create("*");

        routes.stream()
                .map(route -> {
                    var currentRoute = router.route(route.getMethod(), route.getPath());
                    currentRoute.consumes("application/json")
                            .produces("application/json");
                    if (route.parseBody) {
                        currentRoute.handler(BodyHandler.create(false));
                    }
                    route.getHandlers().forEach(currentRoute::handler);
                    currentRoute.failureHandler(ctx -> {
                        int statusCode = ctx.statusCode();
                        log.warn("[Process]Request error [{}] {}", ctx.statusCode(), ctx.failure().getMessage(), ctx.failure());
                        ctx.response().setStatusCode(statusCode).end("请求发生内部错误");
                    });
                    return route.getMethod();
                })
                .collect(Collectors.toSet())
                .forEach(corsHandler::allowedMethod);

        router.route().handler(corsHandler);

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(config.getPort(), http -> {
                    if (http.succeeded()) {
                        promise.complete();
                        log.info("[Startup]HTTP server started on port " + config.getPort());
                    } else {
                        promise.fail(http.cause());
                    }
                });
        return promise.future();
    }


    @Data
    @Builder
    public static class Route {

        HttpMethod method;
        String path;
        Boolean parseBody;
        List<CommonHttpHandler> handlers;

    }
}
