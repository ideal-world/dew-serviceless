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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import com.ecfront.dew.common.Resp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP响应处理.
 *
 * @author gudaoxuri
 */
public abstract class ResponseProcessor {

    /**
     * Post.
     *
     * @param url  the url
     * @param body the body
     * @return the resp
     */
    public Resp<Long> post(String url, Object body) {
        var response = exchange("POST", url, body, new HashMap<>());
        return Resp.generic(response, Long.class);
    }

    /**
     * Post to entity.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param body          the body
     * @param responseClazz the response clazz
     * @return the resp
     */
    public <E> Resp<E> postToEntity(String url, Object body, Class<E> responseClazz) {
        var response = exchange("POST", url, body, new HashMap<>());
        return Resp.generic(response, responseClazz);
    }

    /**
     * Post to list.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param body          the body
     * @param responseClazz the response clazz
     * @return the resp
     */
    public <E> Resp<List<E>> postToList(String url, Object body, Class<E> responseClazz) {
        var response = exchange("POST", url, body, new HashMap<>());
        return Resp.genericList(response, responseClazz);
    }

    /**
     * Post to page.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param body          the body
     * @param responseClazz the response clazz
     * @return the resp
     */
    public <E> Resp<Page<E>> postToPage(String url, Object body, Class<E> responseClazz) {
        var response = exchange("POST", url, body, new HashMap<>());
        return Resp.genericPage(response, responseClazz);
    }

    /**
     * Patch.
     *
     * @param url  the url
     * @param body the body
     * @return the resp
     */
    public Resp<Void> patch(String url, Object body) {
        var response = exchange("PATCH", url, body, new HashMap<>());
        return Resp.generic(response, Void.class);
    }

    /**
     * Patch to entity.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param body          the body
     * @param responseClazz the response clazz
     * @return the resp
     */
    public <E> Resp<E> patchToEntity(String url, Object body, Class<E> responseClazz) {
        var response = exchange("PATCH", url, body, new HashMap<>());
        return Resp.generic(response, responseClazz);
    }

    /**
     * Patch to list.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param body          the body
     * @param responseClazz the response clazz
     * @return the resp
     */
    public <E> Resp<List<E>> patchToList(String url, Object body, Class<E> responseClazz) {
        var response = exchange("PATCH", url, body, new HashMap<>());
        return Resp.genericList(response, responseClazz);
    }

    /**
     * Patch to page.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param body          the body
     * @param responseClazz the response clazz
     * @return the resp
     */
    public <E> Resp<Page<E>> patchToPage(String url, Object body, Class<E> responseClazz) {
        var response = exchange("PATCH", url, body, new HashMap<>());
        return Resp.genericPage(response, responseClazz);
    }

    /**
     * Gets to entity.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param responseClazz the response clazz
     * @return the to entity
     */
    public <E> Resp<E> getToEntity(String url, Class<E> responseClazz) {
        var response = exchange("GET", url, null, new HashMap<>());
        return Resp.generic(response, responseClazz);
    }

    /**
     * Gets to entity.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param responseClazz the response clazz
     * @param cacheSec      the cache sec
     * @return the to entity
     */
    public <E> Resp<E> getToEntity(String url, Class<E> responseClazz, int cacheSec) {
        var response = CacheHelper.getSet(url, cacheSec, () -> exchange("GET", url, null, new HashMap<>()));
        return Resp.generic(response, responseClazz);
    }

    /**
     * Gets to entity.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param header        the header
     * @param responseClazz the response clazz
     * @return the to entity
     */
    public <E> Resp<E> getToEntity(String url, Map<String, String> header, Class<E> responseClazz) {
        var response = exchange("GET", url, null, header);
        return Resp.generic(response, responseClazz);
    }

    /**
     * Gets to entity.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param header        the header
     * @param responseClazz the response clazz
     * @param cacheSec      the cache sec
     * @return the to entity
     */
    public <E> Resp<E> getToEntity(String url, Map<String, String> header, Class<E> responseClazz, int cacheSec) {
        var response = CacheHelper.getSet(url + "|" + $.json.toJsonString(header),
                cacheSec, () -> exchange("GET", url, null, header));
        return Resp.generic(response, responseClazz);
    }

    /**
     * Gets to list.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param responseClazz the response clazz
     * @return the to list
     */
    public <E> Resp<List<E>> getToList(String url, Class<E> responseClazz) {
        var response = exchange("GET", url, null, new HashMap<>());
        return Resp.genericList(response, responseClazz);
    }

    /**
     * Gets to list.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param responseClazz the response clazz
     * @param cacheSec      the cache sec
     * @return the to list
     */
    public <E> Resp<List<E>> getToList(String url, Class<E> responseClazz, int cacheSec) {
        var response = CacheHelper.getSet(url,
                cacheSec, () -> exchange("GET", url, null, new HashMap<>()));
        return Resp.genericList(response, responseClazz);
    }

    /**
     * Gets to page.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param pageNumber    the page number
     * @param pageSize      the page size
     * @param responseClazz the response clazz
     * @return the to page
     */
    public <E> Resp<Page<E>> getToPage(String url, Long pageNumber, Integer pageSize, Class<E> responseClazz) {
        if (url.contains("?")) {
            url += "&pageNumber=" + pageNumber + "&pageSize=" + pageSize;
        } else {
            url += "?pageNumber=" + pageNumber + "&pageSize=" + pageSize;
        }
        var response = exchange("GET", url, null, new HashMap<>());
        return Resp.genericPage(response, responseClazz);
    }

    /**
     * Gets to page.
     *
     * @param <E>           the type parameter
     * @param url           the url
     * @param pageNumber    the page number
     * @param pageSize      the page size
     * @param responseClazz the response clazz
     * @param cacheSec      the cache sec
     * @return the to page
     */
    public <E> Resp<Page<E>> getToPage(String url, Long pageNumber, Integer pageSize, Class<E> responseClazz, int cacheSec) {
        if (url.contains("?")) {
            url += "&pageNumber=" + pageNumber + "&pageSize=" + pageSize;
        } else {
            url += "?pageNumber=" + pageNumber + "&pageSize=" + pageSize;
        }
        String finalUrl = url;
        var response = CacheHelper.getSet(finalUrl,
                cacheSec, () -> exchange("GET", finalUrl, null, new HashMap<>()));
        return Resp.genericPage(response, responseClazz);
    }

    /**
     * Delete.
     *
     * @param url the url
     * @return the resp
     */
    public Resp<Void> delete(String url) {
        var response = exchange("DELETE", url, null, new HashMap<>());
        return Resp.generic(response, Void.class);
    }

    /**
     * Exchange.
     *
     * @param method the method
     * @param url    the url
     * @param body   the body
     * @param header the header
     * @return the resp
     */
    public abstract Resp<?> exchange(String method, String url, Object body, Map<String, String> header);

}
