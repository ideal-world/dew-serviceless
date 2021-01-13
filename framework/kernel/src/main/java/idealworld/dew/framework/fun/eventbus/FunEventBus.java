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

import com.ecfront.dew.common.tuple.Tuple2;
import idealworld.dew.framework.DewConfig;
import idealworld.dew.framework.DewConstant;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.DewException;
import idealworld.dew.framework.util.URIHelper;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryContext;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class FunEventBus {

    private static final Map<String, FunEventBus> EVENT_BUS = new ConcurrentHashMap<>();
    private String code;
    protected EventBus eventBus;

    public static Future<Void> init(String code, Vertx vertx, DewConfig.FunConfig.EventBusConfig eventBusConfig) {
        var dewEventBus = new FunEventBus();
        dewEventBus.code = code;
        dewEventBus.eventBus = vertx.eventBus();
        dewEventBus.addInboundInterceptors().forEach(interceptor -> dewEventBus.eventBus.addInboundInterceptor(interceptor));
        dewEventBus.addOutboundInterceptors().forEach(interceptor -> dewEventBus.eventBus.addOutboundInterceptor(interceptor));
        EVENT_BUS.put(code, dewEventBus);
        return Future.succeededFuture();
    }

    public static Future<Void> destroy() {
        return Future.succeededFuture();
    }

    public static FunEventBus choose(String code) {
        return EVENT_BUS.get(code);
    }

    public static Boolean contains(String code) {
        return EVENT_BUS.containsKey(code);
    }

    public static void remove(String code) {
        EVENT_BUS.remove(code);
    }

    public <T> List<Handler<DeliveryContext<T>>> addInboundInterceptors() {
        return new ArrayList<>();
    }

    public <T> List<Handler<DeliveryContext<T>>> addOutboundInterceptors() {
        return new ArrayList<>();
    }

    public Future<Tuple2<Buffer, Map<String, String>>> request(String moduleName, OptActionKind actionKind, String uri, Buffer body, Map<String, String> header) {
        Promise<Tuple2<Buffer, Map<String, String>>> promise = Promise.promise();
        var deliveryOptions = new DeliveryOptions()
                .addHeader(DewConstant.REQUEST_RESOURCE_ACTION_FLAG, actionKind.toString())
                .addHeader(DewConstant.REQUEST_RESOURCE_URI_FLAG, uri);
        if (header != null) {
            header.forEach(deliveryOptions::addHeader);
        }
        eventBus.request(moduleName,
                body,
                deliveryOptions,
                (Handler<AsyncResult<Message<Buffer>>>) event -> {
                    if (event.failed()) {
                        log.error("[EventBus][{}]Request [{}]{}:{} error", code, moduleName, actionKind.toString(), uri);
                        promise.fail(event.cause());
                    } else {
                        var respHeader = new HashMap<String, String>();
                        event.result().headers().forEach(h -> respHeader.put(h.getKey(), h.getValue()));
                        promise.complete(new Tuple2<>(event.result().body(), respHeader));
                    }
                }
        );
        return promise.future();
    }

    public void publish(String moduleName, OptActionKind actionKind, String uri, Buffer body, Map<String, String> header) {
        var deliveryOptions = new DeliveryOptions()
                .addHeader(DewConstant.REQUEST_RESOURCE_ACTION_FLAG, actionKind.toString())
                .addHeader(DewConstant.REQUEST_RESOURCE_URI_FLAG, uri)
                .addHeader(DewConstant.REQUEST_WITHOUT_RESP_FLAG, "");
        if (header != null) {
            header.forEach(deliveryOptions::addHeader);
        }
        eventBus.publish(moduleName,
                body,
                deliveryOptions
        );
    }

    public void send(String moduleName, OptActionKind actionKind, String uri, Object body, Map<String, String> header) {
        var deliveryOptions = new DeliveryOptions()
                .addHeader(DewConstant.REQUEST_RESOURCE_ACTION_FLAG, actionKind.toString())
                .addHeader(DewConstant.REQUEST_RESOURCE_URI_FLAG, uri)
                .addHeader(DewConstant.REQUEST_WITHOUT_RESP_FLAG, "");
        if (header != null) {
            header.forEach(deliveryOptions::addHeader);
        }
        eventBus.send(moduleName,
                body,
                deliveryOptions
        );
    }

    public <E> void consumer(String moduleName, ConsumerFun<E> consumerFun) {
        eventBus.consumer(moduleName,
                (Handler<Message<Buffer>>) event -> {
                    var header = new HashMap<String, String>();
                    event.headers().forEach(h -> header.put(h.getKey(), h.getValue()));
                    var actionKind = OptActionKind.parse(event.headers().get(DewConstant.REQUEST_RESOURCE_ACTION_FLAG));
                    var uri = URIHelper.newURI(event.headers().get(DewConstant.REQUEST_RESOURCE_URI_FLAG));
                    log.trace("[EventBus][{}]Receive data [{}]{}", code, actionKind, uri.toString());
                    try {
                        var processF = consumerFun.consume(actionKind, uri, header, event.body());
                        if (!event.headers().contains(DewConstant.REQUEST_WITHOUT_RESP_FLAG)) {
                            processF
                                    .onSuccess(processResult -> {
                                        if (processResult instanceof Number
                                                || processResult instanceof Boolean) {
                                            event.reply(Buffer.buffer(processResult.toString(), "utf-8"));
                                        } else if (processResult instanceof String) {
                                            event.reply(Buffer.buffer((String) processResult, "utf-8"));
                                        } else if (processResult instanceof Buffer) {
                                            event.reply(processResult);
                                        } else if (processResult == null) {
                                            event.reply(Buffer.buffer());
                                        } else if (processResult instanceof List) {
                                            var array = new JsonArray();
                                            ((List<?>) processResult).forEach(p -> array.add(JsonObject.mapFrom(p)));
                                            event.reply(array.toBuffer());
                                        } else if (processResult instanceof JsonObject) {
                                            event.reply(((JsonObject) processResult).toBuffer());
                                        } else if (processResult instanceof JsonArray) {
                                            event.reply(((JsonArray) processResult).toBuffer());
                                        } else {
                                            event.reply(JsonObject.mapFrom(processResult).toBuffer());
                                        }
                                    });
                        }
                        processF
                                .onFailure(e -> {
                                    log.error("[EventBus][{}]Process [{}]{}:{} error", code, moduleName, actionKind.toString(), uri, e);
                                    if (e instanceof DewException) {
                                        event.fail(((DewException) e).getCode(), e.getMessage());
                                    } else {
                                        event.fail(-1, e.getMessage());
                                    }
                                });
                    } catch (Exception e) {
                        log.error("[EventBus][{}]Process [{}]{}:{} error", code, moduleName, actionKind.toString(), uri, e);
                        if (e instanceof DewException) {
                            event.fail(((DewException) e).getCode(), e.getMessage());
                        } else {
                            event.fail(-1, e.getMessage());
                        }
                    }
                }
        );
    }

    @FunctionalInterface
    public interface ConsumerFun<E> {

        Future<E> consume(OptActionKind actionKind, URI uri, Map<String, String> header, Buffer body);

    }

}
