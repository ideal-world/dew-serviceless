package idealworld.dew.baas.gateway;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;

/**
 * HTTP 处理器
 *
 * @author gudaoxuri
 */
public class HttpHandler implements Handler<RoutingContext> {

    private WebClient webClient;
    private RedisAPI redisClient;

    public HttpHandler(WebClient webClient, RedisAPI redisClient) {
        this.webClient = webClient;
        this.redisClient = redisClient;
    }

    @Override
    public void handle(RoutingContext ctx) {
        /*ctx -> {

            // This handler will be called for every request
            HttpServerResponse response = ctx.response();
            response.putHeader("content-type", "text/plain");

            // Write to the response and end it
            response.end("Hello World from Vert.x-Web!");
        }*/
        //response.end("Hello World from Vert.x-Web!");
    }
}
