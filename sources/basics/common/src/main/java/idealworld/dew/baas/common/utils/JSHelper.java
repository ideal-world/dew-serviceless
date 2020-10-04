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

import javax.script.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Js helper.
 *
 * @author gudoxuri
 */
public class JSHelper {

    private static final ScriptEngineManager SCRIPT_ENGINE_MANAGER = new ScriptEngineManager();
    private static final Map<String, JSHelper> JS_HELPER_MAP = new HashMap<>();
    private Invocable invocable;

    private JSHelper(Invocable invocable) {
        this.invocable = invocable;
    }

    /**
     * Build js helper.
     *
     * @param jsFunsCode the js funs code
     * @return the js helper
     * @throws ScriptException the script exception
     */
    public static JSHelper build(String jsFunsCode) throws ScriptException {
        Compilable jsEngine = (Compilable) SCRIPT_ENGINE_MANAGER.getEngineByName("nashorn");
        CompiledScript script = jsEngine.compile("var $ = Java.type('com.ecfront.dew.common.$');\r\n" + jsFunsCode);
        script.eval();
        return new JSHelper((Invocable) script.getEngine());
    }

    /**
     * Build by inst id.
     *
     * @param jsFunsCode the js funs code
     * @param instId     the inst id
     * @throws ScriptException the script exception
     */
    public static void buildByInstId(String jsFunsCode, String instId) throws ScriptException {
        JS_HELPER_MAP.put(instId, build(jsFunsCode));
    }

    /**
     * Gets by inst id.
     *
     * @param instId the inst id
     * @return the by inst id
     */
    public static JSHelper getByInstId(String instId) {
        return JS_HELPER_MAP.get(instId);
    }

    /**
     * Remove by inst id.
     *
     * @param instId the inst id
     */
    public static void removeByInstId(String instId) {
        JS_HELPER_MAP.remove(instId);
    }

    /**
     * Remove all.
     */
    public static void removeAll() {
        JS_HELPER_MAP.clear();
    }

    /**
     * Execute t.
     *
     * @param <T>       the type parameter
     * @param jsFunName the js fun name
     * @param args      the args
     * @return the t
     * @throws ScriptException       the script exception
     * @throws NoSuchMethodException the no such method exception
     */
    public <T> T execute(String jsFunName, Object... args) throws ScriptException, NoSuchMethodException {
        return (T) invocable.invokeFunction(jsFunName, args);
    }

    /*public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        JSHelper i1 = JSHelper.build("function r1(data){return 1;}");
        JSHelper i2 = JSHelper.build("function r2(D){return $.field.getGenderByIdCard(D.idcard);}");
        JSHelper i3 = JSHelper.build("function r1(data){return data.a+data.b;}");
        Integer r1 = i1.execute("r1", "");
        String r2 = i2.execute("r2", new HashMap<String,Object>(){{
            put("idcard","331082193609160037");
        }});
        Double r3 = i3.execute("r1", new HashMap<String, Object>() {{
            put("a", 2);
            put("b", 4);
        }});
        System.out.println(r1);
        System.out.println(r2);
        System.out.println(r3);
    }*/

}
