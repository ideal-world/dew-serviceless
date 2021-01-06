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
import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.exception.NotFoundException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.auth.AuthenticationProcessor;
import idealworld.dew.framework.fun.auth.dto.AuthResultKind;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import idealworld.dew.framework.util.CacheHelper;
import idealworld.dew.framework.util.URIHelper;
import idealworld.dew.serviceless.reldb.RelDBConfig;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL服务.
 *
 * @author gudaoxuri
 */
@Slf4j
public class SqlProcessor extends EventBusProcessor {

    private static final Pattern CONTEXT_FLAG_R = Pattern.compile("#\\{\\.+\\}");

    {
        addProcessor("", eventBusContext ->
                exec(
                        eventBusContext.req.header.get(DewConstant.REQUEST_RESOURCE_URI_FLAG),
                        new JsonObject($.security.decodeBase64ToString(eventBusContext.req.header.get(DewConstant.REQUEST_IDENT_OPT_FLAG), StandardCharsets.UTF_8)).mapTo(IdentOptCacheInfo.class),
                        eventBusContext.req.body(String.class),
                        eventBusContext.context));
    }

    private static RelDBAuthPolicy authPolicy;
    private static RelDBConfig config;

    public SqlProcessor(RelDBAuthPolicy _authPolicy, String moduleName, RelDBConfig _config) {
        super(moduleName);
        authPolicy = _authPolicy;
        config = _config;
    }

    public static Future<Buffer> exec(String resourceUriWithoutPath, IdentOptCacheInfo identOptCacheInfo, String strBody, ProcessContext context) {
        var resourceSubjectCode = URIHelper.newURI(resourceUriWithoutPath).getHost();
        if (!FunSQLClient.contains(resourceSubjectCode)) {
            throw context.helper.error(new BadRequestException("请求的资源主题[" + resourceSubjectCode + "]不存在"));
        }
        if (strBody == null || strBody.isBlank()) {
            throw context.helper.error(new BadRequestException("缺少SQL信息"));
        }
        var sqlInfo = new JsonObject(strBody);
        var encryptSql = sqlInfo.getString("sql");
        var parameters = sqlInfo.getJsonArray("parameters").getList();
        return context.cache.get(DewConstant.CACHE_APP_INFO + identOptCacheInfo.getAppId(), ((RelDBConfig) context.conf).getSecurity().getAppInfoCacheExpireSec())
                .compose(appInfo -> {
                    if (appInfo == null) {
                        throw context.helper.error(new UnAuthorizedException("认证错误，AppId不合法"));
                    }
                    var appInfoItems = appInfo.split("\n");
                    var privateKey = CacheHelper.getSet(appInfoItems[2], Integer.MAX_VALUE, () -> $.security.asymmetric.getPrivateKey(appInfoItems[2], "RSA"));
                    var decryptSql = new String($.security.asymmetric.decrypt(
                            $.security.decodeBase64ToBytes(encryptSql), privateKey, 1024, "RSA/ECB/OAEPWithSHA1AndMGF1Padding"));
                    var finalSql = CONTEXT_FLAG_R.matcher(decryptSql).replaceAll(h -> {
                        var hit = h.group();
                        if (hit.equalsIgnoreCase(config.getContextFlag().getCurrentUserId())) {
                            parameters.add(identOptCacheInfo.getAccountCode());
                        } else if (hit.equalsIgnoreCase(config.getContextFlag().getCurrentTimestamp())) {
                            parameters.add(System.currentTimeMillis());
                        } else {
                            throw context.helper.error(new NotFoundException("占位符[" + hit + "]不存在"));
                        }
                        return "?";
                    });
                    var sqlAsts = SqlParser.parse(finalSql);
                    if (sqlAsts == null) {
                        throw context.helper.error(new BadRequestException("请求的SQL解析错误"));
                    }
                    var resources = sqlAsts.stream()
                            .flatMap(sqlAst -> {
                                var actionKind = sqlAst.getActionKind().toString().toLowerCase();
                                var fields = sqlAst.getCondFields();
                                fields.addAll(sqlAst.getEffectFields());
                                fields.addAll(sqlAst.getReturnFields());
                                return fields.stream().map(field ->
                                        new Tuple2<>(URIHelper.newURI(resourceUriWithoutPath + "/" + sqlAst.getTable() + "/" + field), actionKind)
                                );
                            })
                            .collect(Collectors.groupingBy(item -> item._1, Collectors.mapping(item -> item._0, Collectors.toList())));
                    var subjectInfo = AuthenticationProcessor.packageSubjectInfo(identOptCacheInfo);
                    return authPolicy.authentication(context.moduleName, resources, subjectInfo)
                            .compose(authResultKind -> {
                                if (authResultKind == AuthResultKind.REJECT) {
                                    context.helper.error(new UnAuthorizedException("鉴权错误，没有权限访问对应的资源"));
                                }
                                return FunSQLClient.choose(resourceSubjectCode).rawExec(finalSql, parameters)
                                        .compose(result -> context.helper.success(result.toBuffer()));
                            });
                });
    }

}
