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

import idealworld.dew.serviceless.common.enumeration.*;
import idealworld.dew.serviceless.iam.enumeration.GroupKind;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.app.AppIdentAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.app.AppIdentModifyReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.app.AppIdentResp;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.authpolicy.AuthPolicyAddReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.authpolicy.AuthPolicyModifyReq;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.authpolicy.AuthPolicyResp;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.group.*;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.resource.*;
import idealworld.dew.serviceless.iam.scene.appconsole.dto.role.*;
import idealworld.dew.serviceless.iam.scene.systemconsole.dto.TenantAddReq;
import idealworld.dew.serviceless.iam.test.BasicTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

public class AppConsoleTest extends BasicTest {

    @BeforeEach
    public void before() {
        loginBySystemAdmin();
        // 租户注册
        var tenantId = postToEntity("/console/system/tenant", TenantAddReq.builder()
                .name("xyy")
                .build(), Long.class).getBody();
    }

    @Test
    public void testApp() {
        // 添加当前应用的认证
        var appIdentId = postToEntity("/console/app/app/ident", AppIdentAddReq.builder()
                .note("测试用")
                .build(), Long.class).getBody();

        // 修改当前应用的某个认证
        Assertions.assertTrue(patchToEntity("/console/app/app/ident/" + appIdentId, AppIdentModifyReq.builder()
                .note("use test")
                .build(), Void.class).ok());

        // 获取当前应用的认证列表信息
        var appIdentResps = getToPage("/console/app/app/ident", 1L, 10, AppIdentResp.class).getBody();
        Assertions.assertEquals(2, appIdentResps.getRecordTotal());
        Assertions.assertEquals(1, appIdentResps.getPageTotal());
        Assertions.assertEquals("use test", appIdentResps.getObjects().get(1).getNote());

        // 获取当前应用的某个认证SK
        var sk = getToEntity("/console/app/app/ident/" + appIdentId + "/sk/", String.class).getBody();
        Assertions.assertNotNull(sk);

        // 删除当前应用的某个认证
        Assertions.assertTrue(delete("/console/app/app/ident/" + appIdentId).ok());
        Assertions.assertNull(getToEntity("/console/app/app/ident/" + appIdentId + "/sk/", String.class).getBody());

    }

    @Test
    public void testResource() {
        // 添加当前应用的资源主体
        var resourceSubjectId = postToEntity("/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .code("defaultmysql")
                .kind(ResourceKind.RELDB)
                .name("MYSQL")
                .uri("jdbc://xxxxx")
                .build(), Long.class).getBody();
        Assertions.assertEquals("资源主体编码已存在", postToEntity("/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .code("defaultmysql")
                .kind(ResourceKind.RELDB)
                .name("mysql")
                .uri("jdbc://xxxxx")
                .build(), Long.class).getMessage());
        Assertions.assertEquals("资源主体URI已存在", postToEntity("/console/app/resource/subject", ResourceSubjectAddReq.builder()
                .code("defaultmysql2")
                .kind(ResourceKind.RELDB)
                .name("mysql")
                .uri("jdbc://xxxxx")
                .build(), Long.class).getMessage());

        // 修改当前应用的某个资源主体
        Assertions.assertTrue(patchToEntity("/console/app/resource/subject/" + resourceSubjectId, ResourceSubjectModifyReq.builder()
                .code("mysql")
                .sort(100)
                .build(), Void.class).ok());

        // 获取当前应用的某个资源主体信息
        var resourceSubjectResp = getToEntity("/console/app/resource/subject/" + resourceSubjectId, ResourceSubjectResp.class).getBody();
        Assertions.assertEquals("mysql", resourceSubjectResp.getCode());
        Assertions.assertEquals("MYSQL", resourceSubjectResp.getName());
        Assertions.assertEquals("jdbc://xxxxx/", resourceSubjectResp.getUri());

        // 获取当前应用的资源主体列表信息
        var resourceSubjectResps = getToList("/console/app/resource/subject", ResourceSubjectResp.class).getBody();
        Assertions.assertEquals(2, resourceSubjectResps.size());
        Assertions.assertEquals("MYSQL", resourceSubjectResps.get(1).getName());

        // 添加当前应用的资源
        var resourceId = postToEntity("/console/app/resource", ResourceAddReq.builder()
                .name("MYSQL IAM DB")
                .uri("reldb://mysql")
                .relResourceSubjectId(resourceSubjectId)
                .build(), Long.class).getBody();
        Assertions.assertEquals("资源URI已存在", postToEntity("/console/app/resource", ResourceAddReq.builder()
                .name("MYSQL IAM DB")
                .uri("reldb://mysql/")
                .relResourceSubjectId(resourceSubjectId)
                .build(), Long.class).getMessage());

        // 获取当前应用的某个角色信息
        Assertions.assertTrue(patchToEntity("/console/app/resource/" + resourceId, ResourceModifyReq.builder()
                .uri("reldb://mysql/")
                .sort(100)
                .build(), Void.class).ok());

        // 获取当前应用的某个资源信息
        var resourceResp = getToEntity("/console/app/resource/" + resourceId, ResourceResp.class).getBody();
        Assertions.assertEquals("reldb://mysql/", resourceResp.getUri());

        // 获取当前应用的资源列表信息
        var resourceResps = getToList("/console/app/resource", ResourceResp.class).getBody();
        Assertions.assertEquals(4, resourceResps.size());
        Assertions.assertEquals("MYSQL IAM DB", resourceResps.get(3).getName());

        // 删除当前应用的某个角色定义
        Assertions.assertTrue(delete("/console/app/resource/" + resourceId).ok());
        Assertions.assertFalse(getToEntity("/console/app/resource/" + resourceId, ResourceResp.class).ok());

        // --------------------------------------------------------------------

        // 删除当前应用的某个资源
        Assertions.assertTrue(delete("/console/app/resource/subject/" + resourceSubjectId).ok());
        Assertions.assertFalse(getToEntity("/console/app/resource/subject/" + resourceSubjectId, ResourceSubjectResp.class).ok());

    }

    @Test
    public void testGroup() {
        // 添加当前应用的群组
        var groupId = postToEntity("/console/app/group", GroupAddReq.builder()
                .code("test")
                .name("测试群组")
                .kind(GroupKind.ADMINISTRATION)
                .build(), Long.class).getBody();
        Assertions.assertEquals("群组编码已存在", postToEntity("/console/app/group", GroupAddReq.builder()
                .code("test")
                .name("测试群组")
                .kind(GroupKind.ADMINISTRATION)
                .build(), Long.class).getMessage());

        // 修改当前应用的某个群组
        Assertions.assertTrue(patchToEntity("/console/app/group/" + groupId, GroupModifyReq.builder()
                .code("test2")
                .sort(100)
                .build(), Void.class).ok());

        // 获取当前应用的某个群组信息
        var groupResp = getToEntity("/console/app/group/" + groupId, GroupResp.class).getBody();
        Assertions.assertEquals("test2", groupResp.getCode());
        Assertions.assertEquals("测试群组", groupResp.getName());

        // 获取当前应用的群组列表信息
        var groupResps = getToList("/console/app/group", GroupResp.class).getBody();
        Assertions.assertEquals(2, groupResps.size());
        Assertions.assertEquals("测试群组", groupResps.get(1).getName());

        // 添加当前应用某个群组的节点
        var roleNodeId = postToEntity("/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("理想世界")
                .busCode("xxx")
                .build(), Long.class).getBody();
        Assertions.assertEquals("关联群组不合法", postToEntity("/console/app/group/10/node", GroupNodeAddReq.builder()
                .name("理想世界")
                .build(), Long.class).getMessage());


        // 修改当前应用某个群组的节点
        Assertions.assertTrue(patchToEntity("/console/app/group/" + groupId + "/node/" + roleNodeId, GroupNodeModifyReq.builder()
                .name("Ideal World")
                .build(), Void.class).ok());

        // 获取当前应用某个群组的节点列表信息
        var groupNodeResps = getToList("/console/app/group/" + groupId + "/node", GroupNodeResp.class).getBody();
        Assertions.assertEquals(1, groupNodeResps.size());
        Assertions.assertEquals("Ideal World", groupNodeResps.get(0).getName());
        Assertions.assertEquals("xxx", groupNodeResps.get(0).getBusCode());

        // 删除当前应用某个群组的节点
        Assertions.assertTrue(delete("/console/app/group/" + groupId + "/node/" + roleNodeId).ok());
        Assertions.assertEquals(0, getToList("/console/app/group/" + groupId + "/node", GroupNodeResp.class).getBody().size());

        // --------------------------------------------------------------------

        // 群组编码测试
        // 10000
        var rootNodeId = postToEntity("/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("10000")
                .build(), Long.class).getBody();
        // 10001
        var root2NodeId = postToEntity("/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("10001")
                .siblingId(rootNodeId)
                .build(), Long.class).getBody();
        // 1000110000 -> (修改后)1000010000
        var subNodeId = postToEntity("/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("1000010000")
                .parentId(root2NodeId)
                .build(), Long.class).getBody();
        // 1000110001 -> (修改后)1000010001
        var siblingNodeId = postToEntity("/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("1000010001")
                .parentId(root2NodeId)
                .siblingId(subNodeId)
                .build(), Long.class).getBody();
        // 100011000110000 -> (插入后)100011000110001 -> (修改后)100001000110001
        var subSubNodeId = postToEntity("/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("100001000110001")
                .parentId(siblingNodeId)
                .build(), Long.class).getBody();
        // 100011000110001 -> 100011000110002 -> (插入后)100011000110004 -> (修改后)100001000110004 -> (删除后)100001000110003
        var siblingSubNodeId = postToEntity("/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("100001000110003")
                .parentId(siblingNodeId)
                .siblingId(subSubNodeId)
                .build(), Long.class).getBody();
        // --- 插入节点
        // 100011000110000 -> (修改后)100001000110000
        postToEntity("/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("100001000110000")
                .parentId(siblingNodeId)
                .build(), Long.class);
        // 100011000110003 -> (修改后)100001000110003 -> (删除后)100001000110002
        postToEntity("/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("100001000110002")
                .parentId(siblingNodeId)
                .siblingId(subSubNodeId)
                .build(), Long.class);
        // 100011000110002 -> (修改后)100001000110002 -> (删除后)null
        var needDeleteNodeId = postToEntity("/console/app/group/" + groupId + "/node", GroupNodeAddReq.builder()
                .name("null")
                .parentId(siblingNodeId)
                .siblingId(subSubNodeId)
                .build(), Long.class).getBody();
        // --- 修改节点
        patchToEntity("/console/app/group/" + groupId + "/node/" + subNodeId, GroupNodeModifyReq.builder()
                .parentId(rootNodeId)
                .build(), Void.class);
        // --- 删除节点
        delete("/console/app/group/" + groupId + "/node/" + needDeleteNodeId);
        groupNodeResps = getToList("/console/app/group/" + groupId + "/node", GroupNodeResp.class).getBody();
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
        Assertions.assertEquals("请先删除关联的群组节点数据", delete("/console/app/group/" + groupId).getMessage());

    }

    @Test
    public void testRole() {
        // 添加当前应用的角色定义
        var roleDefId = postToEntity("/console/app/role/def", RoleDefAddReq.builder()
                .code("root")
                .name("管理员")
                .build(), Long.class).getBody();
        Assertions.assertEquals("角色定义编码已存在", postToEntity("/console/app/role/def", RoleDefAddReq.builder()
                .code("Root")
                .name("管理员")
                .build(), Long.class).getMessage());

        // 修改当前应用的某个角色定义
        Assertions.assertTrue(patchToEntity("/console/app/role/def/" + roleDefId, RoleDefModifyReq.builder()
                .code("admin")
                .sort(100)
                .build(), Void.class).ok());

        // 获取当前应用的某个角色定义信息
        var roleDefResp = getToEntity("/console/app/role/def/" + roleDefId, RoleDefResp.class).getBody();
        Assertions.assertEquals("admin", roleDefResp.getCode());
        Assertions.assertEquals("管理员", roleDefResp.getName());

        // 获取当前应用的角色定义列表信息
        var roleDefResps = getToList("/console/app/role/def", RoleDefResp.class).getBody();
        Assertions.assertEquals(4, roleDefResps.size());
        Assertions.assertEquals("管理员", roleDefResps.get(3).getName());

        // --------------------------------------------------------------------

        // 添加当前应用的角色
        var roleId = postToEntity("/console/app/role", RoleAddReq.builder()
                .relRoleDefId(roleDefId)
                .build(), Long.class).getBody();
        Assertions.assertEquals("角色已存在", postToEntity("/console/app/role", RoleAddReq.builder()
                .relRoleDefId(roleDefId)
                .build(), Long.class).getMessage());
        Assertions.assertEquals("对应的群组节点不合法", postToEntity("/console/app/role", RoleAddReq.builder()
                .relRoleDefId(roleDefId)
                .relGroupNodeId(100L)
                .build(), Long.class).getMessage());

        // 获取当前应用的某个角色信息
        var roleResp = getToEntity("/console/app/role/" + roleId, RoleResp.class).getBody();
        Assertions.assertEquals("管理员", roleResp.getName());

        // 修改当前应用的某个角色
        Assertions.assertTrue(patchToEntity("/console/app/role/" + roleId, RoleModifyReq.builder()
                .name("测试管理员")
                .sort(100)
                .build(), Void.class).ok());

        // 获取当前应用的角色列表信息
        var roleResps = getToList("/console/app/role", RoleResp.class).getBody();
        Assertions.assertEquals(4, roleResps.size());
        Assertions.assertEquals("测试管理员", roleResps.get(3).getName());

        // 删除当前应用的某个角色
        Assertions.assertTrue(delete("/console/app/role/" + roleId).ok());
        Assertions.assertFalse(getToEntity("/console/app/role/" + roleId, RoleResp.class).ok());

        // --------------------------------------------------------------------

        // 删除当前应用的某个角色定义
        Assertions.assertTrue(delete("/console/app/role/def/" + roleDefId).ok());
        Assertions.assertFalse(getToEntity("/console/app/role/def/" + roleDefId, RoleDefResp.class).ok());

    }

    @Test
    public void testAuthPolicy() {
        // 添加当前应用的权限策略
        var authPolicyId = postToList("/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ACCOUNT)
                .relSubjectIds("1, ")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(1L)
                .actionKind(OptActionKind.CREATE)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Long.class).getBody().get(0);
        Assertions.assertEquals("权限策略对应的资源不合法", postToList("/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ACCOUNT)
                .relSubjectIds("1")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(10L)
                .actionKind(OptActionKind.CREATE)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Long.class).getMessage());
        Assertions.assertEquals("权限策略已存在", postToList("/console/app/authpolicy", AuthPolicyAddReq.builder()
                .relSubjectKind(AuthSubjectKind.ACCOUNT)
                .relSubjectIds("1")
                .subjectOperator(AuthSubjectOperatorKind.EQ)
                .relResourceId(1L)
                .actionKind(OptActionKind.CREATE)
                .resultKind(AuthResultKind.ACCEPT)
                .build(), Long.class).getMessage());

        // 修改当前应用的某个权限策略
        var expiredTime = new Date();
        Assertions.assertTrue(patchToEntity("/console/app/authpolicy/" + authPolicyId, AuthPolicyModifyReq.builder()
                .expiredTime(expiredTime)
                .build(), Void.class).ok());

        // 获取当前应用的某个权限策略信息
        var authPolicyResp = getToEntity("/console/app/authpolicy/" + authPolicyId, AuthPolicyResp.class).getBody();
        Assertions.assertEquals("1,", authPolicyResp.getRelSubjectIds());
        Assertions.assertEquals(expiredTime, authPolicyResp.getExpiredTime());

        // 获取当前应用的权限策略列表信息
        var authPolicyResps = getToPage("/console/app/authpolicy", 1L, 100, AuthPolicyResp.class).getBody();
        Assertions.assertEquals(19, authPolicyResps.getRecordTotal());
        Assertions.assertEquals(1, authPolicyResps.getPageTotal());
        Assertions.assertEquals(1, authPolicyResps.getObjects().get(18).getRelResourceId());

        // 删除当前应用的某个权限策略
        Assertions.assertTrue(delete("/console/app/authpolicy/" + authPolicyId).ok());
        Assertions.assertFalse(getToEntity("/console/app/authpolicy/" + authPolicyId, AuthPolicyResp.class).ok());

    }

}
