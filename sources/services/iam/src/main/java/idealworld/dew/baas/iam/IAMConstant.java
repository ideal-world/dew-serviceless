package idealworld.dew.baas.iam;

import idealworld.dew.baas.common.Constant;

/**
 * 常量池.
 *
 * @author gudaoxuri
 */
public class IAMConstant extends Constant {

    public static final String CACHE_APP_AK = "dew:auth:app:ak:";
    public static final String CACHE_TENANT_STATUS_ENABLE = "dew:auth:tenant:enable:";
    public static final String CACHE_APP_STATUS_ENABLE = "dew:auth:app:enable:";
    public static final String CACHE_ACCESS_TOKEN = "dew:auth:oauth:access-token:";
    public static final String CACHE_ACCOUNT_VCODE_TMP_REL = "dew:auth:account:vocde:tmprel:";
    public static final String CACHE_ACCOUNT_VCODE_ERROR_TIMES = "dew:auth:account:vocde:errortimes:";

    public static final String CONFIG_TENANT_REGISTER_ALLOW = "tenant:register:allow";
    public static final String CONFIG_ACCOUNT_VCODE_EXPIRE_SEC = "account:vcode:expiresec";
    public static final String CONFIG_ACCOUNT_VCODE_ERROR_TIMES = "account:vcode:errortimes";
    // TODO
    public static final String CONFIG_APP_REQUEST_DATE_OFFSET_MS = "app.request.dateoffsetms";
    public static final String CONFIG_SERVICE_URL = "service.url";

    public static final String CONFIG_AUTH_POLICY_MAX_FETCH_COUNT = "iam:auth:policy:fetchcount:max";
    public static final String CONFIG_AUTH_POLICY_EXPIRE_CLEAN_INTERVAL_SEC = "iam:auth:policy:expire:clean:intervalsec";


}


