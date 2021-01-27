/*
 * Copyright 2021. gudaoxuri
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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 脚本测试.
 *
 * @author gudaoxuri
 */
public class ScriptTest {

    @BeforeAll
    public static void before() {
        // 无法触达的地址
        String testJS = new BufferedReader(new InputStreamReader(TaskProcessor.class.getResourceAsStream("/test.js")))
                .lines().collect(Collectors.joining("\n"));
        ScriptProcessor.init("test", testJS);
        ScriptProcessor.init("test", testJS);
        ScriptProcessor.init("test", testJS);
    }

    @Test
    public void testScript() {
        ScriptProcessor.add("test", "test1", "await DewSDK.cache.del('test-key')");
        ScriptProcessor.add("test", "test2", "console.log(await DewSDK.cache.exists('test-key'))");
        ScriptProcessor.add("test", "test3", "i+1");
        ScriptProcessor.add("test", "test4", "1+1");
        try {
            ScriptProcessor.execute("test", "test1", new ArrayList<>(), null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertEquals("java.net.ConnectException: Connection refused: no further information", e.getMessage());
        }
        try {
            ScriptProcessor.execute("test", "test2", new ArrayList<>(), null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertEquals("java.net.ConnectException: Connection refused: no further information", e.getMessage());
        }
        try {
            ScriptProcessor.execute("test", "test3", new ArrayList<>(), null);
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertEquals("ReferenceError: i is not defined", e.getMessage());
        }
        try {
            ScriptProcessor.execute("test", "test4", new ArrayList<>(), null);
        } catch (Exception e) {
            Assertions.fail(e);
        }
    }

    @SneakyThrows
    @Test
    public void testMultiThreadScript() {
        ScriptProcessor.add("test", "test1", "1+1");
        ScriptProcessor.add("test2", "test2", "1+1");
        ScriptProcessor.add("test3", "test3", "1+1");
        var addThread = new Thread(() -> ScriptProcessor.add("test3", "test3_1", "1+1"));
        addThread.start();
        addThread.join();

        var hasError = new AtomicBoolean(false);
        var count = new AtomicLong(0);
        new Thread(() -> {
            while (true) {
                try {
                    ScriptProcessor.add("test", "test1_1", "1+1");
                    ScriptProcessor.execute("test", "test1", new ArrayList<>(), null);
                    ScriptProcessor.execute("test2", "test2", new ArrayList<>(), null);
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
                    ScriptProcessor.execute("test", "test1", new ArrayList<>(), null);
                    ScriptProcessor.execute("test2", "test2", new ArrayList<>(), null);
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
                    ScriptProcessor.execute("test3", "test3", new ArrayList<>(), null);
                    ScriptProcessor.execute("test3", "test3_1", new ArrayList<>(), null);
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

    /**
     * 此测试先在 plugin-gulp 中执行 gulp testToTaskModule 以获得 test.js 文件
     */
    @Test
    public void testGrammar() {
        var result = ScriptProcessor.execute("test", "TodoAction2_test.ioTestStr", new ArrayList<>() {
            {
                add("测试");
                add(100);
                add(new ArrayList<>() {
                    {
                        add("1");
                        add("2");
                        add("3");
                    }
                });
                add("ddddd");
            }
        }, null);
        Assertions.assertEquals("测试", result);
        result = ScriptProcessor.execute("test", "TodoAction2_test.ioTestNum", new ArrayList<>() {
            {
                add("测试");
                add(100);
                add(new ArrayList<>() {
                    {
                        add("1");
                        add("2");
                        add("3");
                    }
                });
                add("ddddd");
            }
        }, null);
        Assertions.assertEquals(100, result);
        result = ScriptProcessor.execute("test", "TodoAction2_test.ioTestArr", new ArrayList<>() {
            {
                add("测试");
                add(100);
                add(new ArrayList<>() {
                    {
                        add("1");
                        add("2");
                        add("3");
                    }
                });
                add("ddddd");
            }
        }, null);
        Assertions.assertEquals("3", ((JsonArray) result).getString(2));
        result = ScriptProcessor.execute("test", "TodoAction2_test.ioTestArr2", new ArrayList<>(), null);
        Assertions.assertEquals("xxxcxccc", ((JsonArray) result).getString(0));
        result = ScriptProcessor.execute("test", "TodoAction2_test.ioTestArr3", new ArrayList<>(), null);
        Assertions.assertEquals("xxxcxccc", ((JsonArray) result).getJsonObject(0).getString("content"));
        result = ScriptProcessor.execute("test", "TodoAction2_test.ioTestObj", new ArrayList<>() {
            {
                add("测试");
                add(100);
                add(new ArrayList<>() {
                    {
                        add("1");
                        add("2");
                        add("3");
                    }
                });
                add("ddddd");
            }
        }, null);
        Assertions.assertEquals("ddddd", result);
        result = ScriptProcessor.execute("test", "TodoAction2_test.ioTestMap", new ArrayList<>() {
            {
                add(new HashMap<>() {
                    {
                        put("xx", "xx");
                    }
                });
            }
        }, null);
        Assertions.assertEquals("xx", ((JsonObject) result).getString("xx"));
        Assertions.assertEquals("add", ((JsonObject) result).getString("add"));
        result = ScriptProcessor.execute("test", "TodoAction2_test.ioTestDto", new ArrayList<>() {
            {
                add(new HashMap<>() {
                    {
                        put("content", "xx");
                    }
                });
            }
        }, null);
        Assertions.assertEquals("xx", ((JsonObject) result).getString("content"));
        Assertions.assertEquals("100", ((JsonObject) result).getString("createUserId"));
        result = ScriptProcessor.execute("test", "TodoAction2_test.ioTestDtos", new ArrayList<>() {
            {
                add(new ArrayList<>() {
                    {
                        add(new HashMap<>() {
                            {
                                put("content", "xx");
                            }
                        });
                        add(new HashMap<>() {
                            {
                                put("content", "yy");
                            }
                        });
                    }
                });
            }
        }, null);
        Assertions.assertEquals("yy", ((JsonArray) result).getJsonObject(1).getString("content"));
        Assertions.assertEquals("100", ((JsonArray) result).getJsonObject(0).getString("createUserId"));
    }

}
