package idealworld.dew.baas.gateway;

import idealworld.dew.baas.common.CommonApplication;
import idealworld.dew.baas.common.funs.httpserver.HttpServer;
import idealworld.dew.baas.gateway.exchange.ExchangeProcessor;
import idealworld.dew.baas.gateway.process.AuthHandler;
import idealworld.dew.baas.gateway.process.DistributeHandler;
import idealworld.dew.baas.gateway.process.IdentHandler;
import idealworld.dew.baas.gateway.process.ReadonlyAuthPolicy;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class GatewayApplication extends CommonApplication<GatewayConfig> {

    @Override
    protected void doStart(GatewayConfig config, Promise startPromise) {
        initRedis(config);
        initHttpClient(config);
        var authPolicy = new ReadonlyAuthPolicy(config.getSecurity().getResourceCacheExpireSec(), config.getSecurity().getGroupNodeLength());
        ExchangeProcessor.init(authPolicy);
        var identHttpHandler = new IdentHandler(config.getRequest(), config.getSecurity());
        var authHttpHandler = new AuthHandler(config.getRequest(), authPolicy);
        var distributeHandler = new DistributeHandler(config.getRequest(), config.getDistribute());
        initHttpServer(config, HttpServer.Route.builder()
                .method(HttpMethod.POST)
                .path(config.getRequest().getPath())
                .parseBody(true)
                .handlers(Arrays.asList(identHttpHandler, authHttpHandler, distributeHandler))
                .build())
                .onSuccess(resp -> startPromise.complete())
                .onFailure(e -> startPromise.fail(e.getCause()));
    }

}
