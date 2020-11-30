package idealworld.dew.serviceless.common.funs.cache;

import io.vertx.core.Future;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * @author gudaoxuri
 */
@Slf4j
public class LocalCacheHelper extends idealworld.dew.serviceless.common.util.CacheHelper {

    public static <E> Future<E> getSetF(String key, Integer cacheSec, Supplier<Future<E>> elseFun) {
        var valueInfo = CACHES.getOrDefault(key, null);
        return Future.future(promise -> {
            if (valueInfo == null
                    || valueInfo.get() == null
                    || ((long) valueInfo.get()[0]) < System.currentTimeMillis()) {
                elseFun.get()
                        .onSuccess(value -> {
                            set(key, value, cacheSec);
                            promise.complete(value);
                        })
                        .onFailure(e -> {
                            log.error("[Cache]GetSet error {}", e.getMessage(), e.getCause());
                        });
            } else {
                log.trace("[Cache]Hit [{}]", key);
                promise.complete((E)valueInfo.get()[1]);
            }
        });

    }

}
