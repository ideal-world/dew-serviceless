package idealworld.dew.baas.reldb;

import idealworld.dew.baas.common.CommonApplication;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.funs.httpserver.HttpServer;
import idealworld.dew.baas.reldb.exchange.ExchangeProcessor;
import idealworld.dew.baas.reldb.process.AuthHandler;
import idealworld.dew.baas.reldb.process.RelDBAuthPolicy;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class RelDBApplication extends CommonApplication<RelDBConfig> {

    @Override
    protected void doStart(RelDBConfig config, Promise<Void> startPromise) {
        initRedis(config);
        initHttpClient(config);
        var authPolicy = new RelDBAuthPolicy(config.getSecurity().getResourceCacheExpireSec(), config.getSecurity().getGroupNodeLength());
        ExchangeProcessor.init(config);
        var authHttpHandler = new AuthHandler(config.getSecurity(), authPolicy);
        initHttpServer(config, HttpServer.Route.builder()
                .method(HttpMethod.POST)
                .path(Constant.REQUEST_PATH_FLAG)
                .parseBody(true)
                .handlers(Collections.singletonList(authHttpHandler))
                .build())
                .onSuccess(resp -> startPromise.complete())
                .onFailure(e -> startPromise.fail(e.getCause()));
    }

}
