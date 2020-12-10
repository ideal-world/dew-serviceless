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

import com.ecfront.dew.common.Resp;
import idealworld.dew.framework.dto.OptActionKind;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

import java.net.URI;
import java.util.Map;

@FunctionalInterface
public interface ConsumerFun {

    Future<Resp<?>> consume(OptActionKind actionKind, URI uri, Map<String, String> header, Buffer body);


}
