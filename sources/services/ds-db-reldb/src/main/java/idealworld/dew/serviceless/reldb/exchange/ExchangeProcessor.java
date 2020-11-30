package idealworld.dew.serviceless.reldb.exchange;

import com.ecfront.dew.common.$;
import idealworld.dew.serviceless.common.CommonApplication;
import idealworld.dew.serviceless.common.CommonConfig;
import idealworld.dew.serviceless.common.auth.LocalResourceCache;
import idealworld.dew.serviceless.common.dto.exchange.ResourceExchange;
import idealworld.dew.serviceless.common.dto.exchange.ResourceSubjectExchange;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.serviceless.common.funs.cache.RedisClient;
import idealworld.dew.serviceless.common.funs.exchange.ExchangeHelper;
import idealworld.dew.serviceless.common.funs.httpclient.HttpClient;
import idealworld.dew.serviceless.common.funs.mysql.MysqlClient;
import idealworld.dew.serviceless.common.util.URIHelper;
import idealworld.dew.serviceless.reldb.RelDBConfig;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeProcessor {

    public static Future<Void> init(RelDBConfig relDBConfig) {
        var header = HttpClient.getIdentOptHeader(relDBConfig.getIam().getAppId(), relDBConfig.getIam().getTenantId());
        HttpClient.request(HttpMethod.GET, relDBConfig.getIam().getUri() + "/console/app/resource/subject", null, header)
                .onSuccess(result -> {
                    var resourceSubjects = $.json.toList(result.toString(), ResourceSubjectExchange.class);
                    for (var resourceSubject : resourceSubjects) {
                        RedisClient.init(resourceSubject.getCode(), CommonApplication.VERTX,
                                CommonConfig.RedisConfig.builder()
                                        .uri(resourceSubject.getUri())
                                        .password(resourceSubject.getSk())
                                        .build());
                        log.info("[Exchange]Init [resourceSubject.code={}] data", resourceSubject.getCode());
                    }
                })
                .onFailure(e -> log.error("[Exchange]Init resourceSubjects error: {}", e.getMessage(), e.getCause()));
        return ExchangeHelper.register(new HashSet<>() {
            {
                add("resource." + ResourceKind.RELDB.toString().toLowerCase());
                add("resourcesubject." + ResourceKind.RELDB.toString().toLowerCase());
            }
        }, exchangeData -> {
            if (exchangeData.getSubjectCategory().startsWith("resourcesubject.")) {
                if (exchangeData.getActionKind() == OptActionKind.CREATE
                        || exchangeData.getActionKind() == OptActionKind.MODIFY) {
                    var resourceSubjectExchange = $.json.toObject(exchangeData.getDetailData(), ResourceSubjectExchange.class);
                    MysqlClient.remove(resourceSubjectExchange.getCode());
                    MysqlClient.init(resourceSubjectExchange.getCode(), CommonApplication.VERTX,
                            CommonConfig.JDBCConfig.builder()
                                    .uri(resourceSubjectExchange.getUri())
                                    .build());
                    log.info("[Exchange]Updated [resourceSubject.code={}] data", resourceSubjectExchange.getCode());
                } else if (exchangeData.getActionKind() == OptActionKind.DELETE) {
                    var code = (String) exchangeData.getDetailData().get("code");
                    MysqlClient.remove(code);
                    log.error("[Exchange]Removed [resourceSubject.code={}]", code);
                }
            } else {
                var resourceExchange = $.json.toObject(exchangeData.getDetailData(), ResourceExchange.class);
                var resourceActionKind = resourceExchange.getResourceActionKind();
                var resourceUri = URIHelper.newURI(resourceExchange.getResourceUri());
                switch (exchangeData.getActionKind()) {
                    case CREATE:
                        LocalResourceCache.addLocalResource(resourceUri, resourceActionKind);
                        log.info("[Exchange]Created [resource.actionKind={},uri={}] data", resourceActionKind, resourceExchange.getResourceUri());
                        break;
                    case MODIFY:
                        LocalResourceCache.removeLocalResource(resourceUri, resourceActionKind);
                        LocalResourceCache.addLocalResource(resourceUri, resourceActionKind);
                        log.info("[Exchange]Modify [resource.actionKind={},uri={}] data", resourceActionKind, resourceExchange.getResourceUri());
                        break;
                    case DELETE:
                        LocalResourceCache.removeLocalResource(resourceUri, resourceActionKind);
                        log.info("[Exchange]Delete [resource.actionKind={},uri={}] data", resourceActionKind, resourceExchange.getResourceUri());
                        break;
                }
            }
        });
    }

}
