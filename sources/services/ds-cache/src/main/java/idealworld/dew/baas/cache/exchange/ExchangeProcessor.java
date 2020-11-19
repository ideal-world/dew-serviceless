package idealworld.dew.baas.cache.exchange;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.cache.CacheConfig;
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

    public static Future<Void> init(CacheConfig cacheConfig) {
        var header = HttpClient.getIdentOptHeader(cacheConfig.getIam().getAppId(), cacheConfig.getIam().getTenantId());
        HttpClient.request(HttpMethod.GET, cacheConfig.getIam().getUri() + "/console/app/resource/subject", null, header)
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
                add("resourcesubject." + ResourceKind.CACHE.toString().toLowerCase());
            }
        }, exchangeData -> {
            if (exchangeData.getActionKind() == OptActionKind.CREATE
                    || exchangeData.getActionKind() == OptActionKind.MODIFY) {
                var resourceSubject = $.json.toObject(exchangeData.getDetailData(), ResourceSubjectExchange.class);
                RedisClient.remove(resourceSubject.getCode());
                RedisClient.init(resourceSubject.getCode(), CommonApplication.VERTX,
                        CommonConfig.RedisConfig.builder()
                                .uri(resourceSubject.getUri())
                                .password(resourceSubject.getSk())
                                .build());
                log.info("[Exchange]Updated [resourceSubject.code={}] data", resourceSubject.getCode());
            } else if (exchangeData.getActionKind() == OptActionKind.DELETE) {
                var code = (String) exchangeData.getDetailData().get("code");
                RedisClient.remove(code);
                log.error("[Exchange]Removed [resourceSubject.code={}]", code);
            }
        });
    }

}
