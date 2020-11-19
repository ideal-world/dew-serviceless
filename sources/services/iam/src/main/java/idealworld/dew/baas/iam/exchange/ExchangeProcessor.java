package idealworld.dew.baas.iam.exchange;

import com.ecfront.dew.common.$;
import group.idealworld.dew.Dew;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.dto.exchange.ExchangeData;
import org.springframework.stereotype.Component;

/**
 * @author gudaoxuri
 */
@Component
public class ExchangeProcessor {

    private static final String QUICK_CHECK_SPLIT = "#";

    public void publish(ExchangeData exchangeData) {
        Dew.cluster.mq.publish(Constant.EVENT_NOTIFY_TOPIC_BY_IAM,
                exchangeData.getSubjectCategory() + QUICK_CHECK_SPLIT + $.json.toJsonString(exchangeData));
    }

}
