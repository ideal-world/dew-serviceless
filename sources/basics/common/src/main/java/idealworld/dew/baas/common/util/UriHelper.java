package idealworld.dew.baas.common.util;

import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * @author gudaoxuri
 */
public class UriHelper {

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

}
