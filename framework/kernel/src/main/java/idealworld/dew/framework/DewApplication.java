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

package idealworld.dew.framework;

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.exception.RTException;
import com.ecfront.dew.common.exception.RTReflectiveOperationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

/**
 * 服务启动基础类.
 *
 * @param <C> 配置信息类
 * @author gudaoxuri
 */
@Slf4j
public abstract class DewApplication<C extends DewConfig> extends AbstractVerticle {

    private Class<C> configClazz = (Class<C>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    private C config;

    @Override
    public final void start(Promise<Void> startPromise) {
        prepare();
        log.info("[Startup]Starting {}", this.getClass().getName());
        log.info("[Startup]Loading config...");
        var config = loadConfig();
        this.config = config;
        log.info("[Startup]Loading custom operations...");
        var startF = start(config);
        startF.onSuccess(st -> {
            log.info("[Startup]Loading modules...");
            var modules = config.getModules();
            var loadModelsF = loadModules(modules);
            loadModelsF.onSuccess(moduleLoadResult -> {
                log.info("[Startup]Loading custom operations...");
                log.info("\r\n==============[Startup]==============\r\n" +
                        "The service has been started and contains the following modules:\r\n" +
                        modules.stream()
                                .filter(DewConfig.ModuleConfig::getEnabled)
                                .map(m -> ">>" + m.getClazzPackage())
                                .collect(Collectors.joining("\r\n")) +
                        "\r\n==============[Startup]==============");
                startPromise.complete();
            });
            loadModelsF.onFailure(e -> {
                log.error("[Startup]Start failure: {}", e.getMessage(), e);
                startPromise.fail(e);
            });
        });
        startF.onFailure(e -> {
            log.error("[Startup]Start failure: {}", e.getMessage(), e);
            startPromise.fail(e);
        });
    }

    protected abstract Future<?> start(C config);

    @Override
    public final void stop(Promise<Void> stopPromise) {
        log.info("[Shutdown]Stopping custom operations...");
        var stopF = stop(config);
        stopF.onComplete(stopResult -> {
            if (stopResult.succeeded()) {
                log.info("[Shutdown]Stopped");
                stopPromise.complete();
            } else {
                log.error("[Shutdown]Stop failure: {}", stopResult.cause().getMessage(), stopResult.cause());
                stopPromise.fail(stopResult.cause());
            }
        });
    }

    protected abstract Future<?> stop(C config);

    private void prepare() {
        ObjectMapper mapper = io.vertx.core.json.jackson.DatabindCodec.mapper();
        mapper.registerModule(new JavaTimeModule());
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

    public C loadConfig() {
        String config;
        if (System.getProperties().containsKey(DewConstant.PARAM_CONFIG)) {
            config = System.getProperty(DewConstant.PARAM_CONFIG);
        } else {
            config = $.file.readAllByClassPath("application-" + System.getProperty(DewConstant.PARAM_PROFILE_KEY) + ".yml", StandardCharsets.UTF_8);
            if (config == null) {
                config = $.file.readAllByClassPath("application-" + System.getProperty(DewConstant.PARAM_PROFILE_KEY) + ".yaml",
                        StandardCharsets.UTF_8);
            }
            if (config == null) {
                throw new RTException("[Startup]Configuration file [" + "application-" + System.getProperty(DewConstant.PARAM_PROFILE_KEY) + ".yml" +
                        "/yaml" + "] not found in classpath");
            }
        }
        var configMap = (Map) YamlHelper.toObject(config);
        System.getProperties().entrySet().stream()
                .filter(p -> p.getKey().toString().startsWith(DewConstant.PARAM_CONFIG_ITEM_PREFIX))
                .forEach(p -> {
                    var configKey = p.getKey().toString().substring(DewConstant.PARAM_CONFIG_ITEM_PREFIX.length());
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
            if (!(((Map) moduleConfig).containsKey("config"))) {
                ((Map) (moduleConfig)).put("config", new LinkedHashMap<>());
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
