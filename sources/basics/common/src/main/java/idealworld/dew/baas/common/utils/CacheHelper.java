/*
 * Copyright 2020. the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package idealworld.dew.baas.common.utils;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.tuple.Tuple2;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * The type Cache helper.
 *
 * @author gudaoxuri
 */
@Slf4j
public class CacheHelper {

    private static final ConcurrentHashMap<String, SoftReference<Tuple2<Long, Resp<?>>>> CACHES = new ConcurrentHashMap<>();

    static {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000 * 60);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                CACHES.forEach((key, value) -> {
                    if (value == null
                            || value.get() == null
                            || value.get()._0 < System.currentTimeMillis()) {
                        CACHES.remove(key);
                    }
                });
            }
        }).start();
    }

    /**
     * Get value.
     *
     * @param key      the key
     * @param cacheSec the cache sec
     * @param elseFun  the else fun
     * @return the value
     */
    public static Resp<?> getOrElse(String key, Integer cacheSec, Supplier<Resp<?>> elseFun) {
        var md5Key = $.security.digest.digest(key, "MD5");
        var valueInfo = CACHES.getOrDefault(md5Key, null);
        if (valueInfo == null
                || valueInfo.get() == null
                || valueInfo.get()._0 < System.currentTimeMillis()) {
            var value = elseFun.get();
            put(md5Key, value, cacheSec);
            return value;
        }
        log.debug("Hit cache : {}", key);
        return valueInfo.get()._1;
    }

    private static void put(String key, Resp<?> value, Integer cacheSec) {
        CACHES.put(key, new SoftReference<>(new Tuple2<>(System.currentTimeMillis() + cacheSec * 1000, value)));
    }

}
