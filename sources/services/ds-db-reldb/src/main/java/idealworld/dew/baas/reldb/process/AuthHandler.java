package idealworld.dew.baas.reldb.process;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.StandardCode;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.dto.IdentOptCacheInfo;
import idealworld.dew.baas.common.enumeration.AuthResultKind;
import idealworld.dew.baas.common.funs.cache.RedisClient;
import idealworld.dew.baas.common.funs.httpserver.CommonHttpHandler;
import idealworld.dew.baas.common.funs.mysql.MysqlClient;
import idealworld.dew.baas.common.util.URIHelper;
import idealworld.dew.baas.reldb.RelDBConfig;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 鉴权处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public class AuthHandler extends CommonHttpHandler {

    private final RelDBConfig.Request request;
    private final RelDBConfig.Security security;
    private final RelDBAuthPolicy authPolicy;

    public AuthHandler(RelDBConfig.Request request, RelDBConfig.Security security, RelDBAuthPolicy authPolicy) {
        this.request = request;
        this.security = security;
        this.authPolicy = authPolicy;
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
                    List<SqlParser.SqlAst> sqlAsts;
                    try {
                        sqlAsts = SqlParser.parse(sql);
                        if (sqlAsts == null) {
                            error(StandardCode.BAD_REQUEST, "请求的SQL解析错误", ctx);
                            return;
                        }
                    } catch (Exception e) {
                        error(StandardCode.BAD_REQUEST, "请求的SQL解析错误", ctx, e);
                        return;

                    }
                    var strResourceUriWithoutPath = ctx.request().getHeader(Constant.CONFIG_RESOURCE_URI_FLAG);
                    var resources = sqlAsts.stream()
                            .flatMap(sqlAst -> {
                                var actionKind = sqlAst.getActionKind().toString().toLowerCase();
                                var fields = sqlAst.getCondFields();
                                fields.addAll(sqlAst.getEffectFields());
                                fields.addAll(sqlAst.getReturnFields());
                                return fields.stream().map(field ->
                                        new Tuple2<>(URIHelper.newURI(strResourceUriWithoutPath + "/" + sqlAst.getTable() + "/" + field), actionKind)
                                );
                            })
                            .collect(Collectors.groupingBy(item -> item._1, Collectors.mapping(item -> item._0, Collectors.toList())));
                    var strIdentOpt = ctx.request().getHeader(request.getIdentOptHeaderName());
                    var identOpt = $.json.toObject(strIdentOpt, IdentOptCacheInfo.class);
                    var subjectInfo = packageSubjectInfo(identOpt);
                    authPolicy.authentication(resources, subjectInfo)
                            .onSuccess(authResultKind -> {
                                if (authResultKind == AuthResultKind.REJECT) {
                                    error(StandardCode.UNAUTHORIZED, "鉴权错误，没有权限访问对应的资源", ctx);
                                    return;
                                }
                                var sqlParameters = $.json.toList(sqlInfo[1], Object.class);
                                MysqlClient.choose(URIHelper.newURI(strResourceUriWithoutPath).getHost()).exec(sql, sqlParameters)
                                        .onSuccess(result -> ctx.end(result.toBuffer()))
                                        .onFailure(e -> error(StandardCode.BAD_REQUEST, "数据查询错误", ctx, e));
                            })
                            .onFailure(e -> error(StandardCode.INTERNAL_SERVER_ERROR, "服务错误", ctx, e));
                })
                .onFailure(e -> error(StandardCode.BAD_REQUEST, "请求的SQL Key [" + sqlKey + "]不存在", ctx));


    }


}