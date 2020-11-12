package idealworld.dew.baas.common.auth;

import com.ecfront.dew.common.Resp;
import com.ecfront.dew.common.exception.RTException;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.funs.cache.RedisClient;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
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

    public static void loadRemoteResources(String filterResourceKind) {
        var scanKey = Constant.CACHE_AUTH_POLICY + (filterResourceKind == null ? "" : filterResourceKind + ":");
        RedisClient.choose("").scan(scanKey, key -> {
            var keyItems = key.substring(Constant.CACHE_AUTH_POLICY.length()).split(":");
            var resourceKind = keyItems[0];
            var resourceUri = resourceKind + "://" + keyItems[1];
            var actionKind = keyItems[2];
            if (!LOCAL_RESOURCES.containsKey(resourceKind)) {
                LOCAL_RESOURCES.put(resourceKind, new ConcurrentHashMap<>());
            }
            if (!LOCAL_RESOURCES.get(resourceKind).containsKey(actionKind)) {
                LOCAL_RESOURCES.get(resourceKind).put(actionKind, new CopyOnWriteArrayList<>());
            }
            try {
                if (!LOCAL_RESOURCES.get(resourceKind).get(actionKind).contains(new URI(resourceUri))) {
                    LOCAL_RESOURCES.get(resourceKind).get(actionKind).add(new URI(resourceUri));
                }
            } catch (URISyntaxException e) {
                log.error("[LocalResourceCache]Init local resource cache error: {}", e.getMessage(), e);
                throw new RTException(e);
            }
        });
    }

    public static Map<String, List<URI>> getResourceInfo(String resourceKind){
        return LOCAL_RESOURCES.getOrDefault(resourceKind,new HashMap<>());
    }

    public static Resp<Void> addLocalResource(URI resourceUri, String actionKind) {
        var resourceKind = resourceUri.getScheme();
        if (!LOCAL_RESOURCES.containsKey(resourceKind)) {
            LOCAL_RESOURCES.put(resourceKind, new ConcurrentHashMap<>());
        }
        if (!LOCAL_RESOURCES.get(resourceKind).containsKey(actionKind)) {
            LOCAL_RESOURCES.get(resourceKind).put(actionKind, new CopyOnWriteArrayList<>());
        }
        LOCAL_RESOURCES.get(resourceKind).get(actionKind).add(resourceUri);
        return Resp.success(null);
    }

    public static Resp<Void> removeLocalResource(URI resourceUri, String actionKind) {
        LOCAL_RESOURCES.getOrDefault(resourceUri.getScheme(), new HashMap<>()).getOrDefault(actionKind, new ArrayList<>()).remove(resourceUri);
        return Resp.success(null);
    }

}

