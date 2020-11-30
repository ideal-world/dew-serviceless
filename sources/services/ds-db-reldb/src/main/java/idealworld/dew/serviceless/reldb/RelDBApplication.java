package idealworld.dew.serviceless.reldb;

import idealworld.dew.serviceless.common.CommonApplication;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.funs.httpserver.HttpServer;
import idealworld.dew.serviceless.reldb.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.reldb.process.AuthHandler;
import idealworld.dew.serviceless.reldb.process.RelDBAuthPolicy;
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
