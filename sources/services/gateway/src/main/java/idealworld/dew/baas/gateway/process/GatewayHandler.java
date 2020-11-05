package idealworld.dew.baas.gateway.process;

import com.ecfront.dew.common.StandardCode;
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

    protected void error(StandardCode statusCode, String msg, RoutingContext ctx) {
        log.warn("[Process]Request error [{}]: {}", statusCode.toString(), msg);
        ctx.response().setStatusCode(Integer.parseInt(statusCode.toString())).end(msg);
    }

    protected void error(StandardCode statusCode, String msg, RoutingContext ctx, Throwable e) {
        log.warn("[Process]Request error [{}]{}", statusCode.toString(), e.getMessage(), e);
        ctx.response().setStatusCode(Integer.parseInt(statusCode.toString())).end(msg);
    }

}
