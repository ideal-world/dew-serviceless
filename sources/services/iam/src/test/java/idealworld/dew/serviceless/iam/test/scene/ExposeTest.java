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

package idealworld.dew.serviceless.iam.test.scene;

import idealworld.dew.serviceless.common.dto.IdentOptInfo;
import idealworld.dew.serviceless.common.enumeration.AuthResultKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectKind;
import idealworld.dew.serviceless.common.enumeration.AuthSubjectOperatorKind;
import idealworld.dew.serviceless.common.enumeration.ResourceKind;
import idealworld.dew.serviceless.iam.enumeration.ExposeKind;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.authpolicy.AuthPolicyResp;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.ResourceAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.ResourceResp;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.ResourceSubjectAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.role.RoleAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.role.RoleDefAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.role.RoleResp;
import idealworld.dew.serviceless.iam.scene.common.dto.tenant.TenantRegisterReq;
import idealworld.dew.serviceless.iam.test.BasicTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExposeTest extends BasicTest {

    @Test
    public void testExpose() {
        loginBySystemAdmin();
        // 租户注册
        var identOptInfo = postToEntity("/common/tenant", TenantRegisterReq.builder()
                .tenantName("测试租户")
                .appName("默认应用")
                .accountUserName("jzy1")
                .accountPassword("si2nc$@2")
                .build(), IdentOptInfo.class).getBody();
        setToken(identOptInfo.getToken());
        // 添加当前应用的角色定义
        var adminRoleDefId = postToEntity("/console/app/role/def", RoleDefAddReq.builder()
                .code("root")
                .name("管理员")
                .build(), Long.class).getBody();
        var tenantRoleDefId = postToEntity("/console/app/role/def", RoleDefAddReq.builder()
                .code("tenantRoleDef")
                .name("开放用户")
                .build(), Long.class).getBody();
        var globalRoleDefId = postToEntity("/console/app/role/def", RoleDefAddReq.builder()
                .code("publicRoleDef")
                .name("开放用户")
                .build(), Long.class).getBody();
        // 添加当前应用的角色
        var adminRoleId = postToEntity("/console/app/role", RoleAddReq.builder()
                .relRoleDefId(adminRoleDefId)
                .name("内部角色")
                .build(), Long.class).getBody();
        var tenantRoleId = postToEntity("/console/app/role", RoleAddReq.builder()
                .relRoleDefId(tenantRoleDefId)
                .name("租户级开放角色")
                .exposeKind(ExposeKind.TENANT)
                .build(), Long.class).getBody();
        var globalRoleId = postToEntity("/console/app/role", RoleAddReq.builder()
                .relRoleDefId(globalRoleDefId)
                .name("全局开放角色")
                .exposeKind(ExposeKind.GLOBAL)
                .build(), Long.class).getBody();
        // 添加当前应用的资源主体
        var resourceSubjectId = postToEntity("/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix("testApp")
                .kind(ResourceKind.HTTP)
                .name("测试应用资源主题")
                .uri("http://test-app")
                .build(), Long.class).getBody();
        // 添加当前应用的资源
        var adminResourceId = postToEntity("/console/app/resource", ResourceAddReq.builder()
                .name("后台管理")
                .pathAndQuery("/system/**")
                .relResourceSubjectId(resourceSubjectId)
                .build(), Long.class).getBody();
        var tenantResourceId = postToEntity("/console/app/resource", ResourceAddReq.builder()
                .name("同租户可见资源")
                .pathAndQuery("/limit/**")
                .relResourceSubjectId(resourceSubjectId)
                .build(), Long.class).getBody();
        var globalResourceId = postToEntity("/console/app/resource", ResourceAddReq.builder()
                .name("公开资源")
                .pathAndQuery("/public/**")
                .relResourceSubjectId(resourceSubjectId)
                .exposeKind(ExposeKind.GLOBAL)
                .build(), Long.class).getBody();
        // 添加当前应用的权限策略
        Assertions.assertTrue(postToList("/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(adminRoleId + "")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(adminResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Long.class).ok());
        Assertions.assertTrue(postToList("/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(tenantRoleId + "")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(tenantResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Long.class).ok());
        Assertions.assertTrue(postToList("/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(globalRoleId + "")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(globalResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Long.class).ok());

        // --------------

        // 租户注册
        identOptInfo = postToEntity("/common/tenant", TenantRegisterReq.builder()
                .tenantName("测试租户2")
                .appName("默认应用")
                .accountUserName("jzy1")
                .accountPassword("si2nc$@2")
                .build(), IdentOptInfo.class).getBody();
        setToken(identOptInfo.getToken());
        // 添加当前应用的角色定义
        var test2RoleDefId = postToEntity("/console/app/role/def", RoleDefAddReq.builder()
                .code("busRole")
                .name("业务员")
                .build(), Long.class).getBody();
        // 添加当前应用的角色
        var test2RoleId = postToEntity("/console/app/role", RoleAddReq.builder()
                .relRoleDefId(test2RoleDefId)
                .name("业务员")
                .build(), Long.class).getBody();
        // 添加当前应用的资源主体
        var test2ResourceSubjectId = postToEntity("/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix("test2App")
                .kind(ResourceKind.HTTP)
                .name("测试2应用资源主题")
                .uri("http://test2-app")
                .build(), Long.class).getBody();
        // 添加当前应用的资源
        var test2ResourceId = postToEntity("/console/app/resource", ResourceAddReq.builder()
                .name("业务操作")
                .pathAndQuery("/bus/**")
                .relResourceSubjectId(test2ResourceSubjectId)
                .build(), Long.class).getBody();

        // 获取角色
        var roleResps = getToList("/console/app/role", RoleResp.class).getBody();
        Assertions.assertEquals(1, roleResps.size());
        Assertions.assertEquals("业务员", roleResps.get(0).getName());
        roleResps = getToList("/console/app/role?expose=true", RoleResp.class).getBody();
        Assertions.assertEquals(3, roleResps.size());
        Assertions.assertEquals("全局开放角色", roleResps.get(2).getName());
        // 获取开放的资源
        var resourceResps = getToList("/console/app/resource", ResourceResp.class).getBody();
        Assertions.assertEquals(1, resourceResps.size());
        Assertions.assertEquals("业务操作", resourceResps.get(0).getName());
        resourceResps = getToList("/console/app/resource?expose=true", ResourceResp.class).getBody();
        Assertions.assertEquals(3, resourceResps.size());
        Assertions.assertEquals("公开资源", resourceResps.get(2).getName());

        // 交叉授权
        // 自己应用的资源 - 自己应用的角色
        Assertions.assertTrue(postToList("/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(test2RoleId + "")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(test2ResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Long.class).ok());
        // 其它应用的资源 - 自己应用的角色
        Assertions.assertTrue(postToList("/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(test2RoleId + "")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(resourceResps.get(0).getId())
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Long.class).ok());
        // 自己应用的资源 - 其它应用的角色
        Assertions.assertTrue(postToList("/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ROLE)
                .relSubjectIds(roleResps.get(0).getId() + "")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(test2ResourceId)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Long.class).ok());

        // 获取当前应用的权限策略列表信息
        var authPolicyResps = getToPage("/console/app/authpolicy", 1L, 10, AuthPolicyResp.class).getBody();
        Assertions.assertEquals(18, authPolicyResps.getRecordTotal());

    }

}
