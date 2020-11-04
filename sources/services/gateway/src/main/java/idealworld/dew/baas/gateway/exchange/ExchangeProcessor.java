package idealworld.dew.baas.gateway.exchange;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.baas.gateway.GatewayConfig;
import idealworld.dew.baas.gateway.process.ReadonlyAuthPolicy;
import idealworld.dew.baas.gateway.util.RedisClient;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ExchangeProcessor {

    public static Future<Void> register(GatewayConfig.Exchange exchange, ReadonlyAuthPolicy policy) {
        Promise<Void> promise = Promise.promise();
        RedisClient.subscribe(exchange.getTopic(), message -> {
            var exchangeData = $.json.toObject(message, ExchangeData.class);
            URI resourceUri;
            try {
                resourceUri = new URI(exchangeData.getResourceUri());
            } catch (URISyntaxException e) {
                log.error("[Exchange] URI [{}] parse error", exchangeData.getResourceUri(), e);
                throw new RTException(e);
            }
            if (exchangeData.getAddOpt()) {
                policy.addLocalResource(resourceUri, exchangeData.getActionKind());
            } else if (exchangeData.getActionKind() == null) {
                policy.removeLocalResource(resourceUri);
            } else {
                policy.removeLocalResource(resourceUri, exchangeData.getActionKind());
            }
        }).onSuccess(response -> promise.complete());
        return promise.future();
    }

}
