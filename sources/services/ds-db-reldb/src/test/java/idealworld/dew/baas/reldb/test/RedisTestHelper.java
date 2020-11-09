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

package idealworld.dew.baas.reldb.test;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import redis.embedded.RedisServer;

@Slf4j
public class RedisTestHelper {

    private static RedisServer redisServer;

    @SneakyThrows
    public static void start() {
        log.info("Enabled Redis Test");
        redisServer = new RedisServer();
        if (!redisServer.isActive()) {
            try {
                redisServer.start();
            } catch (Exception e) {
                log.warn("Start embedded redis error.");
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (redisServer.isActive()) {
                redisServer.stop();
            }
        }));
    }

}
