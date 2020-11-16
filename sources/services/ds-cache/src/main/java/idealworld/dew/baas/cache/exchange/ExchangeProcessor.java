package idealworld.dew.baas.cache.exchange;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.common.CommonApplication;
import idealworld.dew.baas.common.CommonConfig;
import idealworld.dew.baas.common.dto.exchange.ResourceSubjectExchange;
import idealworld.dew.baas.common.enumeration.OptActionKind;
import idealworld.dew.baas.common.enumeration.ResourceKind;
import idealworld.dew.baas.common.funs.cache.RedisClient;
import idealworld.dew.baas.common.funs.exchange.ExchangeHelper;
import idealworld.dew.baas.common.funs.httpclient.HttpClient;
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
                add("resourceSubject." + ResourceKind.CACHE.toString().toLowerCase());
            }
        }, exchangeData -> {
            if (exchangeData.getActionKind() == OptActionKind.CREATE
                    || exchangeData.getActionKind() == OptActionKind.MODIFY) {
                HttpClient.request(HttpMethod.GET, exchangeData.getFetchUrl(), null, null, null)
                        .onSuccess(resp -> {
                            var resourceSubjectExchange = $.json.toObject(exchangeData.getDetailData(), ResourceSubjectExchange.class);
                            RedisClient.remove(exchangeData.getSubjectCode());
                            RedisClient.init(exchangeData.getSubjectCode(), CommonApplication.VERTX,
                                    CommonConfig.RedisConfig.builder()
                                            .uri(resourceSubjectExchange.getUri())
                                            .password(resourceSubjectExchange.getSk())
                                            .build());
                            log.info("[Exchange]Updated [resourceSubject.id={}] data", exchangeData.getSubjectCode());
                        })
                        .onFailure(e -> {
                            log.error("[Exchange]Update [resourceSubject.id={}] error: {}", exchangeData.getSubjectCode(), e.getMessage(), e.getCause());
                        });
            } else if (exchangeData.getActionKind() == OptActionKind.DELETE) {
                RedisClient.remove(exchangeData.getSubjectCode());
                log.error("[Exchange]Removed [resourceSubject.id={}]", exchangeData.getSubjectCode());
            }
        });
    }

}
