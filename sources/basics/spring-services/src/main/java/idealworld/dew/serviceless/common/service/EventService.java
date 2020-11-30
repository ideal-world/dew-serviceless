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

package idealworld.dew.serviceless.common.service;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Resp;
import group.idealworld.dew.Dew;
import idealworld.dew.serviceless.common.resp.StandardResp;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public interface EventService {

    Map<String, List<Function<Object, Resp<Void>>>> PRE_CHECK_SUBS = new HashMap<>();
    Map<String, List<Consumer<Object>>> POST_NOTIFY_SUBS = new HashMap<>();

    Logger log();

    String topicName();

    default void subscribePreCheckEvent(ActionKind actionKind, String subjectCate, Function<Object, Resp<Void>> fun) {
        PRE_CHECK_SUBS.putIfAbsent(actionKind.toString() + "#" + subjectCate, new ArrayList<>());
        PRE_CHECK_SUBS.get(actionKind.toString() + "#" + subjectCate).add(fun);
    }

    default void subscribePostNotifyEvent(ActionKind actionKind, String subjectCate, Consumer<Object> fun) {
        POST_NOTIFY_SUBS.putIfAbsent(actionKind.toString() + "#" + subjectCate, new ArrayList<>());
        POST_NOTIFY_SUBS.get(actionKind.toString() + "#" + subjectCate).add(fun);
    }

    default Resp<Void> checkEventByJVM(ActionKind actionKind, String subjectCate, Object subjectId) {
        var funs = PRE_CHECK_SUBS.getOrDefault(actionKind.toString() + "#" + subjectCate, new ArrayList<>());
        for (Function<Object, Resp<Void>> fun : funs) {
            var result = fun.apply(subjectId);
            if (!result.ok()) {
                return result;
            }
        }
        return Resp.success(null);
    }

    default void notifyEventByJVM(ActionKind actionKind, String subjectCate, Object subjectId) {
        POST_NOTIFY_SUBS.getOrDefault(actionKind.toString() + "#" + subjectCate, new ArrayList<>())
                .forEach(fun -> fun.accept(subjectId));
    }

    default void notifyEventByMQ(ActionKind actionKind, String subjectCate, Object subjectId) {
        var topicName = topicName();
        if (topicName.isEmpty()) {
            return;
        }
        var content = $.json.toJsonString(
                DewEvent.builder()
                        .actionKind(actionKind)
                        .subjectCate(subjectCate)
                        .subjectId(subjectId)
                        .build());
        log().trace("Noify Event {}:{}", topicName, content);
        Dew.cluster.mq.publish(topicName, content);
    }

    @Data
    @Builder
    class DewEvent {

        @Enumerated(EnumType.STRING)
        private ActionKind actionKind;
        private String subjectCate;
        private Object subjectId;

    }

    enum ActionKind {

        CREATE("CREATE"),
        MODIFY("MODIFY"),
        DELETE("DELETE");

        private final String code;

        ActionKind(String code) {
            this.code = code;
        }

        public static ActionKind parse(String code) {
            return Arrays.stream(ActionKind.values())
                    .filter(item -> item.code.equalsIgnoreCase(code))
                    .findFirst()
                    .orElseThrow(() -> StandardResp.e(
                            StandardResp.badRequest("BASIC",
                                    "Action kind {" + code + "} NOT exist.")));
        }

        @Override
        public String toString() {
            return code;
        }
    }

}
