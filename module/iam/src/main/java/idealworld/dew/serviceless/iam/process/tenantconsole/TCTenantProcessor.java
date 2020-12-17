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
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.serviceless.iam.domain.ident.Tenant;
import idealworld.dew.serviceless.iam.domain.ident.TenantCert;
import idealworld.dew.serviceless.iam.domain.ident.TenantIdent;
import idealworld.dew.serviceless.iam.exchange.ExchangeProcessor;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant.*;

import java.util.HashMap;

/**
 * 租户控制台下的租户控制器.
 *
 * @author gudaoxuri
 */
public class TCTenantProcessor {

    static {
        // 修改当前租户
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/tenant", modifyTenant());
        // 获取当前租户信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/tenant", getTenant());
        // 添加当前租户的认证
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/tenant/ident", addTenantIdent());
        // 修改当前租户的某个认证
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/tenant/ident/{tenantIdentId}", modifyTenantIdent());
        // 获取当前租户的某个认证信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/tenant/ident/{tenantIdentId}", getTenantIdent());
        // 获取当前租户的认证列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/tenant/ident", pageTenantIdents());
        // 删除当前租户的某个认证
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/tenant/tenant/ident/{tenantIdentId}", deleteTenantIdent());
        // 添加当前租户的凭证
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/tenant/cert", addTenantCert());
        // 修改当前租户的某个凭证
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/tenant/cert/{tenantCertId}", modifyTenantCert());
        // 获取当前租户的某个凭证信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/tenant/cert/{tenantCertId}", getTenantCert());
        // 获取当前租户的某个凭证信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/tenant/cert/", pageTenantCerts());
        // 删除当前租户的某个认证
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/tenant/tenant/cert/{tenantCertId}", deleteTenantCert());
    }

    public static ProcessFun<Void> modifyTenant() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var tenantModifyReq = context.req.body(TenantModifyReq.class);
            var tenant = context.helper.convert(tenantModifyReq, Tenant.class);
            return context.fun.sql.update(relTenantId, tenant)
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
        };
    }

    public static ProcessFun<TenantResp> getTenant() {
        return context ->
                context.fun.sql.getOne(context.req.identOptInfo.getTenantId(), Tenant.class)
                        .compose(tenant -> context.helper.success(tenant, TenantResp.class));
    }

    // --------------------------------------------------------------------

    public static ProcessFun<Long> addTenantIdent() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var tenantIdentAddReq = context.req.body(TenantIdentAddReq.class);
            return context.helper.existToError(
                    context.fun.sql.exists(
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
                        return context.fun.sql.save(tenantIdent);
                    });
        };
    }

    public static ProcessFun<Void> modifyTenantIdent() {
        return context -> {
            var tenantIdentModifyReq = context.req.body(TenantIdentModifyReq.class);
            var tenantIdent = context.helper.convert(tenantIdentModifyReq, TenantIdent.class);
            return context.fun.sql.update(new HashMap<>() {
                {
                    put("id", context.req.params.get("tenantIdentId"));
                    put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                }
            }, tenantIdent);
        };
    }

    public static ProcessFun<TenantIdentResp> getTenantIdent() {
        return context -> context.fun.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", context.req.params.get("tenantIdentId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                TenantIdent.class)
                .compose(tenantIdent -> context.helper.success(tenantIdent, TenantIdentResp.class));
    }

    public static ProcessFun<Page<TenantIdentResp>> pageTenantIdents() {
        return context -> context.fun.sql.page(
                new HashMap<>() {
                    {
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                context.req.pageNumber(),
                context.req.pageSize(),
                TenantIdent.class)
                .compose(tenantIdents -> context.helper.success(tenantIdents, TenantIdentResp.class));
    }

    public static ProcessFun<Void> deleteTenantIdent() {
        return context -> context.fun.sql.softDelete(
                new HashMap<>() {
                    {
                        put("id", context.req.params.get("tenantIdentId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                TenantIdent.class);
    }

    // --------------------------------------------------------------------

    public static ProcessFun<Long> addTenantCert() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var tenantCertAddReq = context.req.body(TenantCertAddReq.class);
            return context.helper.existToError(
                    context.fun.sql.exists(
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
                        return context.fun.sql.save(tenantCert);
                    });
        };
    }

    public static ProcessFun<Void> modifyTenantCert() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var tenantCertModifyReq = context.req.body(TenantCertModifyReq.class);
            if (tenantCertModifyReq.getCategory() != null) {
                return context.helper.existToError(
                        context.fun.sql.exists(
                                new HashMap<>() {
                                    {
                                        put("category", tenantCertModifyReq.getCategory());
                                        put("rel_tenant_id", relTenantId);
                                        put("!id", context.req.params.get("tenantCertId"));
                                    }
                                },
                                TenantCert.class), () -> new ConflictException("租户凭证类型已存在"))
                        .compose(resp -> {
                            var tenantCert = context.helper.convert(tenantCertModifyReq, TenantCert.class);
                            return context.fun.sql.update(new HashMap<>() {
                                {
                                    put("id", context.req.params.get("tenantCertId"));
                                    put("rel_tenant_id", relTenantId);
                                }
                            }, tenantCert);
                        });
            }
            var tenantCert = context.helper.convert(tenantCertModifyReq, TenantCert.class);
            return context.fun.sql.update(new HashMap<>() {
                {
                    put("id", context.req.params.get("tenantCertId"));
                    put("rel_tenant_id", relTenantId);
                }
            }, tenantCert);
        };
    }

    public static ProcessFun<TenantCertResp> getTenantCert() {
        return context -> context.fun.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", context.req.params.get("tenantCertId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                TenantCert.class)
                .compose(tenantCert -> context.helper.success(tenantCert, TenantCertResp.class));
    }

    public static ProcessFun<Page<TenantCertResp>> pageTenantCerts() {
        return context -> context.fun.sql.page(
                new HashMap<>() {
                    {
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                context.req.pageNumber(),
                context.req.pageSize(),
                TenantCert.class)
                .compose(tenantCerts -> context.helper.success(tenantCerts, TenantCertResp.class));
    }

    public static ProcessFun<Void> deleteTenantCert() {
        return context -> context.fun.sql.softDelete(
                new HashMap<>() {
                    {
                        put("id", context.req.params.get("tenantIdentId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                TenantCert.class);
    }

}
