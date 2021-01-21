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

package idealworld.dew.framework.util;

import lombok.SneakyThrows;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * URI处理辅助类.
 *
 * @author gudaoxuri
 */
public class URIHelper {

    public static String formatUri(URI uri) {
        if (uri.getHost() == null) {
            // E.g. jdbc:h2:men:iam 不用解析
            return uri.toString();
        }
        var query = sortQuery(uri.getRawQuery());
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
                + (uri.getRawQuery() != null ? "?" + query : "");
    }

    public static String sortQuery(String query) {
        if (query != null && !query.isBlank()) {
            return Arrays.stream(query.split("&"))
                    .sorted(Comparator.comparing(u -> u.split("=")[0]))
                    .collect(Collectors.joining("&"));
        }
        return query;
    }

    @SneakyThrows
    public static String formatUri(String host, String pathAndQuery) {
        if (!pathAndQuery.isBlank() && !pathAndQuery.startsWith("/")) {
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
        return getPathAndQuery(new URI(strUri));
    }

    public static String getPathAndQuery(URI uri) {
        var path = uri.getPath().isBlank()
                ? ""
                : uri.getPath().endsWith("/")
                ? uri.getPath().substring(0, uri.getPath().length() - 1)
                : uri.getPath();
        return path
                + (uri.getRawQuery() != null ? "?" + uri.getRawQuery() : "");
    }

    public static Map<String, String> getSingleValueQuery(String query) {
        return getSingleValueQuery(query, false);
    }

    public static Map<String, String> getSingleValueQuery(String query, Boolean toLowerCase) {
        if (query == null) {
            return new HashMap<>();
        }
        return Arrays.stream(query.split("&"))
                .map(i -> i.split("="))
                .collect(Collectors.toMap(i -> toLowerCase ? i[0].toLowerCase() : i[0], i -> i.length > 1 ? URLDecoder.decode(i[1], StandardCharsets.UTF_8) : ""));
    }

    @SneakyThrows
    public static URI newURI(String strUri) {
        return new URI(strUri);
    }
}
