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

package idealworld.dew.serviceless.iam.test;

import idealworld.dew.framework.dto.OptActionKind;
import idealworld.dew.framework.fun.auth.dto.AuthResultKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectKind;
import idealworld.dew.framework.fun.auth.dto.AuthSubjectOperatorKind;
import idealworld.dew.framework.fun.auth.dto.ResourceKind;
import idealworld.dew.serviceless.iam.dto.GroupKind;
import idealworld.dew.serviceless.iam.process.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.app.AppIdentModifyReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.app.AppIdentResp;
import idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy.AuthPolicyModifyReq;
import idealworld.dew.serviceless.iam.process.appconsole.dto.authpolicy.AuthPolicyResp;
import idealworld.dew.serviceless.iam.process.appconsole.dto.group.*;
import idealworld.dew.serviceless.iam.process.appconsole.dto.resource.*;
import idealworld.dew.serviceless.iam.process.appconsole.dto.role.*;
import idealworld.dew.serviceless.iam.process.systemconsole.dto.TenantAddReq;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

public class AppConsoleTest extends IAMBasicTest {

    @BeforeEach
    public void before(Vertx vertx, VertxTestContext testContext) {
        loginBySystemAdmin();
        // 租户注册
        req(OptActionKind.CREATE, "/console/system/tenant", TenantAddReq.builder()
                .name("xyy")
                .build(), Long.class);
        testContext.completeNow();
    }

    @Test
    public void testApp(Vertx vertx, VertxTestContext testContext) {
        // 添加当前应用的认证
        var appIdentId = req(OptActionKind.CREATE, "/console/app/app/ident", AppIdentAddReq.builder()
                .note("测试用")
                .build(), Long.class)._0;

        // 修改当前应用的某个认证
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/app/app/ident/" + appIdentId, AppIdentModifyReq.builder()
                .note("use test")
                .build(), Void.class)._1);

        // 获取当前应用的认证列表信息
        var appIdentResps = reqPage("/console/app/app/ident", 1L, 10L, AppIdentResp.class)._0;
        Assertions.assertEquals(2, appIdentResps.getRecordTotal());
        Assertions.assertEquals(1, appIdentResps.getPageTotal());
        Assertions.assertEquals("use test", appIdentResps.getObjects().get(1).getNote());

        // 获取当前应用的某个认证SK
        var sk = req(OptActionKind.FETCH, "/console/app/app/ident/" + appIdentId + "/sk", null, String.class)._0;
        Assertions.assertNotNull(sk);

        // 删除当前应用的某个认证
        Assertions.assertNull(req(OptActionKind.DELETE, "/console/app/app/ident/" + appIdentId, null, Void.class)._1);
        Assertions.assertNull(req(OptActionKind.FETCH, "/console/app/app/ident/" + appIdentId + "/sk", null, String.class)._0);

        testContext.completeNow();
    }

    @Test
    public void testResource(Vertx vertx, VertxTestContext testContext) {
        // 添加当前应用的资源主体
        var resourceSubjectId = req(OptActionKind.CREATE, "/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix("defaultmysql")
                .kind(ResourceKind.RELDB)
                .name("MYSQL")
                .uri("mysql://xxxxx")
                .build(), Long.class)._0;
        Assertions.assertEquals("资源主体编码已存在", req(OptActionKind.CREATE, "/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix("defaultmysql")
                .kind(ResourceKind.RELDB)
                .name("mysql")
                .uri("mysql://xxxxx")
                .build(), Long.class)._1.getMessage());
        Assertions.assertNull(req(OptActionKind.CREATE, "/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .codePostfix("defaultmysql2")
                .kind(ResourceKind.RELDB)
                .name("mysql")
                .uri("mysql://xxxxx")
                .build(), Long.class)._1);

        // 修改当前应用的某个资源主体
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/app/resource/subject/" + resourceSubjectId, ResourceSubjectModifyReq.builder()
                .codePostfix("mysql")
                .kind(ResourceKind.RELDB)
                .sort(100)
                .build(), Void.class)._1);

        // 获取当前应用的某个资源主体信息
        var resourceSubjectResp = req(OptActionKind.FETCH, "/console/app/resource/subject/" + resourceSubjectId, null, ResourceSubjectResp.class)._0;
        Assertions.assertEquals("mysql", resourceSubjectResp.getCodePostfix());
        Assertions.assertEquals("MYSQL", resourceSubjectResp.getName());
        Assertions.assertEquals("mysql://xxxxx", resourceSubjectResp.getUri());

        // 获取当前应用的资源主体列表信息
        var resourceSubjectResps = reqList("/console/app/resource/subject", ResourceSubjectResp.class)._0;
        Assertions.assertTrue(resourceSubjectResps.size() >= 4);
        Assertions.assertTrue(resourceSubjectResps.stream().anyMatch(r -> r.getName().equals("MYSQL")));
        resourceSubjectResps = reqList("/console/app/resource/subject?kind=" + ResourceKind.RELDB.toString(), ResourceSubjectResp.class)._0;
        Assertions.assertTrue(resourceSubjectResps.size() >= 2);
        Assertions.assertTrue(resourceSubjectResps.stream().anyMatch(r -> r.getName().equals("MYSQL")));
        resourceSubjectResps = reqList("/console/app/resource/subject?kind=" + ResourceKind.HTTP.toString(), ResourceSubjectResp.class)._0;
        Assertions.assertTrue(resourceSubjectResps.size() >= 1);
        Assertions.assertTrue(resourceSubjectResps.stream().anyMatch(r -> r.getName().equals("用户权限中心 APIs")));
        resourceSubjectResps = reqList("/console/app/resource/subject?kind=" + ResourceKind.HTTP.toString() + "&name=权限", ResourceSubjectResp.class)._0;
        Assertions.assertEquals(1, resourceSubjectResps.size());
        Assertions.assertEquals("用户权限中心 APIs", resourceSubjectResps.get(0).getName());
        resourceSubjectResps = reqList("/console/app/resource/subject?kind=" + ResourceKind.HTTP.toString() + "&name=权限系统", ResourceSubjectResp.class)._0;
        Assertions.assertEquals(0, resourceSubjectResps.size());

        // 添加当前应用的资源
        var resourceId = req(OptActionKind.CREATE, "/console/app/resource", ResourceAddReq.builder()
                .name("MYSQL IAM DB")
                .pathAndQuery("/mysql")
                .relResourceSubjectId(resourceSubjectId)
                .build(), Long.class)._0;
        Assertions.assertEquals("资源URI已存在", req(OptActionKind.CREATE, "/console/app/resource", ResourceAddReq.builder()
                .name("MYSQL IAM DB")
                .pathAndQuery("/mysql/")
                .relResourceSubjectId(resourceSubjectId)
                .build(), Long.class)._1.getMessage());

        // 修改当前应用的某个资源
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/app/resource/" + resourceId, ResourceModifyReq.builder()
                .pathAndQuery("/")
                .sort(100)
                .build(), Void.class)._1);

        // 获取当前应用的某个资源信息
        var resourceResp = req(OptActionKind.FETCH, "/console/app/resource/" + resourceId, null, ResourceResp.class)._0;
        Assertions.assertEquals("", resourceResp.getPathAndQuery());

        // 获取当前应用的资源列表信息
        var resourceResps = reqList("/console/app/resource", ResourceResp.class)._0;
        Assertions.assertTrue(resourceResps.size() >= 5);
        Assertions.assertTrue(resourceResps.stream().anyMatch(r -> r.getName().equals("MYSQL IAM DB")));

        // 删除当前应用的某个资源
        Assertions.assertNull(req(OptActionKind.DELETE, "/console/app/resource/" + resourceId, null, Void.class)._1);
        Assertions.assertNotNull(req(OptActionKind.FETCH, "/console/app/resource/" + resourceId, null, ResourceResp.class)._1);

        // --------------------------------------------------------------------

        // 删除当前应用的某个资源主体
        Assertions.assertNull(req(OptActionKind.DELETE, "/console/app/resource/subject/" + resourceSubjectId, null, Void.class)._1);
        Assertions.assertNotNull(req(OptActionKind.FETCH, "/console/app/resource/subject/" + resourceSubjectId, null, ResourceSubjectResp.class)._1);

        testContext.completeNow();
    }

    @Test
    public void testGroup(Vertx vertx, VertxTestContext testContext) {
        // 添加当前应用的群组
        var groupId = req(OptActionKind.CREATE, "/console/app/group", GroupAddReq.builder()
                .code("test")
                .name("测试群组")
                .kind(GroupKind.ADMINISTRATION)
                .build(), Long.class)._0;
        Assertions.assertEquals("群组编码已存在", req(OptActionKind.CREATE, "/console/app/group", GroupAddReq.builder()
                .code("test")
                .name("测试群组")
                .kind(GroupKind.ADMINISTRATION)
                .build(), Long.class)._1.getMessage());

        // 修改当前应用的某个群组
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/app/group/" + groupId, GroupModifyReq.builder()
                .code("test2")
                .sort(100)
                .build(), Void.class)._1);

        // 获取当前应用的某个群组信息
        var groupResp = req(OptActionKind.FETCH, "/console/app/group/" + groupId, null, GroupResp.class)._0;
        Assertions.assertEquals("test2", groupResp.getCode());
        Assertions.assertEquals("测试群组", groupResp.getName());

        // 获取当前应用的群组列表信息
        var groupResps = reqList("/console/app/group", GroupResp.class)._0;
        Assertions.assertEquals(2, groupResps.size());
        Assertions.assertEquals("测试群组", groupResps.get(1).getName());

        // 添加当前应用某个群组的节点
        var roleNodeId = req(OptActionKind.CREATE, "/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("理想世界")
                .busCode("xxx")
                .build(), Long.class)._0;
        Assertions.assertEquals("找不到对应的关联群组", req(OptActionKind.CREATE, "/console/app/group/10/node", GroupNodeAddReq.builder()
                .name("理想世界")
                .build(), Long.class)._1.getMessage());


        // 修改当前应用某个群组的节点
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/app/group/" + groupId + "/node/" + roleNodeId, GroupNodeModifyReq.builder()
                .name("Ideal World")
                .build(), Void.class)._1);

        // 获取当前应用某个群组的节点列表信息
        var groupNodeResps = reqList("/console/app/group/" + groupId + "/node", GroupNodeResp.class)._0;
        Assertions.assertEquals(1, groupNodeResps.size());
        Assertions.assertEquals("Ideal World", groupNodeResps.get(0).getName());
        Assertions.assertEquals("xxx", groupNodeResps.get(0).getBusCode());

        // 删除当前应用某个群组的节点
        Assertions.assertNull(req(OptActionKind.DELETE, "/console/app/group/" + groupId + "/node/" + roleNodeId, null, Void.class)._1);
        Assertions.assertEquals(0, reqList("/console/app/group/" + groupId + "/node", GroupNodeResp.class)._0.size());

        // --------------------------------------------------------------------

        // 群组编码测试
        // 10000
        var rootNodeId = req(OptActionKind.CREATE, "/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("10000")
                .build(), Long.class)._0;
        // 10001
        var root2NodeId = req(OptActionKind.CREATE, "/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("10001")
                .siblingId(rootNodeId)
                .build(), Long.class)._0;
        // 1000110000 -> (修改后)1000010000
        var subNodeId = req(OptActionKind.CREATE, "/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("1000010000")
                .parentId(root2NodeId)
                .build(), Long.class)._0;
        // 1000110001 -> (修改后)1000010001
        var siblingNodeId = req(OptActionKind.CREATE, "/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("1000010001")
                .parentId(root2NodeId)
                .siblingId(subNodeId)
                .build(), Long.class)._0;
        // 100011000110000 -> (插入后)100011000110001 -> (修改后)100001000110001
        var subSubNodeId = req(OptActionKind.CREATE, "/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("100001000110001")
                .parentId(siblingNodeId)
                .build(), Long.class)._0;
        // 100011000110001 -> 100011000110002 -> (插入后)100011000110004 -> (修改后)100001000110004 -> (删除后)100001000110003
        var siblingSubNodeId = req(OptActionKind.CREATE, "/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("100001000110003")
                .parentId(siblingNodeId)
                .siblingId(subSubNodeId)
                .build(), Long.class)._0;
        // --- 插入节点
        // 100011000110000 -> (修改后)100001000110000
        req(OptActionKind.CREATE, "/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("100001000110000")
                .parentId(siblingNodeId)
                .build(), Long.class);
        // 100011000110003 -> (修改后)100001000110003 -> (删除后)100001000110002
        req(OptActionKind.CREATE, "/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("100001000110002")
                .parentId(siblingNodeId)
                .siblingId(subSubNodeId)
                .build(), Long.class);
        // 100011000110002 -> (修改后)100001000110002 -> (删除后)null
        var needDeleteNodeId = req(OptActionKind.CREATE, "/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("null")
                .parentId(siblingNodeId)
                .siblingId(subSubNodeId)
                .build(), Long.class)._0;
        // --- 修改节点
        req(OptActionKind.PATCH, "/console/app/group/" + groupId + "/node/" + subNodeId, GroupNodeModifyReq.builder()
                .parentId(rootNodeId)
                .build(), Void.class);
        // --- 删除节点
        req(OptActionKind.DELETE, "/console/app/group/" + groupId + "/node/" + needDeleteNodeId, null, Void.class);
        groupNodeResps = reqList("/console/app/group/" + groupId + "/node", GroupNodeResp.class)._0;
        Assertions.assertEquals(8, groupNodeResps.size());
        Assertions.assertEquals("10000", groupNodeResps.get(0).getName());
        Assertions.assertEquals("1000010000", groupNodeResps.get(1).getName());
        Assertions.assertEquals("1000010001", groupNodeResps.get(2).getName());
        Assertions.assertEquals("100001000110000", groupNodeResps.get(3).getName());
        Assertions.assertEquals("100001000110001", groupNodeResps.get(4).getName());
        Assertions.assertEquals("100001000110002", groupNodeResps.get(5).getName());
        Assertions.assertEquals("100001000110003", groupNodeResps.get(6).getName());
        Assertions.assertEquals("10001", groupNodeResps.get(7).getName());

        // --------------------------------------------------------------------

        // 删除当前应用的某个群组
        Assertions.assertEquals("请先删除关联的群组节点数据", req(OptActionKind.DELETE, "/console/app/group/" + groupId, null, Void.class)._1.getMessage());

        testContext.completeNow();
    }

    @Test
    public void testRole(Vertx vertx, VertxTestContext testContext) {
        // 添加当前应用的角色定义
        var roleDefId = req(OptActionKind.CREATE, "/console/app/role/def", RoleDefAddReq.builder()
                .code("root")
                .name("管理员")
                .build(), Long.class)._0;
        Assertions.assertEquals("角色定义编码已存在", req(OptActionKind.CREATE, "/console/app/role/def", RoleDefAddReq.builder()
                .code("Root")
                .name("管理员")
                .build(), Long.class)._1.getMessage());

        // 修改当前应用的某个角色定义
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/app/role/def/" + roleDefId, RoleDefModifyReq.builder()
                .code("admin")
                .sort(100)
                .build(), Void.class)._1);

        // 获取当前应用的某个角色定义信息
        var roleDefResp = req(OptActionKind.FETCH, "/console/app/role/def/" + roleDefId, null, RoleDefResp.class)._0;
        Assertions.assertEquals("admin", roleDefResp.getCode());
        Assertions.assertEquals("管理员", roleDefResp.getName());

        // 获取当前应用的角色定义列表信息
        var roleDefResps = reqPage("/console/app/role/def", 1L, 10L, RoleDefResp.class)._0;
        Assertions.assertEquals(4, roleDefResps.getRecordTotal());
        Assertions.assertTrue(roleDefResps.getObjects().stream().anyMatch(r -> r.getName().contains("管理员")));

        // --------------------------------------------------------------------

        // 添加当前应用的角色
        var roleId = req(OptActionKind.CREATE, "/console/app/role", RoleAddReq.builder()
                .relRoleDefId(roleDefId)
                .build(), Long.class)._0;
        Assertions.assertEquals("角色已存在", req(OptActionKind.CREATE, "/console/app/role", RoleAddReq.builder()
                .relRoleDefId(roleDefId)
                .build(), Long.class)._1.getMessage());
        Assertions.assertEquals("找不到对应的群组节点", req(OptActionKind.CREATE, "/console/app/role", RoleAddReq.builder()
                .relRoleDefId(roleDefId)
                .relGroupNodeId(100L)
                .build(), Long.class)._1.getMessage());

        // 获取当前应用的某个角色信息
        var roleResp = req(OptActionKind.FETCH, "/console/app/role/" + roleId, null, RoleResp.class)._0;
        Assertions.assertEquals("管理员", roleResp.getName());

        // 修改当前应用的某个角色
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/app/role/" + roleId, RoleModifyReq.builder()
                .name("测试管理员")
                .sort(100)
                .build(), Void.class)._1);

        // 获取当前应用的角色列表信息
        var roleResps = reqList("/console/app/role", RoleResp.class)._0;
        Assertions.assertEquals(4, roleResps.size());
        Assertions.assertEquals("测试管理员", roleResps.get(3).getName());

        // 删除当前应用的某个角色
        Assertions.assertNull(req(OptActionKind.DELETE, "/console/app/role/" + roleId, null, Void.class)._1);
        Assertions.assertNotNull(req(OptActionKind.FETCH, "/console/app/role/" + roleId, null, RoleResp.class)._1);

        // --------------------------------------------------------------------

        // 删除当前应用的某个角色定义
        Assertions.assertNull(req(OptActionKind.DELETE, "/console/app/role/def/" + roleDefId, null, Void.class)._1);
        Assertions.assertNotNull(req(OptActionKind.FETCH, "/console/app/role/def/" + roleDefId, null, RoleDefResp.class)._1);

        testContext.completeNow();
    }

    @Test
    public void testAuthPolicy(Vertx vertx, VertxTestContext testContext) {
        // 添加当前应用的权限策略
        Assertions.assertNull(req(OptActionKind.CREATE, "/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ACCOUNT)
                .relSubjectIds("1, ")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(1L)
                .actionKind(OptActionKind.CREATE)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Void.class)._1);
        var authPolicyId = reqPage("/console/app/authpolicy", 1L, 1L, AuthPolicyResp.class)._0.getObjects().get(0).getId();
        Assertions.assertEquals("找不到对应的权限策略", req(OptActionKind.CREATE, "/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ACCOUNT)
                .relSubjectIds("1")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(10L)
                .actionKind(OptActionKind.CREATE)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Void.class)._1.getMessage());
        Assertions.assertEquals("权限策略已存在", req(OptActionKind.CREATE, "/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ACCOUNT)
                .relSubjectIds("1")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(1L)
                .actionKind(OptActionKind.CREATE)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Void.class)._1.getMessage());

        // 修改当前应用的某个权限策略
        var expiredTime = new Date().getTime();
        Assertions.assertNull(req(OptActionKind.PATCH, "/console/app/authpolicy/" + authPolicyId, AuthPolicyModifyReq.builder()
                .expiredTime(expiredTime)
                .build(), Void.class)._1);

        // 获取当前应用的某个权限策略信息
        var authPolicyResp = req(OptActionKind.FETCH, "/console/app/authpolicy/" + authPolicyId, null, AuthPolicyResp.class)._0;
        Assertions.assertEquals("1,", authPolicyResp.getRelSubjectIds());
        Assertions.assertEquals(expiredTime, authPolicyResp.getExpiredTime());

        // 获取当前应用的权限策略列表信息
        var authPolicyResps = reqPage("/console/app/authpolicy", 1L, 100L, AuthPolicyResp.class)._0;
        Assertions.assertEquals(1, authPolicyResps.getPageTotal());

        // 删除当前应用的某个权限策略
        Assertions.assertNull(req(OptActionKind.DELETE, "/console/app/authpolicy/" + authPolicyId, null, Void.class)._1);
        Assertions.assertNotNull(req(OptActionKind.FETCH, "/console/app/authpolicy/" + authPolicyId, null, AuthPolicyResp.class)._1);

        testContext.completeNow();
    }

}
