package idealworld.dew.baas.iam;

import idealworld.dew.baas.common.Constant;

/**
 * 常量池.
 *
 * @author gudaoxuri
 */
public class IAMConstant extends Constant {

    public static final String CACHE_APP_AK = "iam:app:ak:";
    public static final String CACHE_TENANT_STATUS_ENABLE = "iam:tenant:enable:";
    public static final String CACHE_APP_STATUS_ENABLE = "iam:app:enable:";

    public static final String CACHE_ACCESS_TOKEN = "iam:oauth:access-token:";

    public static final String CACHE_ACCOUNT_VCODE_TMP_REL = "iam:account:vocde:tmprel:";
    public static final String CACHE_ACCOUNT_VCODE_ERROR_TIMES = "iam:account:vocde:errortimes:";

    public static final String CONFIG_TENANT_REGISTER_ALLOW = "tenant:register:allow";
    public static final String CONFIG_ACCOUNT_VCODE_EXPIRE_SEC = "account:vcode:expiresec";
    public static final String CONFIG_ACCOUNT_VCODE_ERROR_TIMES = "account:vcode:errortimes";
    public static final String CONFIG_APP_REQUEST_DATE_OFFSET_MS = "app.request.dateoffsetms";
    public static final String CONFIG_SERVICE_URL = "service.url";


}


