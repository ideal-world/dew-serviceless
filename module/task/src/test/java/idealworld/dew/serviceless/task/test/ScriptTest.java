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

package idealworld.dew.serviceless.task.test;

import idealworld.dew.serviceless.task.process.ScriptProcessor;
import idealworld.dew.serviceless.task.process.TaskProcessor;
import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
public class ScriptTest {

    @BeforeAll
    public static void before() {
        String dewSDK = new BufferedReader(new InputStreamReader(TaskProcessor.class.getResourceAsStream("/DewSDK_JVM.js")))
                .lines().collect(Collectors.joining("\n"));
        ScriptProcessor.init("http://127.0.0.1:9000", dewSDK);
    }

    @Test
    public void testScript() {
        ScriptProcessor.add(1L, "test1", "await DewSDK.cache.del('test-key')");
        ScriptProcessor.add(1L, "test2", "console.log(await DewSDK.cache.exists('test-key'))");
        ScriptProcessor.add(1L, "test3", "i+1");
        ScriptProcessor.add(1L, "test4", "1+1");
        try {
            ScriptProcessor.execute(1L, "test1", new ArrayList<>());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertEquals("TypeError: (t.adapter || u.adapter) is not a function", e.getMessage());
        }
        try {
            ScriptProcessor.execute(1L, "test2", new ArrayList<>());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertEquals("TypeError: (t.adapter || u.adapter) is not a function", e.getMessage());
        }
        try {
            ScriptProcessor.execute(1L, "test3", new ArrayList<>());
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertEquals("ReferenceError: i is not defined", e.getMessage());
        }
        try {
            ScriptProcessor.execute(1L, "test4", new ArrayList<>());
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @SneakyThrows
    @Test
    public void testMultiThreadScript() {
        ScriptProcessor.add(1L, "test1", "1+1");
        ScriptProcessor.add(2L, "test2", "1+1");
        ScriptProcessor.add(3L, "test3", "1+1");
        var addThread = new Thread(() -> ScriptProcessor.add(3L, "test3_1", "1+1"));
        addThread.start();
        addThread.join();

        var hasError = new AtomicBoolean(false);
        var count = new AtomicLong(0);
        new Thread(() -> {
            while (true) {
                try {
                    ScriptProcessor.add(1L, "test1_1", "1+1");
                    ScriptProcessor.execute(1L, "test1", new ArrayList<>());
                    ScriptProcessor.execute(2L, "test2", new ArrayList<>());
                    count.addAndGet(1);
                } catch (Exception e) {
                    Assertions.fail(e);
                    hasError.set(true);
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                try {
                    ScriptProcessor.execute(1L, "test1", new ArrayList<>());
                    ScriptProcessor.execute(2L, "test2", new ArrayList<>());
                    count.addAndGet(1);
                } catch (Exception e) {
                    Assertions.fail(e);
                    hasError.set(true);
                }
            }
        }).start();
        new Thread(() -> {
            while (true) {
                try {
                    ScriptProcessor.execute(3L, "test3", new ArrayList<>());
                    ScriptProcessor.execute(3L, "test3_1", new ArrayList<>());
                    count.addAndGet(1);
                } catch (Exception e) {
                    Assertions.fail(e);
                    hasError.set(true);
                }
            }
        }).start();

        Thread.sleep(1000);
        System.out.println("Execute times:" + count.get());
        if (hasError.get()) {
            Assertions.fail();
        }

    }

    @Test
    public void testGrammar(){
        String dewSDK = new BufferedReader(new InputStreamReader(TaskProcessor.class.getResourceAsStream("/DewSDK_JVM.js")))
                .lines().collect(Collectors.joining("\n"));
        String testJS = new BufferedReader(new InputStreamReader(TaskProcessor.class.getResourceAsStream("/TodoAction.test.ts")))
                .lines().collect(Collectors.joining("\n"));
        Context context = Context.newBuilder().allowAllAccess(true).build();
        context.eval(Source.create("js", "let global = this"));
        context.eval(Source.create("js", dewSDK));
        context.eval(Source.create("js", "const DewSDK = this.JVM.DewSDK"));
        context.eval(Source.create("js", testJS));
    }

}
