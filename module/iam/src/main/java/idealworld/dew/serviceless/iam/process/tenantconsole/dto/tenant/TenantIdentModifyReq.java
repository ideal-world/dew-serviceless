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

package idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 修改租户认证配置请求.
 *
 * @author gudaoxuri
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantIdentModifyReq implements Serializable {

    // 认证AK校验正则规则说明
    @Size(max = 2000)
    private String validAkRuleNote;
    // 认证AK校验正则规则
    @Size(max = 2000)
    private String validAkRule;
    // 认证SK校验正则规则说明
    @Size(max = 2000)
    private String validSkRuleNote;
    // 认证SK校验正则规则
    @Size(max = 2000)
    private String validSkRule;
    // 认证有效时间（秒）
    private Long validTimeSec;

}
