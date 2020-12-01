/*
 * Copyright 2020. gudaoxuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package idealworld.dew.serviceless.reldb.process;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.StandardCode;
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.IdentOptCacheInfo;
import idealworld.dew.serviceless.common.enumeration.AuthResultKind;
import idealworld.dew.serviceless.common.funs.cache.RedisClient;
import idealworld.dew.serviceless.common.funs.httpserver.CommonHttpHandler;
import idealworld.dew.serviceless.common.funs.mysql.MysqlClient;
import idealworld.dew.serviceless.common.util.URIHelper;
import idealworld.dew.serviceless.reldb.RelDBConfig;
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
            error(StandardCode.BAD_REQUEST, AuthHandler.class, "请求格式不合法", ctx);
            return;
        }
        var strResourceUriWithoutPath = ctx.request().getHeader(Constant.REQUEST_RESOURCE_URI_FLAG);
        var resourceSubjectCode = URIHelper.newURI(strResourceUriWithoutPath).getHost();
        if (!MysqlClient.contains(resourceSubjectCode)) {
            error(StandardCode.BAD_REQUEST, AuthHandler.class, "请求的资源主题不存在", ctx);
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
                        error(StandardCode.UNAUTHORIZED, AuthHandler.class, "认证错误，AppId不合法", ctx);
                        return;
                    }
                    var appInfoItems = appInfo.split("\n");
                    var privateKey = $.security.asymmetric.getPrivateKey(appInfoItems[2], "RSA");
                    var decryptSql = new String($.security.asymmetric.decrypt(
                            $.security.decodeBase64ToBytes(encryptSql), privateKey, 1024, "RSA/ECB/OAEPWithSHA1AndMGF1Padding"));
                    var sqlAsts = SqlParser.parse(decryptSql);
                    if (sqlAsts == null) {
                        error(StandardCode.BAD_REQUEST, AuthHandler.class, "请求的SQL解析错误", ctx);
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
                                    error(StandardCode.UNAUTHORIZED, AuthHandler.class, "鉴权错误，没有权限访问对应的资源", ctx);
                                    return;
                                }
                                MysqlClient.choose(URIHelper.newURI(strResourceUriWithoutPath).getHost()).exec(decryptSql, parameters)
                                        .onSuccess(result -> ctx.end(result.toBuffer()))
                                        .onFailure(e -> error(StandardCode.BAD_REQUEST, AuthHandler.class, "数据查询错误", ctx, e));
                            })
                            .onFailure(e -> error(StandardCode.INTERNAL_SERVER_ERROR, AuthHandler.class, "鉴权服务错误", ctx, e));
                })
                .onFailure(e -> error(StandardCode.INTERNAL_SERVER_ERROR, AuthHandler.class, "缓存服务错误", ctx, e));
    }

}
