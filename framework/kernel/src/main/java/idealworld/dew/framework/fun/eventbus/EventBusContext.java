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

import idealworld.dew.framework.dto.IdentOptExchangeInfo;
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
public class EventBusContext {

    public EventBusContext init() {
        context.init(req.identOptInfo);
        return this;
    }

    @Builder.Default
    public Request req = new Request();
    @Builder.Default
    public ProcessContext context = new ProcessContext();

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
        public IdentOptExchangeInfo identOptInfo = IdentOptExchangeInfo.builder().build();

        public Long pageNumber() {
            return Long.parseLong(params.getOrDefault("pageNumber", "1"));
        }

        public Long pageSize() {
            return Long.parseLong(params.getOrDefault("pageSize", "10"));
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

}
