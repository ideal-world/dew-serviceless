package idealworld.dew.serviceless.iam.exchange;

import com.ecfront.dew.common.$;
import group.idealworld.dew.Dew;
import idealworld.dew.serviceless.common.Constant;
import idealworld.dew.serviceless.common.dto.exchange.ExchangeData;
import idealworld.dew.serviceless.common.enumeration.CommonStatus;
import idealworld.dew.serviceless.common.enumeration.OptActionKind;
import idealworld.dew.serviceless.iam.IAMConstant;
import idealworld.dew.serviceless.iam.domain.auth.*;
import idealworld.dew.serviceless.iam.domain.ident.*;
import idealworld.dew.serviceless.iam.scene.common.service.IAMBasicService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * @author gudaoxuri
 */
@Slf4j
@Component
public class ExchangeProcessor extends IAMBasicService {

    private static final String QUICK_CHECK_SPLIT = "#";

    public static void publish(String subjectCategory, OptActionKind actionKind, Object subjectIds, Map<String, Object> detailData) {
        if (subjectIds instanceof Collection<?>) {
            for (var subjectId : (Collection<?>) subjectIds) {
                Dew.cluster.mq.publish(Constant.EVENT_NOTIFY_TOPIC_BY_IAM,
                        subjectCategory + QUICK_CHECK_SPLIT + $.json.toJsonString(ExchangeData.builder()
                                .actionKind(actionKind)
                                .subjectCategory(subjectCategory)
                                .subjectId(subjectId.toString())
                                .fetchUrl(fetchUrl(actionKind, subjectCategory, subjectId.toString()))
                                .detailData(detailData)
                                .build()));
            }
            return;
        }
        Dew.cluster.mq.publish(Constant.EVENT_NOTIFY_TOPIC_BY_IAM,
                subjectCategory + QUICK_CHECK_SPLIT + $.json.toJsonString(ExchangeData.builder()
                        .actionKind(actionKind)
                        .subjectCategory(subjectCategory)
                        .subjectId(subjectIds.toString())
                        .fetchUrl(fetchUrl(actionKind, subjectCategory, subjectIds.toString()))
                        .detailData(detailData)
                        .build()));
    }

    private static String fetchUrl(OptActionKind actionKind, String subjectCategory, Object subjectId) {
        if (actionKind == OptActionKind.DELETE) {
            return "";
        }
        String url = "http://" + Dew.Info.name + "/";
        if (subjectCategory.equals(App.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/app/" + subjectId;
        } else if (subjectCategory.equals(AppIdent.class.getSimpleName().toLowerCase())) {
            return url + "console/app/app/ident/" + subjectId;
        } else if (subjectCategory.equals(Account.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/account/" + subjectId;
        } else if (subjectCategory.equals(AccountIdent.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/account/ident/" + subjectId;
        } else if (subjectCategory.equals(AccountApp.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/account/app/" + subjectId;
        } else if (subjectCategory.equals(AccountGroup.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/account/group/" + subjectId;
        } else if (subjectCategory.equals(AccountRole.class.getSimpleName().toLowerCase())) {
            return url + "console/tenant/account/role/" + subjectId;
        } else if (subjectCategory.equals(Group.class.getSimpleName().toLowerCase())) {
            return url + "console/app/group/" + subjectId;
        } else if (subjectCategory.equals(GroupNode.class.getSimpleName().toLowerCase())) {
            return url + "console/app/group/node/" + subjectId;
        } else if (subjectCategory.equals(RoleDef.class.getSimpleName().toLowerCase())) {
            return url + "console/app/role/def/" + subjectId;
        } else if (subjectCategory.equals(Role.class.getSimpleName().toLowerCase())) {
            return url + "console/app/role/" + subjectId;
        } else if (subjectCategory.equals(ResourceSubject.class.getSimpleName().toLowerCase())) {
            return url + "console/app/resource/subject/" + subjectId;
        } else if (subjectCategory.equals(Resource.class.getSimpleName().toLowerCase())) {
            return url + "console/app/resource/" + subjectId;
        } else if (subjectCategory.equals(AuthPolicy.class.getSimpleName().toLowerCase())) {
            return url + "console/app/authpolicy/" + subjectId;
        }
        log.error("[MQ]URL for resources [{}] not found", subjectCategory);
        return "";
    }


    public void cacheAppIdents() {
        if (!ELECTION.isLeader()) {
            return;
        }
        var qAppIdent = QAppIdent.appIdent;
        var qTenant = QTenant.tenant;
        var qApp = QApp.app;
        sqlBuilder
                .select(qAppIdent.ak, qAppIdent.sk, qAppIdent.relAppId, qAppIdent.validTime, qApp.relTenantId)
                .from(qAppIdent)
                .innerJoin(qApp)
                .on(qApp.id.eq(qAppIdent.relAppId)
                        .and(qApp.status.eq(CommonStatus.ENABLED)))
                .innerJoin(qTenant)
                .on(qTenant.id.eq(qApp.relTenantId)
                        .and(qTenant.status.eq(CommonStatus.ENABLED)))
                .where(qAppIdent.validTime.gt(new Date()))
                .fetch()
                .forEach(info -> {
                    var ak = info.get(0, String.class);
                    var sk = info.get(1, String.class);
                    var appId = info.get(2, Long.class);
                    var validTime = info.get(3, Date.class);
                    var tenantId = info.get(4, Long.class);
                    changeAppIdent(ak, sk, validTime, appId, tenantId);
                });
    }

    public void enableTenant(Long tenantId) {
        var qAppIdent = QAppIdent.appIdent;
        var qApp = QApp.app;
        sqlBuilder
                .select(qAppIdent.ak, qAppIdent.sk, qAppIdent.relAppId, qAppIdent.validTime)
                .from(qAppIdent)
                .innerJoin(qApp)
                .on(qApp.id.eq(qAppIdent.relAppId).and(qApp.status.eq(CommonStatus.ENABLED)))
                .where(qApp.relTenantId.eq(tenantId))
                .where(qAppIdent.validTime.gt(new Date()))
                .fetch()
                .forEach(info -> {
                    var ak = info.get(0, String.class);
                    var sk = info.get(1, String.class);
                    var appId = info.get(2, Long.class);
                    var validTime = info.get(3, Date.class);
                    changeAppIdent(ak, sk, validTime, appId, tenantId);
                });
    }

    public void disableTenant(Long tenantId) {
        var qAppIdent = QAppIdent.appIdent;
        var qApp = QApp.app;
        sqlBuilder
                .select(qAppIdent.ak)
                .from(qAppIdent)
                .innerJoin(qApp)
                .on(qApp.id.eq(qAppIdent.relAppId))
                .where(qApp.relTenantId.eq(tenantId))
                .fetch()
                .forEach(this::deleteAppIdent);
    }

    public void enableApp(Long appId, Long tenantId) {
        var qApp = QApp.app;
        sqlBuilder
                .select(qApp.pubKey, qApp.priKey)
                .from(qApp)
                .where(qApp.id.eq(appId))
                .fetch()
                .forEach(info -> {
                    var publicKey = info.get(0, String.class);
                    var privateKey = info.get(1, String.class);
                    Dew.cluster.cache.set(IAMConstant.CACHE_APP_INFO + appId, tenantId + "\n" + publicKey + "\n" + privateKey);
                });
        var qAppIdent = QAppIdent.appIdent;
        sqlBuilder
                .select(qAppIdent.ak, qAppIdent.sk, qAppIdent.validTime)
                .from(qAppIdent)
                .where(qAppIdent.relAppId.eq(appId))
                .where(qAppIdent.validTime.gt(new Date()))
                .fetch()
                .forEach(info -> {
                    var ak = info.get(0, String.class);
                    var sk = info.get(1, String.class);
                    var validTime = info.get(2, Date.class);
                    changeAppIdent(ak, sk, validTime, appId, tenantId);
                });
    }

    public void disableApp(Long appId, Long tenantId) {
        var qAppIdent = QAppIdent.appIdent;
        sqlBuilder
                .select(qAppIdent.ak)
                .from(qAppIdent)
                .where(qAppIdent.relAppId.eq(appId))
                .fetch()
                .forEach(this::deleteAppIdent);
        Dew.cluster.cache.del(IAMConstant.CACHE_APP_INFO + appId);
    }

    public void changeAppIdent(AppIdent appIdent, Long appId, Long tenantId) {
        changeAppIdent(appIdent.getAk(), appIdent.getSk(), appIdent.getValidTime(), appId, tenantId);
    }

    public void deleteAppIdent(String ak) {
        Dew.cluster.cache.del(IAMConstant.CACHE_APP_AK + ak);
    }

    private void changeAppIdent(String ak, String sk, Date validTime, Long appId, Long tenantId) {
        Dew.cluster.cache.del(IAMConstant.CACHE_APP_AK + ak);
        if (validTime == null) {
            Dew.cluster.cache.set(IAMConstant.CACHE_APP_AK + ak, sk + ":" + tenantId + ":" + appId);
        } else {
            Dew.cluster.cache.setex(IAMConstant.CACHE_APP_AK + ak, sk + ":" + tenantId + ":" + appId,
                    (validTime.getTime() - System.currentTimeMillis()) / 1000);
        }
    }

}
