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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import idealworld.dew.framework.domain.IdEntity;
import idealworld.dew.framework.dto.IdResp;
import idealworld.dew.framework.exception.BadRequestException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import lombok.SneakyThrows;

import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
public class ProcessHelper {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    public Future<Void> success() {
        return Future.succeededFuture();
    }

    public <E> Future<E> success(E result) {
        return Future.succeededFuture(result);
    }

    @SneakyThrows
    public <E extends Throwable> E error(E e) {
        throw e;
    }

    public <E extends IdResp, I extends IdEntity> Future<E> success(I entity, Class<E> clazz) {
        return Future.succeededFuture($.bean.copyProperties(entity, clazz));
    }

    public <E extends IdResp, I extends IdEntity> Future<List<E>> success(List<I> entities, Class<E> clazz) {
        return Future.succeededFuture(
                entities.stream()
                        .map(r -> $.bean.copyProperties(r, clazz))
                        .collect(Collectors.toList()));
    }

    public <E extends IdResp, I extends IdEntity> Future<Page<E>> success(Page<I> entities, Class<E> clazz) {
        var page = new Page<E>();
        page.setPageNumber(entities.getPageNumber());
        page.setPageSize(entities.getPageSize());
        page.setPageTotal(entities.getPageTotal());
        page.setRecordTotal(entities.getRecordTotal());
        page.setObjects(entities.getObjects().stream()
                .map(r -> $.bean.copyProperties(r, clazz))
                .collect(Collectors.toList()));
        return Future.succeededFuture(page);
    }

    public <E> E convert(Object bean, Class<E> clazz) {
        return $.bean.copyProperties(bean, clazz);
    }

    public <E> Future<E> existToError(Future<E> existFuture, Supplier<Throwable> errorFun) {
        return toError(existFuture, errorFun, false);
    }

    public <E> Future<E> notExistToError(Future<E> existFuture, Supplier<Throwable> errorFun) {
        return toError(existFuture, errorFun, true);
    }

    private <E> Future<E> toError(Future<E> existFuture, Supplier<Throwable> errorFun, Boolean needExist) {
        return existFuture
                .compose(exists -> {
                    if (exists instanceof Boolean) {
                        if (((Boolean) exists)) {
                            return needExist
                                    ? Future.succeededFuture(exists)
                                    : Future.failedFuture(errorFun.get());
                        } else {
                            return !needExist
                                    ? Future.succeededFuture(exists)
                                    : Future.failedFuture(errorFun.get());
                        }
                    } else {
                        if (exists != null) {
                            return needExist
                                    ? Future.succeededFuture(exists)
                                    : Future.failedFuture(errorFun.get());
                        } else {
                            return !needExist
                                    ? Future.succeededFuture(exists)
                                    : Future.failedFuture(errorFun.get());
                        }
                    }
                });
    }

    public <E> Future<List<E>> findWithRecursion(E initObj, Function<E, Future<List<E>>> recursionFun) {
        var result = new ArrayList<E>();
        Promise<List<E>> promise = Promise.promise();
        findWithRecursion(result, initObj, recursionFun, promise);
        return promise.future();
    }

    private <E> void findWithRecursion(List<E> result, E obj, Function<E, Future<List<E>>> recursionFun, Promise<List<E>> promise) {
        recursionFun.apply(obj)
                .onSuccess(objs -> {
                    if (objs.isEmpty()) {
                        promise.complete(result);
                    } else {
                        result.addAll(objs);
                        objs.forEach(o -> findWithRecursion(result, o, recursionFun, promise));
                    }
                })
                .onFailure(promise::fail);
    }


    static <E> E parseBody(Buffer body, Class<E> bodyClazz, String... excludeKeys) {
        if (bodyClazz == Void.class) {
            return null;
        }
        if (bodyClazz == Buffer.class) {
            return (E) body;
        }
        if (bodyClazz == String.class) {
            return (E) body.toString(StandardCharsets.UTF_8);
        }
        var jsonBody = Json.decodeValue(body);
        if (jsonBody instanceof JsonObject) {
            trimValues((JsonObject) jsonBody, Arrays.asList(excludeKeys));
            if (bodyClazz == JsonObject.class) {
                return (E) jsonBody;
            }
            var beanBody = ((JsonObject) jsonBody).mapTo(bodyClazz);
            var violations = VALIDATOR.validate(beanBody);
            if (violations.isEmpty()) {
                return beanBody;
            }
            var errorMsg = violations.stream()
                    .map(violation -> "[" + violation.getPropertyPath().toString() + "]" + violation.getMessage())
                    .collect(Collectors.joining("\n"));
            throw new BadRequestException(errorMsg);
        }
        if (jsonBody instanceof JsonArray) {
            if (bodyClazz == JsonArray.class) {
                return (E) jsonBody;
            }
            if (bodyClazz == List.class) {
                return (E) ((JsonArray) jsonBody).getList();
            }
            if (bodyClazz == Set.class) {
                return (E) new HashSet<>(((JsonArray) jsonBody).getList());
            }
        }
        throw new BadRequestException("无法将\"" + body.toString() + "\"转换成[" + bodyClazz.getName() + "]");
    }

    private static void trimValues(JsonObject json, List<String> excludeKeys) {
        json.stream()
                .filter(j -> !excludeKeys.contains(j.getKey()))
                .forEach(j -> {
                    if (j.getValue() instanceof JsonObject) {
                        trimValues((JsonObject) j.getValue(), excludeKeys.stream().map(k -> k.substring(j.getKey().length() + 1)).collect(Collectors.toList()));
                    } else if (j.getValue() instanceof JsonArray) {
                        ((JsonArray) j.getValue()).forEach(i -> {
                            if (j instanceof JsonObject) {
                                trimValues((JsonObject) i, excludeKeys.stream().map(k -> k.substring(j.getKey().length() + 1)).collect(Collectors.toList()));
                            }
                        });
                    } else if (j.getValue() instanceof String) {
                        json.put(j.getKey(), ((String) j.getValue()).trim());
                    }
                });
    }

}
