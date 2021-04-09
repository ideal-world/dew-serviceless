/*
 * Copyright 2021. gudaoxuri
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
import idealworld.dew.framework.dto.IdentOptExchangeInfo;
import idealworld.dew.framework.exception.BadRequestException;
import idealworld.dew.framework.exception.NotFoundException;
import idealworld.dew.framework.exception.UnAuthorizedException;
import idealworld.dew.framework.fun.auth.dto.AuthResultKind;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import idealworld.dew.framework.util.URIHelper;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 关系型数据库控制器.
 *
 * @author gudaoxuri
 */
@Slf4j
public class RelDBProcessor extends EventBusProcessor {

    private static RelDBAuthPolicy authPolicy;

    {
        addProcessor("", eventBusContext ->
                exec(
                        eventBusContext.req.header.get(DewConstant.REQUEST_RESOURCE_URI_FLAG),
                        new JsonObject($.security.decodeBase64ToString(
                                eventBusContext.req.header.get(DewConstant.REQUEST_IDENT_OPT_FLAG), StandardCharsets.UTF_8))
                                .mapTo(IdentOptExchangeInfo.class),
                        eventBusContext.req.body(String.class),
                        eventBusContext.context));
    }

    public RelDBProcessor(RelDBAuthPolicy _authPolicy, String moduleName) {
        super(moduleName);
        authPolicy = _authPolicy;
    }

    public static Future<Buffer> exec(String resourceUriWithoutPath, IdentOptExchangeInfo identOptCacheInfo, String strBody, ProcessContext context) {
        var resourceSubjectCode = URIHelper.newURI(resourceUriWithoutPath).getHost();
        if (!FunSQLClient.contains(resourceSubjectCode)) {
            throw context.helper.error(new NotFoundException("找不到请求的资源主体[" + resourceSubjectCode + "]"));
        }
        if (strBody == null || strBody.isBlank()) {
            throw context.helper.error(new BadRequestException("缺少SQL信息"));
        }
        var sqlInfo = new JsonObject(strBody);
        var sql = sqlInfo.getString("sql");
        var sqlAsts = SqlParser.parse(sql);
        if (sqlAsts == null) {
            log.warn("Sql parse error: {}", sql);
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
        return authPolicy.authentication(context.moduleName, resources, identOptCacheInfo)
                .compose(authResultKind -> {
                    if (authResultKind == AuthResultKind.REJECT) {
                        context.helper.error(new UnAuthorizedException("鉴权错误，没有权限访问对应的资源"));
                    }
                    return FunSQLClient.choose(resourceSubjectCode).rawExec(sql, sqlInfo.getJsonArray("parameters").getList())
                            .compose(result -> context.helper.success(result.toBuffer()));
                });
    }

}
