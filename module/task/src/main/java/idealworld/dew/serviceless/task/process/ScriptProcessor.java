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
import lombok.extern.slf4j.Slf4j;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

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
                Context context = Context.newBuilder().allowAllAccess(true).build();
                SCRIPT_CONTAINER.put(appId, context);
                SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "let global = this"));
                SCRIPT_CONTAINER.get(appId).eval(Source.create("js", funs));
                SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "const $ = Java.type('idealworld.dew.serviceless.task.helper.ScriptExchangeHelper')"));
                SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "const DewSDK = this.JVM.DewSDK\r\n" +
                        "DewSDK.setting.ajax((url, headers, data) => {\r\n" +
                        "  return new Promise((resolve, reject) => {\n\n" +
                        "    resolve({data:JSON.parse($.req(url,headers,data))})\n\n" +
                        "  })\n\n" +
                        "})\r\n" +
                        "Dew.setting.currentTime(() => {\r\n" +
                        "  return $.currentTime()\r\n" +
                        "})\r\n" +
                        "Dew.setting.signature((text, key) => {\r\n" +
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
            var normal = new ScriptOutput();
            var error = new ScriptOutput();
            Consumer<Object> then = (v) -> normal.write("" + v);
            Consumer<Object> catchy = (v) -> error.write("" + v);
            SCRIPT_CONTAINER.get(appId).getBindings("js").getMember(funName).execute(parameters)
                    .invokeMember("then", then).invokeMember("catch", catchy);
            if (!error.toString().trim().isBlank()) {
                throw new RTScriptException(error.toString().trim());
            }
            // TODO
            return normal;
        } finally {
            LOCKS.get(appId).unlock();
        }
    }

    private static class ScriptOutput extends ByteArrayOutputStream {

        void write(String text) {
            try {
                this.write(text.getBytes());
            } catch (IOException e) {
                assert false;
            }
        }

        @Override
        public synchronized String toString() {
            return this.toString(StandardCharsets.UTF_8);
        }
    }

}
