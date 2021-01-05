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
    private static String _dewSDK;
    private static String _requirejs;

    public static void init(String gatewayServerUrl, String requirejs, String dewSDK) {
        _gatewayServerUrl = gatewayServerUrl;
        _requirejs = requirejs;
        _dewSDK = dewSDK;
    }

    public static void add(Long appId, String funName, String funBody) {
        LOCKS.putIfAbsent(appId, new ReentrantLock());
        LOCKS.get(appId).lock();
        if (!SCRIPT_CONTAINER.containsKey(appId)) {
            Context context = Context.newBuilder().allowAllAccess(true).build();
            SCRIPT_CONTAINER.put(appId, context);
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", _requirejs));
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", _dewSDK));
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "const $ = Java.type('idealworld.dew.serviceless.task.helper.ScriptExchangeHelper')"));
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "require('DewSDK').setAjaxImpl((url, headers, data) => {\r\n" +
                    "        return new Promise((resolve, reject) => {\n\n" +
                    "            resolve({data:JSON.parse($.req(url,headers,data))})\n\n" +
                    "        })\n\n" +
                    "    })"));
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "const DewSDK = require('DewSDK').DewSDK"));
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "DewSDK.init('" + _gatewayServerUrl + "', '" + appId + "')"));
        }
        SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "async function " + funName + "(){\r\n" + funBody + "\r\n}"));
        LOCKS.get(appId).unlock();
    }

    public static void remove(Long appId) {
        SCRIPT_CONTAINER.remove(appId);
    }

    public static void execute(Long appId, String funName) {
        LOCKS.get(appId).lock();
        try {
            var normal = new ScriptOutput();
            var error = new ScriptOutput();
            Consumer<Object> then = (v) -> normal.write("" + v);
            Consumer<Object> catchy = (v) -> error.write("" + v);
            SCRIPT_CONTAINER.get(appId).getBindings("js").getMember(funName).execute()
                    .invokeMember("then", then).invokeMember("catch", catchy);
            if (!error.toString().trim().isBlank()) {
                throw new RTScriptException(error.toString().trim());
            }
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
