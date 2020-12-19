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

package idealworld.dew.framework.fun.eventbus;

import idealworld.dew.framework.domain.IdEntity;
import idealworld.dew.framework.dto.IdentOptCacheInfo;
import idealworld.dew.framework.fun.cache.FunRedisClient;
import idealworld.dew.framework.fun.httpclient.FunHttpClient;
import idealworld.dew.framework.fun.sql.FunSQLClient;
import io.vertx.core.buffer.Buffer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * @author gudaoxuri
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessContext {

    public ProcessContext init() {
        helper = new ProcessHelper(this);
        if (FunSQLClient.contains(moduleName)) {
            fun.sql = FunSQLClient.choose(moduleName);
            if (fun.sql.addEntityByInsertFun == null) {
                fun.sql.addEntityByInsertFun = o -> ProcessHelper.addSafeInfo((IdEntity) o, true, this);
            }
            if (fun.sql.addEntityByUpdateFun == null) {
                fun.sql.addEntityByUpdateFun = o -> ProcessHelper.addSafeInfo((IdEntity) o, false, this);
            }
        }
        if (FunRedisClient.contains(moduleName)) {
            fun.cache = FunRedisClient.choose(moduleName);
        }
        if (FunHttpClient.contains(moduleName)) {
            fun.http = FunHttpClient.choose(moduleName);
        }
        if (FunEventBus.contains(moduleName)) {
            fun.eb = FunEventBus.choose(moduleName);
        }
        return this;
    }

    @Builder.Default
    public Request req = new Request();
    @Builder.Default
    public Function fun = new Function();
    public ProcessHelper helper;
    public Object conf;
    public String moduleName;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        @Builder.Default
        public Map<String, String> params = new HashMap<>();
        @Builder.Default
        public Map<String, String> header = new HashMap<>();
        private Object body;
        @Builder.Default
        public IdentOptCacheInfo identOptInfo = IdentOptCacheInfo.builder().build();

        public Long pageNumber() {
            return Long.parseLong(params.get("pageNumber"));
        }

        public Long pageSize() {
            return Long.parseLong(params.get("pageSize"));
        }

        public <E> E body(Class<E> clazz) {
            if (body == null) {
                return null;
            } else if (body instanceof Buffer) {
                return ProcessHelper.parseBody((Buffer) body, clazz);
            }
            return (E) body;
        }

    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class Function {

        public FunSQLClient sql;
        public FunRedisClient cache;
        public FunHttpClient http;
        public FunEventBus eb;

    }


}
