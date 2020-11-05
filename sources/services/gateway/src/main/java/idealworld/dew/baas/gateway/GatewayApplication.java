package idealworld.dew.baas.gateway;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.exception.RTIOException;
import idealworld.dew.baas.gateway.exchange.ExchangeProcessor;
import idealworld.dew.baas.gateway.process.AuthHandler;
import idealworld.dew.baas.gateway.process.DistributeHandler;
import idealworld.dew.baas.gateway.process.IdentHandler;
import idealworld.dew.baas.gateway.process.ReadonlyAuthPolicy;
import idealworld.dew.baas.gateway.util.HttpClient;
import idealworld.dew.baas.gateway.util.RedisClient;
import idealworld.dew.baas.gateway.util.YamlHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class GatewayApplication extends AbstractVerticle {

    private static final String PROFILE_KEY = "dew.profile";

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new GatewayApplication());
    }

    @Override
    public void start(Promise<Void> startPromise) {
        var config = loadConfig();
        RedisClient.init(vertx, config.getRedis());
        HttpClient.init(vertx);
        var authPolicy = new ReadonlyAuthPolicy(config.getSecurity().getResourceCacheExpireSec(), config.getSecurity().getGroupNodeLength());
        ExchangeProcessor.register(config.getExchange(), authPolicy);
        var identHttpHandler = new IdentHandler(config.getRequest(), config.getSecurity());
        var authHttpHandler = new AuthHandler(config.getRequest(), authPolicy);
        var distributeHandler = new DistributeHandler(config.getRequest(), config.getDistribute());
        startServer(startPromise, config.getRequest(), identHttpHandler, authHttpHandler, distributeHandler);
    }

    private GatewayConfig loadConfig() {
        String config;
        try {
            config = $.file.readAllByClassPath("application-" + System.getProperty(PROFILE_KEY) + ".yml", StandardCharsets.UTF_8);
        } catch (RTIOException ignore) {
            try {
                config = $.file.readAllByClassPath("application-" + System.getProperty(PROFILE_KEY) + ".yaml", StandardCharsets.UTF_8);
            } catch (RTIOException e) {
                log.error("[Startup]Configuration file [{}] not found in classpath", "application-" + System.getProperty(PROFILE_KEY) + ".yml/yaml");
                throw e;
            }
        }
        return YamlHelper.toObject(GatewayConfig.class, config);
    }

    private void startServer(Promise<Void> startPromise, GatewayConfig.Request request,
                             IdentHandler identHandler, AuthHandler authHandler, DistributeHandler distributeHandler) {
        var router = Router.router(vertx);
        router.route()
                .handler(CorsHandler.create("*")
                        .allowedMethod(HttpMethod.OPTIONS)
                        .allowedMethod(HttpMethod.POST));
        router.route(HttpMethod.POST, request.getPath())
                .consumes("application/json")
                .produces("application/json")
                .handler(BodyHandler.create(false))
                .handler(identHandler)
                .handler(authHandler)
                .handler(distributeHandler)
                .failureHandler(ctx -> {
                    int statusCode = ctx.statusCode();
                    log.warn("[Process]Request error [{}] {}", ctx.statusCode(), ctx.failure().getMessage(), ctx.failure());
                    ctx.response().setStatusCode(statusCode).end("请求发生内部错误");
                });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(request.getPort(), http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        log.info("[Startup]HTTP server started on port " + request.getPort());
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }


}
