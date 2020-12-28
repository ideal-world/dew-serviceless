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

import com.oracle.truffle.api.impl.Accessor;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.js.runtime.builtins.JSProxy;
import com.oracle.truffle.js.runtime.objects.JSObject;
import com.oracle.truffle.js.runtime.objects.PropertyProxy;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gudaoxuri
 */
public class ScriptProcessor {

    private static final Map<Long, Context> SCRIPT_CONTAINER = new HashMap<>();

    private static String _gatewayServerUrl;
    private static String _dewSDK;
    private static String _requirejs;

    public static void init(String gatewayServerUrl, String requirejs, String dewSDK) {
        _gatewayServerUrl = gatewayServerUrl;
        _requirejs = requirejs;
        _dewSDK = dewSDK;
    }

    public synchronized static void add(Long appId, String funName, String funBody) {
        if (!SCRIPT_CONTAINER.containsKey(appId)) {
            Context context = Context.newBuilder().allowAllAccess(true).build();
            SCRIPT_CONTAINER.put(appId, context);
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", _requirejs));
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", _dewSDK));
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "const $ = Java.type('idealworld.dew.serviceless.task.helper.ScriptExchangeHelper')"));
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "const DewSDK = require('DewSDK').DewSDK"));
            SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "DewSDK.init('" + _gatewayServerUrl + "', '" + appId + "')"));
        }
        SCRIPT_CONTAINER.get(appId).eval(Source.create("js", "async function " + funName + "(){\r\n"+funBody+"\r\n}"));
    }

    public static void remove(Long appId) {
        SCRIPT_CONTAINER.remove(appId);
    }

    public static void execute(Long appId, String funName) {
        var value = SCRIPT_CONTAINER.get(appId).getBindings("js").getMember(funName).execute();
        Object name = getPropertyWithoutSideEffect(value, "name");
        Object message = getPropertyWithoutSideEffect(obj, "message");
            System.out.println(value.toString());
    }

    private static Object getPropertyWithoutSideEffect(DynamicObject obj, String key) {
        Object value = obj.get(key);
        if (value == null) {
            return !JSProxy.isProxy(obj) ? getPropertyWithoutSideEffect(JSObject.getPrototype(obj), key) : null;
        } else if (value instanceof Accessor) {
            return "{Accessor}";
        } else {
            return value instanceof PropertyProxy ? null : value;
        }
    }

}
