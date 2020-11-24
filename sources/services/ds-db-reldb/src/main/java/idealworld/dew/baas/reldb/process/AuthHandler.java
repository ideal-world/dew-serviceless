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
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 鉴权处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public class AuthHandler extends CommonHttpHandler {

    private final RelDBConfig.Security security;
    private final RelDBAuthPolicy authPolicy;

    public AuthHandler(RelDBConfig.Security security, RelDBAuthPolicy authPolicy) {
        this.security = security;
        this.authPolicy = authPolicy;
    }

    @Override
    public void handle(RoutingContext ctx) {
        if (!ctx.request().headers().contains(Constant.REQUEST_IDENT_OPT_FLAG)
                || !ctx.request().headers().contains(Constant.REQUEST_RESOURCE_URI_FLAG)) {
            error(StandardCode.BAD_REQUEST, "请求格式不合法", ctx);
            return;
        }
        var strResourceUriWithoutPath = ctx.request().getHeader(Constant.REQUEST_RESOURCE_URI_FLAG);
        var resourceSubjectCode = URIHelper.newURI(strResourceUriWithoutPath).getHost();
        if (!MysqlClient.contains(resourceSubjectCode)) {
            error(StandardCode.BAD_REQUEST, "请求的资源主题不存在", ctx);
            return;
        }
        var strIdentOpt = $.security.decodeBase64ToString(ctx.request().getHeader(Constant.REQUEST_IDENT_OPT_FLAG), StandardCharsets.UTF_8);
        var identOptInfo = $.json.toObject(strIdentOpt, IdentOptCacheInfo.class);
        var sqlInfo = $.json.toJson(ctx.getBodyAsString());
        var encryptSql = sqlInfo.get("sql").asText();
        var parameters = $.json.toList(sqlInfo.get("parameters"), Object.class);
        RedisClient.choose("").get(security.getCacheAppInfo() + identOptInfo.getAppId(), security.getAppInfoCacheExpireSec())
                .onSuccess(appInfo -> {
                    if (appInfo == null) {
                        error(StandardCode.UNAUTHORIZED, "认证错误，AppId不合法", ctx);
                        return;
                    }
                    var appInfoItems = appInfo.split("\n");
                    var privateKey = $.security.asymmetric.getPrivateKey(appInfoItems[2], "RSA");
                    var decryptSql = new String($.security.asymmetric.decrypt(
                            $.security.decodeBase64ToBytes(encryptSql), privateKey, 1024, "RSA/ECB/OAEPWithSHA1AndMGF1Padding"));
                    var sqlAsts = SqlParser.parse(decryptSql);
                    if (sqlAsts == null) {
                        error(StandardCode.BAD_REQUEST, "请求的SQL解析错误", ctx);
                        return;
                    }
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
                    var subjectInfo = packageSubjectInfo(identOptInfo);
                    authPolicy.authentication(resources, subjectInfo)
                            .onSuccess(authResultKind -> {
                                if (authResultKind == AuthResultKind.REJECT) {
                                    error(StandardCode.UNAUTHORIZED, "鉴权错误，没有权限访问对应的资源", ctx);
                                    return;
                                }
                                MysqlClient.choose(URIHelper.newURI(strResourceUriWithoutPath).getHost()).exec(decryptSql, parameters)
                                        .onSuccess(result -> ctx.end(result.toBuffer()))
                                        .onFailure(e -> error(StandardCode.BAD_REQUEST, "数据查询错误", ctx, e));
                            })
                            .onFailure(e -> error(StandardCode.INTERNAL_SERVER_ERROR, "服务错误", ctx, e));
                })
                .onFailure(e -> error(StandardCode.INTERNAL_SERVER_ERROR, "服务错误", ctx, e));
    }

}
