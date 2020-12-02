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

package idealworld.dew.serviceless.common.util;

import lombok.SneakyThrows;

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
        if (uri.getSchemeSpecificPart() != null && !uri.getSchemeSpecificPart().isBlank()) {
            // E.g. jdbc:h2:men:iam 不用解析
            return uri.toString();
        }
        var query = "";
        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            query = Arrays.stream(uri.getQuery().split("&"))
                    .sorted(Comparator.comparing(u -> u.split("=")[0]))
                    .collect(Collectors.joining("&"));
        }
        var path = uri.getPath().isBlank()
                ? ""
                : uri.getPath().endsWith("/")
                ? uri.getPath().substring(0, uri.getPath().length() - 1)
                : uri.getPath();
        return uri.getScheme()
                + "://"
                + uri.getHost()
                + (uri.getPort() != -1 ? ":" + uri.getPort() : "")
                + path
                + (uri.getQuery() != null ? "?" + query : "");
    }

    @SneakyThrows
    public static String formatUri(String host, String pathAndQuery) {
        if (!pathAndQuery.startsWith("/")) {
            pathAndQuery = "/" + pathAndQuery;
        }
        if (host.endsWith("/")) {
            host = host.substring(0, host.length() - 1);
        }
        var uri = new URI(host + pathAndQuery);
        return formatUri(uri);
    }

    @SneakyThrows
    public static String formatUri(String strUri) {
        var uri = new URI(strUri);
        return formatUri(uri);
    }

    @SneakyThrows
    public static String getPathAndQuery(String strUri) {
        var uri = new URI(strUri);
        return (uri.getPath().isBlank() ? "/" : uri.getPath())
                + (uri.getQuery() != null ? "?" + uri.getQuery() : "");
    }

    public static Map<String, String> getSingleValueQuery(String query) {
        if (query == null) {
            return new HashMap<>();
        }
        return Arrays.stream(query.split("&"))
                .map(i -> i.split("="))
                .collect(Collectors.toMap(i -> i[0].toLowerCase(), i -> i.length > 1 ? i[1] : ""));
    }

    @SneakyThrows
    public static URI newURI(String strUri) {
        return new URI(strUri);
    }
}
