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

package idealworld.dew.framework;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.exception.RTIOException;
import com.ecfront.dew.common.exception.RTReflectiveOperationException;
import idealworld.dew.framework.util.YamlHelper;
import io.vertx.core.*;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class DewApplication<C extends DewConfig> extends AbstractVerticle {

    private Class<C> configClazz = (Class<C>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    private C config;

    protected abstract Future<?> start(C config);

    protected abstract Future<?> stop(C config);

    @Override
    public final void start(Promise<Void> startPromise) {
        log.info("[Startup]Starting {}", this.getClass().getName());
        log.info("[Startup]Loading config...");
        var config = loadConfig();
        this.config = config;
        log.info("[Startup]Loading modules...");
        loadModules(config.getModules())
                .onSuccess(moduleLoadResult -> {
                    log.info("[Startup]Loading custom operations...");
                    start(config)
                            .onSuccess(st -> {
                                log.info("[Startup]Started");
                                startPromise.complete();
                            })
                            .onFailure(e -> {
                                log.error("[Startup]Start failure: {}", e.getMessage(), e);
                                startPromise.fail(e);
                            });
                })
                .onFailure(e -> {
                    log.error("[Startup]Start failure: {}", e.getMessage(), e);
                    startPromise.fail(e);
                });
    }

    @Override
    public final void stop(Promise<Void> stopPromise) {
        var unDeployModules = vertx.deploymentIDs().stream()
                .map(id -> (Future) vertx.undeploy(id))
                .collect(Collectors.toList());
        CompositeFuture.all(unDeployModules)
                .onComplete(moduleUnLoadResult -> {
                    if (moduleUnLoadResult.failed()) {
                        log.error("[Shutdown]Stop failure: {}", moduleUnLoadResult.cause().getMessage(), moduleUnLoadResult.cause());
                    }
                    log.info("[Shutdown]Stopping custom operations...");
                    stop(config)
                            .onComplete(stopResult -> {
                                if (stopResult.succeeded()) {
                                    log.info("[Shutdown]Stopped");
                                    stopPromise.complete();
                                } else {
                                    log.error("[Shutdown]Stop failure: {}", stopResult.cause().getMessage(), stopResult.cause());
                                    stopPromise.fail(stopResult.cause());
                                }

                            });

                });
    }

    private CompositeFuture loadModules(List<DewConfig.ModuleConfig> moduleConfig) {
        var deployModules = moduleConfig.stream()
                .filter(DewConfig.ModuleConfig::getEnabled)
                .map(conf -> {
                    try {
                        var module = (DewModule<?>) Class.forName(conf.getClazzPackage()).getDeclaredConstructor().newInstance();
                        var deploymentOptions = new DeploymentOptions();
                        if (conf.getInstance() != null) {
                            deploymentOptions.setInstances(conf.getInstance());
                        }
                        if (conf.getHa() != null && conf.getHa()) {
                            deploymentOptions.setHa(true);
                        }
                        if (conf.getWork() != null && conf.getWork()) {
                            deploymentOptions.setWorker(true);
                            deploymentOptions.setWorkerPoolName(module.getClass().getSimpleName() + "-pool");
                            if (conf.getWorkPoolSize() != null) {
                                deploymentOptions.setWorkerPoolSize(conf.getWorkPoolSize());
                            }
                            if (conf.getWorkMaxTimeMs() != null) {
                                deploymentOptions.setMaxWorkerExecuteTimeUnit(TimeUnit.MILLISECONDS);
                                deploymentOptions.setMaxWorkerExecuteTime(conf.getWorkMaxTimeMs());
                            }
                        }
                        deploymentOptions.setConfig(new JsonObject().put("config", conf.getConfig()).put("funs", JsonObject.mapFrom(conf.getFuns())));
                        log.info("[Startup]Loading module: {}", conf.getClazzPackage());
                        return (Future) vertx.deployVerticle(module, deploymentOptions);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                        throw new RTReflectiveOperationException("[Startup]Module load failure : " + e.getMessage(), e);
                    }
                })
                .collect(Collectors.toList());
        return CompositeFuture.all(deployModules);
    }

    private C loadConfig() {
        String config;
        try {
            config = $.file.readAllByClassPath("application-" + System.getProperty(DewConstant.PARAM_PROFILE_KEY) + ".yml", StandardCharsets.UTF_8);
        } catch (RTIOException | NullPointerException ignore) {
            try {
                config = $.file.readAllByClassPath("application-" + System.getProperty(DewConstant.PARAM_PROFILE_KEY) + ".yaml", StandardCharsets.UTF_8);
            } catch (RTIOException | NullPointerException e) {
                log.error("[Startup]Configuration file [{}] not found in classpath", "application-" + System.getProperty(DewConstant.PARAM_PROFILE_KEY) + ".yml/yaml");
                throw e;
            }
        }
        var configMap = (Map) YamlHelper.toObject(config);
        System.getProperties().entrySet().stream()
                .filter(p -> p.getKey().toString().startsWith(DewConstant.PARAM_CONFIG_PREFIX))
                .forEach(p -> {
                    var configKey = p.getKey().toString().substring(DewConstant.PARAM_CONFIG_PREFIX.length());
                    var configObj = configMap;
                    while (configKey.contains(".")) {
                        var prefix = configKey.substring(0, configKey.indexOf("."));
                        configObj = (Map) configObj.get(prefix);
                        configKey = configKey.substring(configKey.indexOf(".") + 1);
                    }
                    configObj.put(configKey, p.getValue());
                });
        ((List) configMap.get("modules")).forEach(moduleConfig -> {
            if (!(((Map) moduleConfig).containsKey("funs"))) {
                ((Map) (moduleConfig)).put("funs", configMap.get("funs"));
            } else {
                var mergedConfig = mergeItems((Map) (configMap.get("funs")), (Map) (((Map) moduleConfig).get("funs")));
                ((Map) (moduleConfig)).put("funs", mergedConfig);
            }
        });
        return JsonObject.mapFrom(configMap).mapTo(configClazz);
    }

    private static Map mergeItems(Map source, Map target) {
        target.forEach((k, v) -> {
            if (source.containsKey(k) && v instanceof LinkedHashMap) {
                // 如果源map和目标map都存在，并且存在子项目，递归合并
                // 并且存在子项目，递归合并
                target.put(k, mergeItems((LinkedHashMap) source.get(k), (LinkedHashMap) v));
            }
            // 否则不合并，即使用target的原始值
        });
        source.forEach((k, v) -> {
            if (!target.containsKey(k)) {
                // 添加 源map存在，目标map不存在的项目
                target.put(k, v);
            }
        });
        return target;
    }

}
