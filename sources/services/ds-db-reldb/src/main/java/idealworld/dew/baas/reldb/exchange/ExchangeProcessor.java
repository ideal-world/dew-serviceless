package idealworld.dew.baas.reldb.exchange;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.common.CommonApplication;
import idealworld.dew.baas.common.CommonConfig;
import idealworld.dew.baas.common.auth.LocalResourceCache;
import idealworld.dew.baas.common.dto.exchange.ResourceExchange;
import idealworld.dew.baas.common.dto.exchange.ResourceSubjectExchange;
import idealworld.dew.baas.common.enumeration.OptActionKind;
import idealworld.dew.baas.common.enumeration.ResourceKind;
import idealworld.dew.baas.common.funs.exchange.ExchangeHelper;
import idealworld.dew.baas.common.funs.httpclient.HttpClient;
import idealworld.dew.baas.common.funs.mysql.MysqlClient;
import idealworld.dew.baas.common.util.URIHelper;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeProcessor {

    public static Future<Void> init() {
        return ExchangeHelper.register(new HashSet<>() {
            {
                add("resource." + ResourceKind.RELDB.toString().toLowerCase());
                add("resourceSubject." + ResourceKind.RELDB.toString().toLowerCase());
            }
        }, exchangeData -> {
            if (exchangeData.getSubjectCategory().equalsIgnoreCase("resourceSubject")) {
                if (exchangeData.getActionKind() == OptActionKind.CREATE
                        || exchangeData.getActionKind() == OptActionKind.MODIFY) {
                    HttpClient.request(HttpMethod.GET, exchangeData.getFetchUrl(), null, null, null)
                            .onSuccess(resp -> {
                                var resourceSubjectExchange = $.json.toObject(exchangeData.getDetailData(), ResourceSubjectExchange.class);
                                MysqlClient.remove(exchangeData.getSubjectId());
                                MysqlClient.init(exchangeData.getSubjectId(), CommonApplication.VERTX,
                                        CommonConfig.JDBCConfig.builder()
                                                .url(resourceSubjectExchange.getUri())
                                                .build());
                                log.info("[Exchange]Updated [resourceSubject.id={}] data", exchangeData.getSubjectId());
                            })
                            .onFailure(e -> {
                                log.error("[Exchange]Update [resourceSubject.id={}] error: {}", exchangeData.getSubjectId(), e.getMessage(), e.getCause());
                            });

                } else if (exchangeData.getActionKind() == OptActionKind.DELETE) {
                    MysqlClient.remove(exchangeData.getSubjectId());
                    log.error("[Exchange]Removed [resourceSubject.id={}]", exchangeData.getSubjectId());
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
