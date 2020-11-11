package idealworld.dew.baas.reldb.process;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.StandardCode;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.dto.IdentOptCacheInfo;
import idealworld.dew.baas.common.funs.cache.RedisClient;
import idealworld.dew.baas.common.funs.httpserver.CommonHttpHandler;
import idealworld.dew.baas.common.funs.mysql.MysqlClient;
import idealworld.dew.baas.common.util.URIHelper;
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
    private final RelDBConfig.Security security;

    public AuthHandler(RelDBConfig.Request request, RelDBConfig.Security security) {
        this.request = request;
        this.security = security;
    }

    @SneakyThrows
    @Override
    public void handle(RoutingContext ctx) {
        if (!ctx.request().headers().contains(request.getIdentOptHeaderName())) {
            error(StandardCode.BAD_REQUEST, "请求格式不合法", ctx);
            return;
        }
        var sqlInfo = ctx.getBodyAsString().split("\\|");
        var sqlKey = sqlInfo[0];
        RedisClient.choose("").get(sqlKey, security.getSqlCacheExpireSec())
                .onSuccess(sql -> {
                    var sqlAst = SqlParser.parse(sql);
                    var strIdentOpt = ctx.request().getHeader(request.getIdentOptHeaderName());
                    var strResourceUri = ctx.request().getHeader(Constant.CONFIG_RESOURCE_URI_FLAG);
                    var strAction = ctx.request().getHeader(Constant.CONFIG_RESOURCE_ACTION_FLAG);
                    var resourceUri = URIHelper.newURI(strResourceUri);
                    var identOpt = $.json.toObject(strIdentOpt, IdentOptCacheInfo.class);
                    // TODO

                    var sqlParameters = $.json.toList(sqlInfo[1], Object.class);
                    MysqlClient.choose(resourceUri.getHost()).exec(sql, sqlParameters)
                            .onSuccess(result -> ctx.end(result.toBuffer()))
                            .onFailure(e -> error(StandardCode.BAD_REQUEST, "数据查询错误", ctx, e));
                })
                .onFailure(e -> error(StandardCode.BAD_REQUEST, "请求的SQL Key [" + sqlKey + "]不存在", ctx));


    }


}
