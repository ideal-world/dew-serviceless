package idealworld.dew.baas.gateway.util;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @author gudaoxuri
 */
@Slf4j
public class HttpClient {

    private static WebClient webClient;

    public static void init(Vertx vertx) {
        webClient = WebClient.create(vertx);
    }

}
