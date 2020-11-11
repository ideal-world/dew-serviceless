package idealworld.dew.baas.reldb.exchange;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.common.CommonApplication;
import idealworld.dew.baas.common.CommonConfig;
import idealworld.dew.baas.common.enumeration.OptActionKind;
import idealworld.dew.baas.common.funs.exchange.ExchangeHelper;
import idealworld.dew.baas.common.funs.httpclient.HttpClient;
import idealworld.dew.baas.common.funs.mysql.MysqlClient;
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
                add("resource");
                add("resourceSubject");
            }
        }, exchangeData -> {
            if (exchangeData.getSubjectCategory().equalsIgnoreCase("resourceSubject")) {
                if (exchangeData.getActionKind() == OptActionKind.CREATE
                        || exchangeData.getActionKind() == OptActionKind.MODIFY) {
                    HttpClient.request(HttpMethod.GET, exchangeData.getFetchUrl(), null, null, null)
                            .onSuccess(resp -> {
                                MysqlClient.remove(exchangeData.getSubjectId());
                                MysqlClient.init(exchangeData.getSubjectId(), CommonApplication.VERTX,
                                        $.json.toObject(resp, CommonConfig.JDBCConfig.class));
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
                // TODO
            }
        });
    }

}
