package idealworld.dew.serviceless.common;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.exception.RTIOException;
import idealworld.dew.serviceless.common.funs.cache.RedisClient;
import idealworld.dew.serviceless.common.funs.httpclient.HttpClient;
import idealworld.dew.serviceless.common.funs.httpserver.HttpServer;
import idealworld.dew.serviceless.common.util.YamlHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
public abstract class CommonApplication<C extends CommonConfig> extends AbstractVerticle {

    public static Vertx VERTX;
    private static final String PROFILE_KEY = "dew.profile";

    private Class<C> configClazz = (Class<C>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    protected abstract void doStart(C config, Promise<Void> startPromise);

    protected void initRedis(C config) {
        RedisClient.init("", vertx, config.getRedis());
    }

    protected void initHttpClient(C config) {
        HttpClient.init(vertx);
    }

    protected Future<Void> initHttpServer(C config, HttpServer.Route... routes) {
        return HttpServer.init(vertx, config.getHttpServer(), Arrays.asList(routes));
    }

    @Override
    final public void start(Promise<Void> startPromise) {
        VERTX = vertx;
        var config = loadConfig();
        doStart(config, startPromise);
    }

    private C loadConfig() {
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
        return YamlHelper.toObject(configClazz, config);
    }

}
