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

package idealworld.dew.framework.fun.auth;

import idealworld.dew.framework.dto.OptActionKind;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 鉴权策略.
 * <p>
 * Redis格式：
 * <p>
 * 资源类型:资源URI:资源操作类型 = {权限主体运算类型:{权限主体类型:[权限主体Id]}}
 *
 * @author gudaoxuri
 */
@Slf4j
public class LocalResourceCache {

    // resourceKind -> actionKind -> uris
    private static final Map<String, Map<String, List<URI>>> LOCAL_RESOURCES = new ConcurrentHashMap<>();

    public static Map<String, List<URI>> getResourceInfo(String resourceKind) {
        return LOCAL_RESOURCES.getOrDefault(resourceKind, new HashMap<>());
    }

    public static void addLocalResource(URI resourceUri, String actionKind) {
        var resourceKind = resourceUri.getScheme();
        if (!LOCAL_RESOURCES.containsKey(resourceKind)) {
            LOCAL_RESOURCES.put(resourceKind, new ConcurrentHashMap<>());
        }
        if (actionKind == null || actionKind.equalsIgnoreCase("")) {
            addLocalResource(resourceKind, resourceUri, OptActionKind.CREATE.toString().toLowerCase());
            addLocalResource(resourceKind, resourceUri, OptActionKind.MODIFY.toString().toLowerCase());
            addLocalResource(resourceKind, resourceUri, OptActionKind.PATCH.toString().toLowerCase());
            addLocalResource(resourceKind, resourceUri, OptActionKind.EXISTS.toString().toLowerCase());
            addLocalResource(resourceKind, resourceUri, OptActionKind.FETCH.toString().toLowerCase());
            addLocalResource(resourceKind, resourceUri, OptActionKind.DELETE.toString().toLowerCase());
        } else {
            addLocalResource(resourceKind, resourceUri, actionKind);
        }
    }

    private static void addLocalResource(String resourceKind, URI resourceUri, String actionKind) {
        if (!LOCAL_RESOURCES.get(resourceKind).containsKey(actionKind)) {
            LOCAL_RESOURCES.get(resourceKind).put(actionKind, new CopyOnWriteArrayList<>());
        }
        LOCAL_RESOURCES.get(resourceKind).get(actionKind).add(resourceUri);
    }

    public static void removeLocalResource(URI resourceUri, String actionKind) {
        if (actionKind == null || actionKind.equalsIgnoreCase("")) {
            LOCAL_RESOURCES.getOrDefault(resourceUri.getScheme(), new HashMap<>()).getOrDefault(OptActionKind.CREATE.toString().toLowerCase(), new ArrayList<>()).remove(resourceUri);
            LOCAL_RESOURCES.getOrDefault(resourceUri.getScheme(), new HashMap<>()).getOrDefault(OptActionKind.MODIFY.toString().toLowerCase(), new ArrayList<>()).remove(resourceUri);
            LOCAL_RESOURCES.getOrDefault(resourceUri.getScheme(), new HashMap<>()).getOrDefault(OptActionKind.PATCH.toString().toLowerCase(), new ArrayList<>()).remove(resourceUri);
            LOCAL_RESOURCES.getOrDefault(resourceUri.getScheme(), new HashMap<>()).getOrDefault(OptActionKind.EXISTS.toString().toLowerCase(), new ArrayList<>()).remove(resourceUri);
            LOCAL_RESOURCES.getOrDefault(resourceUri.getScheme(), new HashMap<>()).getOrDefault(OptActionKind.FETCH.toString().toLowerCase(), new ArrayList<>()).remove(resourceUri);
            LOCAL_RESOURCES.getOrDefault(resourceUri.getScheme(), new HashMap<>()).getOrDefault(OptActionKind.DELETE.toString().toLowerCase(), new ArrayList<>()).remove(resourceUri);
        } else {
            LOCAL_RESOURCES.getOrDefault(resourceUri.getScheme(), new HashMap<>()).getOrDefault(actionKind.toLowerCase(), new ArrayList<>()).remove(resourceUri);
        }
    }

}

