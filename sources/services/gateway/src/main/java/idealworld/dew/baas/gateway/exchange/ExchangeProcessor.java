package idealworld.dew.baas.gateway.exchange;

import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.baas.common.funs.exchange.ExchangeHelper;
import idealworld.dew.baas.gateway.process.GatewayAuthPolicy;
import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeProcessor {

    public static Future<Void> init(GatewayAuthPolicy policy) {
        return ExchangeHelper.register(new HashSet<>() {
            {
                add("resource");
            }
        }, exchangeData -> {
            URI resourceUri;
            var resUri = exchangeData.getDetailData().get("resourceUri").toString();
            var resourceActionKind = exchangeData.getDetailData().get("resourceActionKind").toString();
            try {
                resourceUri = new URI(resUri);
            } catch (URISyntaxException e) {
                log.error("[Exchange]URI [{}] parse error", resUri, e);
                throw new RTException(e);
            }
            switch (exchangeData.getActionKind()) {
                case CREATE:
                    policy.addLocalResource(resourceUri, resourceActionKind);
                    break;
                case MODIFY:
                    policy.removeLocalResource(resourceUri, resourceActionKind);
                    policy.addLocalResource(resourceUri, resourceActionKind);
                    break;
                case DELETE:
                    policy.removeLocalResource(resourceUri, resourceActionKind);
                    break;
            }
        });
    }

}
