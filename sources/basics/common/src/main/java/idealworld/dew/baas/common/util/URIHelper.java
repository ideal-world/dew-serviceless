package idealworld.dew.baas.common.util;

import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
public class URIHelper {

    public static String formatUri(URI uri) {
        var query = "";
        if (uri.getQuery() != null) {
            query = Arrays.stream(uri.getQuery().split("&"))
                    .sorted(Comparator.comparing(u -> u.split("=")[0]))
                    .collect(Collectors.joining("&"));
        }
        return uri.getScheme()
                + "//"
                + uri.getHost()
                + (uri.getPort() != -1 ? ":" + uri.getPort() : "")
                + uri.getPath()
                + (uri.getQuery() != null ? "?" + query : "");
    }

    public static Map<String, String> getSingleValueQuery(String query) {
        if (query == null) {
            return new HashMap<>();
        }
        return Arrays.stream(query.split("&"))
                .map(i -> i.split("="))
                .collect(Collectors.toMap(i -> i[0], i -> i.length > 1 ? i[1] : ""));
    }

}
