package idealworld.dew.baas.reldb.exchange;

import idealworld.dew.baas.common.funs.exchange.ExchangeHelper;
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

        });
    }

}
