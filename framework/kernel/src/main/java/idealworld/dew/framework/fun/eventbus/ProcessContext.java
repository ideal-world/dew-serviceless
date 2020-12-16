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
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessContext {

    public ProcessContext init() {
        fun.sql.addEntityByInsertFun = o -> ProcessHelper.addSafeInfo((IdEntity) o, true, this);
        fun.sql.addEntityByUpdateFun = o -> ProcessHelper.addSafeInfo((IdEntity) o, false, this);
        return this;
    }

    @Builder.Default
    public Request req = new Request();
    @Builder.Default
    public Function fun = new Function();
    @Builder.Default
    public ProcessHelper helper = new ProcessHelper(this);
    public Object conf;
    public String moduleName;

    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {

        public Map<String, String> params;
        public Map<String, String> header;
        private Buffer body;
        public IdentOptCacheInfo identOptInfo;

        public Long pageNumber(){
           return Long.parseLong(params.get("pageNumber"));
        }

        public Long pageSize(){
            return Long.parseLong(params.get("pageSize"));
        }

        public <E> E body(Class<E> clazz) {
            return ProcessHelper.parseBody(body, clazz);
        }

    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Function {

        public FunSQLClient sql;
        public FunRedisClient cache;
        public FunHttpClient http;
        public FunEventBus eb;

    }


}
