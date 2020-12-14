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

import com.ecfront.dew.common.$;
import com.ecfront.dew.common.Page;
import idealworld.dew.framework.dto.CommonStatus;
import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.exception.ConflictException;
import idealworld.dew.framework.fun.eventbus.ProcessFun;
import idealworld.dew.framework.fun.eventbus.ReceiveProcessor;
import idealworld.dew.serviceless.iam.domain.ident.Tenant;
import idealworld.dew.serviceless.iam.domain.ident.TenantCert;
import idealworld.dew.serviceless.iam.domain.ident.TenantIdent;
import idealworld.dew.serviceless.iam.process.tenantconsole.dto.tenant.*;

import java.util.HashMap;

/**
 * 租户控制台下的租户控制器.
 *
 * @author gudaoxuri
 */
public class TCTenantProcessor {

    {
        // 修改当前租户
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/tenant", modifyTenant());
        // 获取当前租户信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/tenant", getTenant());
        // 添加当前租户的认证
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/ident", addTenantIdent());
        // 修改当前租户的某个认证
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/ident/{tenantIdentId}", modifyTenantIdent());
        // 获取当前租户的某个认证信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/ident/{tenantIdentId}", getTenantIdent());
        // 获取当前租户的认证列表信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/ident", pageTenantIdents());
        // 删除当前租户的某个认证
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/tenant/ident/{tenantIdentId}", deleteTenantIdent());
        // 添加当前租户的凭证
        ReceiveProcessor.addProcessor(OptActionKind.CREATE, "/console/tenant/cert", addTenantCert());
        // 修改当前租户的某个凭证
        ReceiveProcessor.addProcessor(OptActionKind.PATCH, "/console/tenant/cert/{tenantCertId}", modifyTenantCert());
        // 获取当前租户的某个凭证信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/cert/{tenantCertId}", getTenantCert());
        // 获取当前租户的某个凭证信息
        ReceiveProcessor.addProcessor(OptActionKind.FETCH, "/console/tenant/cert/", pageTenantCerts());
        // 删除当前租户的某个认证
        ReceiveProcessor.addProcessor(OptActionKind.DELETE, "/console/tenant/cert/{tenantCertId}", deleteTenantCert());
    }

    public ProcessFun<Long> modifyTenant() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var tenantModifyReq = context.helper.parseBody(context.req.body, TenantModifyReq.class);
            var tenant = context.helper.convert(tenantModifyReq, Tenant.class);
            return context.fun.sql.update(relTenantId, tenant, context)
                    .compose(resp -> {
                        if (tenantModifyReq.getStatus() != null) {
                            if (tenantModifyReq.getStatus() == CommonStatus.ENABLED) {
                                exchangeProcessor.enableTenant(relTenantId);
                            } else {
                                exchangeProcessor.disableTenant(relTenantId);
                            }
                        }
                        return context.helper.success(resp);
                    });
        };
    }

    public ProcessFun<TenantResp> getTenant() {
        return context -> {
            return context.fun.sql.getOne(context.req.identOptInfo.getTenantId(), Tenant.class, context)
                    .compose(tenant -> context.helper.success(tenant, TenantResp.class));
        };
    }

    // --------------------------------------------------------------------

    public ProcessFun<Long> addTenantIdent() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var tenantIdentAddReq = context.helper.parseBody(context.req.body, TenantIdentAddReq.class);
            return context.fun.sql.exists(
                    new HashMap<>() {
                        {
                            put("kind", tenantIdentAddReq.getKind());
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    TenantIdent.class,
                    context)
                    .compose(existsResult -> {
                        if (existsResult) {
                            return context.helper.error(new ConflictException("租户认证类型已存在"));
                        }
                        return context.helper.success(null);
                    })
                    .compose(resp -> {
                        var tenantIdent = $.bean.copyProperties(tenantIdentAddReq, TenantIdent.class);
                        tenantIdent.setRelTenantId(relTenantId);
                        return context.fun.sql.save(tenantIdent, context);
                    });
        };
    }

    public ProcessFun<Long> modifyTenantIdent() {
        return context -> {
            var tenantIdentModifyReq = context.helper.parseBody(context.req.body, TenantIdentModifyReq.class);
            var tenantIdent = context.helper.convert(tenantIdentModifyReq, TenantIdent.class);
            return context.fun.sql.update(new HashMap<>() {
                {
                    put("id", context.req.params.get("tenantIdentId"));
                    put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                }
            }, tenantIdent, context);
        };
    }

    public ProcessFun<TenantIdentResp> getTenantIdent() {
        return context -> context.fun.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", context.req.params.get("tenantIdentId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                TenantIdent.class, context)
                .compose(tenantIdent -> context.helper.success(tenantIdent, TenantIdentResp.class));
    }

    public ProcessFun<Page<TenantIdentResp>> pageTenantIdents() {
        return context -> context.fun.sql.page(
                new HashMap<>() {
                    {
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                TenantIdent.class, context)
                .compose(tenantIdents -> context.helper.success(tenantIdents, TenantIdentResp.class));
    }

    public ProcessFun<Long> deleteTenantIdent() {
        return context -> context.fun.sql.softDelete(
                new HashMap<>() {
                    {
                        put("id", context.req.params.get("tenantIdentId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                TenantIdent.class, context);
    }

    // --------------------------------------------------------------------

    public ProcessFun<Long> addTenantCert() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var tenantCertAddReq = context.helper.parseBody(context.req.body, TenantCertAddReq.class);
            return context.fun.sql.exists(
                    new HashMap<>() {
                        {
                            put("category", tenantCertAddReq.getCategory());
                            put("rel_tenant_id", relTenantId);
                        }
                    },
                    TenantCert.class,
                    context)
                    .compose(existsResult -> {
                        if (existsResult) {
                            return context.helper.error(new ConflictException("租户凭证类型已存在"));
                        }
                        return context.helper.success(null);
                    })
                    .compose(resp -> {
                        var tenantCert = $.bean.copyProperties(tenantCertAddReq, TenantCert.class);
                        tenantCert.setRelTenantId(relTenantId);
                        return context.fun.sql.save(tenantCert, context);
                    });
        };
    }

    public ProcessFun<Long> modifyTenantCert() {
        return context -> {
            var relTenantId = context.req.identOptInfo.getTenantId();
            var tenantCertModifyReq = context.helper.parseBody(context.req.body, TenantCertModifyReq.class);
            if (tenantCertModifyReq.getCategory() != null) {
                return context.fun.sql.exists(
                        new HashMap<>() {
                            {
                                put("category", tenantCertModifyReq.getCategory());
                                put("rel_tenant_id", relTenantId);
                                put("-id", context.req.params.get("tenantCertId"));
                            }
                        },
                        TenantCert.class,
                        context)
                        .compose(existsResult -> {
                            if (existsResult) {
                                return context.helper.error(new ConflictException("租户凭证类型已存在"));
                            }
                            return context.helper.success(null);
                        })
                        .compose(resp -> {
                            var tenantCert = context.helper.convert(tenantCertModifyReq, TenantCert.class);
                            return context.fun.sql.update(new HashMap<>() {
                                {
                                    put("id", context.req.params.get("tenantCertId"));
                                    put("rel_tenant_id", relTenantId);
                                }
                            }, tenantCert, context);
                        });
            }
            var tenantCert = context.helper.convert(tenantCertModifyReq, TenantCert.class);
            return context.fun.sql.update(new HashMap<>() {
                {
                    put("id", context.req.params.get("tenantCertId"));
                    put("rel_tenant_id", relTenantId);
                }
            }, tenantCert, context);
        };
    }

    public ProcessFun<TenantCertResp> getTenantCert() {
        return context -> context.fun.sql.getOne(
                new HashMap<>() {
                    {
                        put("id", context.req.params.get("tenantCertId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                TenantCert.class, context)
                .compose(tenantCert -> context.helper.success(tenantCert, TenantCertResp.class));
    }

    public ProcessFun<Page<TenantCertResp>> pageTenantCerts() {
        return context -> context.fun.sql.page(
                new HashMap<>() {
                    {
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                TenantCert.class, context)
                .compose(tenantCerts -> context.helper.success(tenantCerts, TenantCertResp.class));
    }

    public ProcessFun<Long> deleteTenantCert() {
        return context -> context.fun.sql.softDelete(
                new HashMap<>() {
                    {
                        put("id", context.req.params.get("tenantIdentId"));
                        put("rel_tenant_id", context.req.identOptInfo.getTenantId());
                    }
                },
                TenantCert.class, context);
    }

}
