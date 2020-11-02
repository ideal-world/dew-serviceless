package idealworld.dew.baas.gateway.process;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public abstract class GatewayHandler implements Handler<RoutingContext> {

    protected static final String CONTEXT_INFO = "CONTEXT";

    protected void error(int statusCode, String msg, RoutingContext ctx) {
        log.warn("Request error [{}]: {}", statusCode, msg);
        ctx.response().setStatusCode(statusCode).end(msg);
    }

    protected void error(int statusCode, String msg, RoutingContext ctx, Throwable e) {
        log.warn("Request error [{}]: {}", statusCode, msg, e);
        ctx.response().setStatusCode(statusCode).end(msg);
    }

}
