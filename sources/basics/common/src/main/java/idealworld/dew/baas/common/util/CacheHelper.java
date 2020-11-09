package idealworld.dew.baas.common.util;

import com.ecfront.dew.common.$;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Cache helper.
 *
 * @author gudaoxuri
 */
@Slf4j
public class CacheHelper {

    protected static final ConcurrentHashMap<String, SoftReference<Object[]>> CACHES = new ConcurrentHashMap<>();

    private static final int CLEANING_INTERVAL_SEC = 60;

    static {
        $.timer.periodic(CLEANING_INTERVAL_SEC, false, () -> {
            CACHES.forEach((key, value) -> {
                if (value == null
                        || value.get() == null
                        || ((long) value.get()[0]) < System.currentTimeMillis()) {
                    CACHES.remove(key);
                }
            });
        });
    }

    public static void set(String key, Object value, Integer cacheSec) {
        CACHES.put(key,
                new SoftReference<>(
                        new Object[]{System.currentTimeMillis() + cacheSec * 1000, value}
                )
        );
    }

    public static <E> E getSet(String key, Integer cacheSec, Supplier<E> elseFun) {
        var valueInfo = CACHES.getOrDefault(key, null);
        if (valueInfo == null
                || valueInfo.get() == null
                || ((long) valueInfo.get()[0]) < System.currentTimeMillis()) {
            E value = elseFun.get();
            set(key, value, cacheSec);
            return value;
        }
        log.trace("[Cache]Hit [{}]", key);
        return (E) valueInfo.get()[1];
    }

}