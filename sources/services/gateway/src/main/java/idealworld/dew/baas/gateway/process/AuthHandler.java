package idealworld.dew.baas.gateway.process;

import com.ecfront.dew.common.StandardCode;
import idealworld.dew.baas.common.Constant;
import idealworld.dew.baas.common.enumeration.AuthActionKind;
import idealworld.dew.baas.common.enumeration.AuthResultKind;
import idealworld.dew.baas.common.enumeration.AuthSubjectKind;
import idealworld.dew.baas.gateway.GatewayConfig;
import io.vertx.ext.web.RoutingContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 鉴权处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public class AuthHandler extends GatewayHandler {

    private final GatewayConfig.Request request;
    private final ReadonlyAuthPolicy authPolicy;

    public AuthHandler(GatewayConfig.Request request, ReadonlyAuthPolicy authPolicy) {
        this.request = request;
        this.authPolicy = authPolicy;
    }

    @SneakyThrows
    @Override
    public void handle(RoutingContext ctx) {
        var identOptInfo = (IdentOptCacheInfo) ctx.get(CONTEXT_INFO);
        var resourceUri = (URI) ctx.get(request.getResourceUriKey());
        var action = (AuthActionKind) ctx.get(request.getActionKey());

        // 带优先级
        var subjectInfo = new LinkedHashMap<AuthSubjectKind, List<String>>();
        if (identOptInfo != null) {
            if (identOptInfo.getAccountCode() != null) {
                subjectInfo.put(AuthSubjectKind.ACCOUNT, new ArrayList<>() {
                    {
                        add(identOptInfo.getAccountCode().toString());
                    }
                });
            }
            if (identOptInfo.getGroupInfo() != null && !identOptInfo.getGroupInfo().isEmpty()) {
                subjectInfo.put(AuthSubjectKind.GROUP_NODE, identOptInfo.getGroupInfo().stream()
                        .map(group -> group.getGroupCode() + Constant.GROUP_CODE_NODE_CODE_SPLIT + group.getGroupNodeCode())
                        .collect(Collectors.toList()));
            }
            if (identOptInfo.getRoleInfo() != null && !identOptInfo.getRoleInfo().isEmpty()) {
                subjectInfo.put(AuthSubjectKind.ROLE, identOptInfo.getRoleInfo().stream()
                        .map(IdentOptCacheInfo.RoleInfo::getCode)
                        .collect(Collectors.toList()));
            }
            if (identOptInfo.getAppId() != null) {
                subjectInfo.put(AuthSubjectKind.APP, new ArrayList<>() {
                    {
                        add(identOptInfo.getAppId().toString());
                    }
                });
            }
            if (identOptInfo.getTenantId() != null) {
                subjectInfo.put(AuthSubjectKind.TENANT, new ArrayList<>() {
                    {
                        add(identOptInfo.getTenantId().toString());
                    }
                });
            }
        }
        authPolicy.authentication(resourceUri, action.toString(), subjectInfo)
                .onSuccess(authResultKind -> {
                    if (authResultKind == AuthResultKind.REJECT) {
                        error(Integer.parseInt(StandardCode.UNAUTHORIZED.toString()), "鉴权错误，没有权限访问对应的资源[" + action + "|" + resourceUri.toString() + "]", ctx);
                        return;
                    }
                    ctx.next();
                })
                .onFailure(e -> error(Integer.parseInt(StandardCode.UNAUTHORIZED.toString()), "鉴权错误:" + e.getMessage(), ctx, e));
    }


}
