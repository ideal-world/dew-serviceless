package idealworld.dew.baas.reldb.process;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.StandardCode;
import idealworld.dew.baas.common.dto.IdentOptCacheInfo;
import idealworld.dew.baas.common.funs.httpserver.CommonHttpHandler;
import idealworld.dew.baas.reldb.RelDBConfig;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 鉴权处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public class AuthHandler extends CommonHttpHandler {

    private final RelDBConfig.Request request;

    public AuthHandler(RelDBConfig.Request request) {
        this.request = request;
    }

    @SneakyThrows
    @Override
    public void handle(RoutingContext ctx) {
        if (!ctx.request().headers().contains(request.getIdentOptHeaderName())) {
            error(StandardCode.BAD_REQUEST, "请求格式不合法", ctx);
            return;
        }
        var strIdentOpt = ctx.request().getHeader(request.getIdentOptHeaderName());
        var identOpt = $.json.toObject(strIdentOpt, IdentOptCacheInfo.class);
        var sql = ctx.getBodyAsString();
    }


}
