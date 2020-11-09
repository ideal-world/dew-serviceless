package idealworld.dew.baas.reldb;

import idealworld.dew.baas.common.CommonApplication;
import idealworld.dew.baas.common.funs.httpserver.HttpServer;
import idealworld.dew.baas.reldb.process.AuthHandler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

@Slf4j
public class RelDBApplication extends CommonApplication<RelDBConfig> {

    @Override
    protected void doStart(RelDBConfig config, Promise startPromise) {
        initRedis(config);
        initHttpClient(config);

        initHttpServer(config, HttpServer.Route.builder()
                .method(HttpMethod.POST)
                .path(config.getRequest().getPath())
                .parseBody(true)
                .handlers(Collections.singletonList(new AuthHandler(config.getRequest())))
                .build())
                .onSuccess(resp -> startPromise.complete())
                .onFailure(e -> startPromise.fail(e.getCause()));
    }

}
