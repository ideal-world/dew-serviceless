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

package idealworld.dew.serviceless.iam.process.tenantconsole;

import com.ecfront.dew.common.Page;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.fun.eventbus.EventBusProcessor;
import idealworld.dew.framework.fun.eventbus.ProcessContext;
import idealworld.dew.serviceless.iam.domain.ident.Tenant;
import idealworld.dew.serviceless.iam.domain.ident.TenantCert;
import idealworld.dew.serviceless.iam.domain.ident.TenantIdent;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant.*;
import io.vertx.core.Future;

import java.util.HashMap;

/**
 * 租户控制台下的租户控制器.
 *
 * @author gudaoxuri
 */
public class TCTenantProcessor extends EventBusProcessor {

    public TCTenantProcessor(String moduleName) {
        super(moduleName);
    }

    {
        // 修改当前租户
        addProcessor(OptActionKind.PATCH, "/console/tenant/tenant", eventBusContext ->
                modifyTenant(eventBusContext.req.body(TenantModifyReq.class), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前租户信息
        addProcessor(OptActionKind.FETCH, "/console/tenant/tenant", eventBusContext ->
                getTenant(eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 添加当前租户的认证
        addProcessor(OptActionKind.CREATE, "/console/tenant/tenant/ident", eventBusContext ->
                addTenantIdent(eventBusContext.req.body(TenantIdentAddReq.class), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前租户的某个认证
        addProcessor(OptActionKind.PATCH, "/console/tenant/tenant/ident/{tenantIdentId}", eventBusContext ->
                modifyTenantIdent(Long.parseLong(eventBusContext.req.params.get("tenantIdentId")), eventBusContext.req.body(TenantIdentModifyReq.class), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前租户的某个认证信息
        addProcessor(OptActionKind.FETCH, "/console/tenant/tenant/ident/{tenantIdentId}", eventBusContext ->
                getTenantIdent(Long.parseLong(eventBusContext.req.params.get("tenantIdentId")), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前租户的认证列表信息
        addProcessor(OptActionKind.FETCH, "/console/tenant/tenant/ident", eventBusContext ->
                pageTenantIdents(eventBusContext.req.pageNumber(), eventBusContext.req.pageSize(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 删除当前租户的某个认证
        addProcessor(OptActionKind.DELETE, "/console/tenant/tenant/ident/{tenantIdentId}", eventBusContext ->
                deleteTenantIdent(Long.parseLong(eventBusContext.req.params.get("tenantIdentId")), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 添加当前租户的凭证
        addProcessor(OptActionKind.CREATE, "/console/tenant/tenant/cert", eventBusContext ->
                addTenantCert(eventBusContext.req.body(TenantCertAddReq.class), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 修改当前租户的某个凭证
        addProcessor(OptActionKind.PATCH, "/console/tenant/tenant/cert/{tenantCertId}", eventBusContext ->
                modifyTenantCert(Long.parseLong(eventBusContext.req.params.get("tenantCertId")), eventBusContext.req.body(TenantCertModifyReq.class), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前租户的某个凭证信息
        addProcessor(OptActionKind.FETCH, "/console/tenant/tenant/cert/{tenantCertId}", eventBusContext ->
                getTenantCert(Long.parseLong(eventBusContext.req.params.get("tenantCertId")), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 获取当前租户的某个凭证信息
        addProcessor(OptActionKind.FETCH, "/console/tenant/tenant/cert", eventBusContext ->
                pageTenantCerts(eventBusContext.req.pageNumber(), eventBusContext.req.pageSize(), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
        // 删除当前租户的某个认证
        addProcessor(OptActionKind.DELETE, "/console/tenant/tenant/cert/{tenantCertId}", eventBusContext ->
                deleteTenantCert(Long.parseLong(eventBusContext.req.params.get("tenantCertId")), eventBusContext.req.identOptInfo.getTenantId(), eventBusContext.context));
    }

    public static Future<Void> modifyTenant(TenantModifyReq tenantModifyReq, Long relTenantId, ProcessContext context) {
        var tenant = context.helper.convert(tenantModifyReq, Tenant.class);
        return context.sql.update(relTenantId, tenant)
                .compose(resp -> {
                    if (tenantModifyReq.getStatus() != null) {
                        if (tenantModifyReq.getStatus() == CommonStatus.ENABLED) {
                            return ExchangeProcessor.enableTenant(relTenantId, context);
                        } else {
                            return ExchangeProcessor.disableTenant(relTenantId, context);
                        }
                    }
                    return context.helper.success(resp);
                });
    }

    public static Future<TenantResp> getTenant(Long relTenantId, ProcessContext context) {
        return context.sql.getOne(relTenantId, Tenant.class)
                .compose(tenant -> context.helper.success(tenant, TenantResp.class));
    }

    // --------------------------------------------------------------------

    public static Future<Long> addTenantIdent(TenantIdentAddReq tenantIdentAddReq, Long relTenantId, ProcessContext context) {
        return context.helper.existToError(
                context.sql.exists(
                        new HashMap<>() {
                            {
                                put("kind", tenantIdentAddReq.getKind());
                                put("rel_tenant_id", relTenantId);
                            }
                        },
                        TenantIdent.class), () -> new ConflictException("租户认证类型已存在"))
                .compose(resp -> {
                    var tenantIdent = context.helper.convert(tenantIdentAddReq, TenantIdent.class);
                    tenantIdent.setRelTenantId(relTenantId);
                    return context.sql.save(tenantIdent);
                });
    }

    public static Future<Void> modifyTenantIdent(Long tenantIdentId, TenantIdentModifyReq tenantIdentModifyReq, Long relTenantId, ProcessContext context) {
        var tenantIdent = context.helper.convert(tenantIdentModifyReq, TenantIdent.class);
        return context.sql.update(new HashMap<>() {
            {
                put("id", tenantIdentId);
                put("rel_tenant_id", relTenantId);
            }
        }, tenantIdent);
    }

    public static Future<TenantIdentResp> getTenantIdent(Long tenantIdentId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", tenantIdentId);
                        put("rel_tenant_id", relTenantId);
                    }
                },
                TenantIdent.class)
                .compose(tenantIdent -> context.helper.success(tenantIdent, TenantIdentResp.class));
    }

    public static Future<Page<TenantIdentResp>> pageTenantIdents(Long pageNumber, Long pageSize, Long relTenantId, ProcessContext context) {
        return context.sql.page(
                new HashMap<>() {
                    {
                        put("rel_tenant_id", relTenantId);
                    }
                },
                pageNumber,
                pageSize,
                TenantIdent.class)
                .compose(tenantIdents -> context.helper.success(tenantIdents, TenantIdentResp.class));
    }

    public static Future<Void> deleteTenantIdent(Long tenantIdentId, Long relTenantId, ProcessContext context) {
        return context.sql.softDelete(
                new HashMap<>() {
                    {
                        put("id", tenantIdentId);
                        put("rel_tenant_id", relTenantId);
                    }
                },
                TenantIdent.class);
    }

    // --------------------------------------------------------------------

    public static Future<Long> addTenantCert(TenantCertAddReq tenantCertAddReq, Long relTenantId, ProcessContext context) {
        return context.helper.existToError(
                context.sql.exists(
                        new HashMap<>() {
                            {
                                put("category", tenantCertAddReq.getCategory());
                                put("rel_tenant_id", relTenantId);
                            }
                        },
                        TenantCert.class), () -> new ConflictException("租户凭证类型已存在"))
                .compose(resp -> {
                    var tenantCert = context.helper.convert(tenantCertAddReq, TenantCert.class);
                    tenantCert.setRelTenantId(relTenantId);
                    return context.sql.save(tenantCert);
                });
    }

    public static Future<Void> modifyTenantCert(Long tenantCertId, TenantCertModifyReq tenantCertModifyReq, Long relTenantId, ProcessContext context) {
        if (tenantCertModifyReq.getCategory() != null) {
            return context.helper.existToError(
                    context.sql.exists(
                            new HashMap<>() {
                                {
                                    put("category", tenantCertModifyReq.getCategory());
                                    put("rel_tenant_id", relTenantId);
                                    put("!id", tenantCertId);
                                }
                            },
                            TenantCert.class), () -> new ConflictException("租户凭证类型已存在"))
                    .compose(resp -> {
                        var tenantCert = context.helper.convert(tenantCertModifyReq, TenantCert.class);
                        return context.sql.update(new HashMap<>() {
                            {
                                put("id", tenantCertId);
                                put("rel_tenant_id", relTenantId);
                            }
                        }, tenantCert);
                    });
        }
        var tenantCert = context.helper.convert(tenantCertModifyReq, TenantCert.class);
        return context.sql.update(new HashMap<>() {
            {
                put("id", tenantCertId);
                put("rel_tenant_id", relTenantId);
            }
        }, tenantCert);
    }

    public static Future<TenantCertResp> getTenantCert(Long tenantCertId, Long relTenantId, ProcessContext context) {
        return context.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", tenantCertId);
                        put("rel_tenant_id", relTenantId);
                    }
                },
                TenantCert.class)
                .compose(tenantCert -> context.helper.success(tenantCert, TenantCertResp.class));
    }

    public static Future<Page<TenantCertResp>> pageTenantCerts(Long pageNumber, Long pageSize, Long relTenantId, ProcessContext context) {
        return context.sql.page(
                new HashMap<>() {
                    {
                        put("rel_tenant_id", relTenantId);
                    }
                },
                pageNumber,
                pageSize,
                TenantCert.class)
                .compose(tenantCerts -> context.helper.success(tenantCerts, TenantCertResp.class));
    }

    public static Future<Void> deleteTenantCert(Long tenantCertId, Long relTenantId, ProcessContext context) {
        return context.sql.softDelete(
                new HashMap<>() {
                    {
                        put("id", tenantCertId);
                        put("rel_tenant_id", relTenantId);
                    }
                },
                TenantCert.class);
    }

}
