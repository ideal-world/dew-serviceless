package idealworld.dew.baas.gateway;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.gateway.util.YamlHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class GatewayApplication extends AbstractVerticle {

    private static final String PROFILE_KEY = "dew.profile";

    @Override
    public void start(Promise<Void> startPromise) throws Exception {
        var config = loadConfig();
        var httpClient  = initHttpClient();
        var redisClient = initRedisClient(config.getRedis());
        var httpHandler = new HttpHandler(httpClient,RedisAPI.api(redisClient));
        startServer(startPromise, config.getPort(), httpHandler);
    }

    private GatewayConfig loadConfig() {
        var strConfig = $.file.readAllByClassPath("application-" + System.getProperty(PROFILE_KEY), StandardCharsets.UTF_8);
        return YamlHelper.toObject(GatewayConfig.class, strConfig);
    }

    private Redis initRedisClient(GatewayConfig.RedisConfig config) {
        return Redis.createClient(
                vertx,
                new RedisOptions()
                        .setConnectionString(config.getUri())
                        .setMaxPoolSize(config.getMaxPoolSize())
                        .setMaxWaitingHandlers(config.getMaxPoolWaiting()));
    }

    private WebClient initHttpClient() {
        return WebClient.create(vertx);
    }

    private void startServer(Promise<Void> startPromise, Integer port, HttpHandler httpHandler) {
        var router = Router.router(vertx);
        router.route()
                .handler(CorsHandler.create("*")
                        .allowedMethod(HttpMethod.OPTIONS)
                        .allowedMethod(HttpMethod.POST));
        router.route(HttpMethod.POST, "/execute")
                .consumes("application/json")
                .produces("application/json")
                .handler(httpHandler)
                .failureHandler(ctx -> {
                    int statusCode = ctx.statusCode();
                    log.warn("Request error {}", ctx.statusCode(), ctx.failure());
                    ctx.response().setStatusCode(statusCode).end("");
                });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, http -> {
                    if (http.succeeded()) {
                        startPromise.complete();
                        log.info("HTTP server started on port " + port);
                    } else {
                        startPromise.fail(http.cause());
                    }
                });
    }


}
