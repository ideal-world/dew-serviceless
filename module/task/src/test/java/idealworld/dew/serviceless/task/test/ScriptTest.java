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
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
public class ScriptTest {

    @Test
    public void testScript(){
        String requirejs = new BufferedReader(new InputStreamReader(TaskProcessor.class.getResourceAsStream("/requirejs.js")))
                .lines().collect(Collectors.joining("\n"));
        String dewSDK = new BufferedReader(new InputStreamReader(TaskProcessor.class.getResourceAsStream("/DewSDK_browserify.js")))
                .lines().collect(Collectors.joining("\n"));
        ScriptProcessor.init("http://127.0.0.1:9000",requirejs, dewSDK);
        ScriptProcessor.add(1L,"test1","await DewSDK.cache.del('test-key')");
        ScriptProcessor.add(1L,"test2","console.log(await DewSDK.cache.exists('test-key'))");
        ScriptProcessor.add(1L,"test3","1+1");
        try{
            ScriptProcessor.execute(1L,"test1");
            ScriptProcessor.execute(1L,"test2");
            ScriptProcessor.execute(1L,"test3");
        }catch (Exception e){
            System.out.println(e);
        }

    }

}
