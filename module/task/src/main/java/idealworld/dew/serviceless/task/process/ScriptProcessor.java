/*
 * Copyright 2020. gudaoxuri
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

package idealworld.dew.serviceless.task.process;

import com.ecfront.dew.common.exception.RTScriptException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyArray;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
@Slf4j
public class ScriptProcessor {

    private static final Map<Long, Context> SCRIPT_CONTAINER = new HashMap<>();
    private static final Map<Long, Lock> LOCKS = new HashMap<>();


    private static String _gatewayServerUrl;

    public static void init(String gatewayServerUrl) {
        _gatewayServerUrl = gatewayServerUrl;
    }

    public static void init(Long appId, String funs) {
        if (!SCRIPT_CONTAINER.containsKey(appId)) {
            LOCKS.putIfAbsent(appId, new ReentrantLock());
            LOCKS.get(appId).lock();
            try {
                Context context = Context.newBuilder().allowAllAccess(true).allowHostAccess(HostAccess.newBuilder(HostAccess.ALL)
                        .targetTypeMapping(BigDecimal.class, Double.class,
                                (v) -> true,
                                BigDecimal::doubleValue)
                        .build()).build();
                SCRIPT_CONTAINER.put(appId, context);
                SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "let global = this"));
                SCRIPT_CONTAINER.get(appId).eval(Source.create("js", funs));
                SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "const $ = Java.type('idealworld.dew.serviceless.task.helper.ScriptExchangeHelper')"));
                SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "const DewSDK = JVM.DewSDK\r\n" +
                        "DewSDK.setting.ajax((url, headers, data) => {\r\n" +
                        "  return new Promise((resolve, reject) => {\n\n" +
                        "    resolve({data:JSON.parse($.req(url,headers,data))})\n\n" +
                        "  })\n\n" +
                        "})\r\n" +
                        "DewSDK.setting.currentTime(() => {\r\n" +
                        "  return $.currentTime()\r\n" +
                        "})\r\n" +
                        "DewSDK.setting.signature((text, key) => {\r\n" +
                        "  return $.signature(text, key)\r\n" +
                        "})\r\n"));
                SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "DewSDK.init('" + _gatewayServerUrl + "', '" + appId + "')"));
            } finally {
                LOCKS.get(appId).unlock();
            }
        }
    }

    public static void add(Long appId, String funName, String funBody) {
        LOCKS.get(appId).lock();
        try {
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "async function " + funName + "(){\r\n" + funBody + "\r\n}"));
        } finally {
            LOCKS.get(appId).unlock();
        }
    }

    public static void remove(Long appId) {
        SCRIPT_CONTAINER.remove(appId);
    }

    public static Object execute(Long appId, String funName, List<?> parameters) {
        LOCKS.get(appId).lock();
        try {
            var result = new Object[2];
            Consumer<Object> then = (v) -> result[0] = v;
            Consumer<Object> catchy = (v) -> result[1] = v;
            var funNamePath = funName.split("\\.");
            SCRIPT_CONTAINER.get(appId).getBindings("js").getMember("JVM").getMember(funNamePath[0]).getMember(funNamePath[1])
                    .execute(compatibilityParameters(parameters).toArray())
                    .invokeMember("then", then).invokeMember("catch", catchy);
            if (result[1] != null && !result[1].toString().trim().isBlank()) {
                throw new RTScriptException(result[1].toString().trim());
            }
            return compatibilityResult(result[0]);
        } finally {
            LOCKS.get(appId).unlock();
        }
    }

    private static List<?> compatibilityParameters(Collection<?> parameters) {
        return parameters.stream().map(p -> {
            if (p instanceof Map) {
                return ProxyObject.fromMap((Map) p);
            } else if (p instanceof Collection) {
                return compatibilityParameters((Collection) p);
            }
            return p;
        }).collect(Collectors.toList());
    }

    private static Object compatibilityResult(Object result) {
        if (result instanceof Map) {
            return JsonObject.mapFrom(result);
        } else if (result instanceof Collection) {
            return new JsonArray((List) (((Collection) result).stream()
                    .map(r -> compatibilityResult(r))
                    .collect(Collectors.toList())));
        } else if (result instanceof ProxyObject) {
            // TODO
            ((ProxyObject) result).getMemberKeys();
        }
        return result;
    }

    static final class MapProxyObject implements ProxyObject {

        Map<String, Object> values;

        MapProxyObject(Map<String, Object> values) {
            this.values = values;
        }

        @Override
        public void putMember(String key, Value value) {
            values.put(key, value.isHostObject() ? value.asHostObject() : value);
        }

        @Override
        public boolean hasMember(String key) {
            return values.containsKey(key);
        }

        public Map<String, Object> getMap() {
            return values;
        }

        @Override
        public Object getMemberKeys() {
            return new ProxyArray() {
                private final Object[] keys = values.keySet().toArray();

                @Override
                public void set(long index, Value value) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public long getSize() {
                    return keys.length;
                }

                @Override
                public Object get(long index) {
                    if (index < 0 || index > Integer.MAX_VALUE) {
                        throw new ArrayIndexOutOfBoundsException();
                    }
                    return keys[(int) index];
                }
            };
        }

        @Override
        public Object getMember(String key) {
            return values.get(key);
        }

        @Override
        public boolean removeMember(String key) {
            if (values.containsKey(key)) {
                values.remove(key);
                return true;
            } else {
                return false;
            }
        }
    }

}
