package idealworld.dew.baas.cache;

import idealworld.dew.baas.cache.exchange.ExchangeProcessor;
import idealworld.dew.baas.cache.process.CacheHandler;
import idealworld.dew.baas.common.CommonApplication;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.funs.httpserver.HttpServer;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class CacheApplication extends CommonApplication<CacheConfig> {

    @Override
    protected void doStart(CacheConfig config, Promise<Void> startPromise) {
        initRedis(config);
        initHttpClient(config);
        var cacheHandler = new CacheHandler();
        ExchangeProcessor.init();
        initHttpServer(config, HttpServer.Route.builder()
                .method(HttpMethod.POST)
                .path(Constant.REQUEST_PATH_FLAG)
                .parseBody(true)
                .handlers(Collections.singletonList(cacheHandler))
                .build())
                .onSuccess(resp -> startPromise.complete())
                .onFailure(e -> startPromise.fail(e.getCause()));
    }

}
