package idealworld.dew.serviceless.common.funs.httpserver;

import com.ecfront.dew.common.StandardCode;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.IdentOptCacheInfo;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTTP 处理器
 *
 * @author gudaoxuri
 */
@Slf4j
public abstract class CommonHttpHandler implements Handler<RoutingContext> {

    protected static final String CONTEXT_INFO = "CONTEXT";

    protected void error(StandardCode statusCode, String msg, RoutingContext ctx) {
        log.warn("[Process]Request error [{}]: {}", statusCode.toString(), msg);
        ctx.response().setStatusCode(Integer.parseInt(statusCode.toString())).end(msg);
    }

    protected void error(StandardCode statusCode, String msg, RoutingContext ctx, Throwable e) {
        log.warn("[Process]Request error [{}]{}", statusCode.toString(), e.getMessage(), e);
        ctx.response().setStatusCode(Integer.parseInt(statusCode.toString())).end(msg);
    }

    protected Map<AuthSubjectKind, List<String>> packageSubjectInfo(IdentOptCacheInfo identOptInfo) {
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
        return subjectInfo;
    }

}
