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

package idealworld.dew.baas.common;

import com.ecfront.dew.common.$;
import idealworld.dew.baas.common.service.ConfigService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * The type Common config.
 *
 * @author gudaoxuri
 */
@Component
@Data
public abstract class CommonConfig {

    private Integer syncIntervalSec = 60;
    private String eventNotifyTopicName = "";

    @Autowired
    private ConfigService configService;

    @PostConstruct
    private void sync() {
        // TODO　test
        $.timer.periodic(syncIntervalSec, true, () -> {
            var config = configService.findAll();
            if (config.containsKey(Constant.CONFIG_EVENT_CONFIG_SYNC_INTERVAL_SEC)) {
                syncIntervalSec = Integer.parseInt(config.get(Constant.CONFIG_EVENT_CONFIG_SYNC_INTERVAL_SEC));
            }
            if (config.containsKey(Constant.CONFIG_EVENT_NOTIFY_TOPIC_NAME)) {
                eventNotifyTopicName = config.get(Constant.CONFIG_EVENT_NOTIFY_TOPIC_NAME);
            }
            doSync(config);
        });
    }

    protected abstract void doSync(Map<String, String> config);

}
