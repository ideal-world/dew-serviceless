package idealworld.dew.baas.gateway.exchange;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.common.auth.LocalResourceCache;
import idealworld.dew.baas.common.dto.exchange.ResourceExchange;
import idealworld.dew.baas.common.funs.exchange.ExchangeHelper;
import idealworld.dew.baas.common.util.URIHelper;
import io.vertx.core.Future;
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
                add("resource");
            }
        }, exchangeData -> {
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
        });
    }

}
