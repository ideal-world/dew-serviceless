package idealworld.dew.serviceless.gateway;

import idealworld.dew.serviceless.common.CommonApplication;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.funs.httpserver.HttpServer;
import idealworld.dew.serviceless.gateway.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.gateway.process.AuthHandler;
import idealworld.dew.serviceless.gateway.process.DistributeHandler;
import idealworld.dew.serviceless.gateway.process.GatewayAuthPolicy;
import idealworld.dew.serviceless.gateway.process.IdentHandler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class GatewayApplication extends CommonApplication<GatewayConfig> {

    @Override
    protected void doStart(GatewayConfig config, Promise<Void> startPromise) {
        initRedis(config);
        initHttpClient(config);
        var authPolicy = new GatewayAuthPolicy(config.getSecurity().getResourceCacheExpireSec(), config.getSecurity().getGroupNodeLength());
        ExchangeProcessor.init();
        var identHttpHandler = new IdentHandler(config.getSecurity());
        var authHttpHandler = new AuthHandler(authPolicy);
        var distributeHandler = new DistributeHandler(config.getDistribute());
        initHttpServer(config, HttpServer.Route.builder()
                .method(HttpMethod.POST)
                .path(Constant.REQUEST_PATH_FLAG)
                .parseBody(true)
                .handlers(Arrays.asList(identHttpHandler, authHttpHandler, distributeHandler))
                .build())
                .onSuccess(resp -> startPromise.complete())
                .onFailure(e -> startPromise.fail(e.getCause()));
    }

}
