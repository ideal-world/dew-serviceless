/*
 * Copyright 2020. the original author or authors.
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

package idealworld.dew.baas.common.controller;

import com.ecfront.dew.common.tuple.Tuple2;
import group.idealworld.dew.Dew;
import idealworld.dew.baas.common.dto.IdentOptInfo;
import idealworld.dew.baas.common.resp.StandardResp;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Basic controller.
 *
 * @author gudaoxuri
 */
public abstract class BasicController {

    protected Tuple2<Long, Long> getCurrentAppAndTenantId() {
        return Dew.auth.getOptInfo()
                .map(info -> new Tuple2<>(((IdentOptInfo) info).getAppId(), ((IdentOptInfo) info).getTenantId()))
                .orElseThrow(() -> StandardResp.e(
                        StandardResp.unAuthorized("BASIC", "用户未登录")
                ));
    }

    protected Long getCurrentTenantId() {
        return Dew.auth.getOptInfo()
                .map(info -> ((IdentOptInfo) info).getTenantId())
                .orElseThrow(() -> StandardResp.e(
                        StandardResp.unAuthorized("BASIC", "用户未登录")
                ));
    }

    /**
     * Export response entity.
     *
     * @param file     the file
     * @param fileName the file name
     * @return the response entity
     * @throws IOException the io exception
     */
    protected ResponseEntity<FileSystemResource> export(File file, String fileName) throws IOException {
        if (file == null) {
            return null;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Content-Disposition", "attachment; filename=" + fileName);
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        return ResponseEntity
                .ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType(Files.probeContentType(file.toPath())))
                .body(new FileSystemResource(file));
    }

}
