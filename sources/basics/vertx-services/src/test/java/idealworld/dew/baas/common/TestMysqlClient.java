package idealworld.dew.baas.common;

import idealworld.dew.baas.common.funs.mysql.MysqlClient;
import io.vertx.core.Future;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class TestMysqlClient extends BasicTest {

    private MysqlClient mysqlClient;

    @Before
    public void before(TestContext testContext) {
        RedisTestHelper.start();
        MysqlClient.init("", rule.vertx(), CommonConfig.JDBCConfig.builder()
                .host("127.0.0.1")
                .db("test")
                .userName("root")
                .password("123456")
                .build());
        mysqlClient = MysqlClient.choose("");
    }

    @Test
    public void testExec(TestContext testContext) {
        Async async = testContext.async();
        Future.succeededFuture()
                .compose(resp ->
                        mysqlClient.exec("drop table if exists iam_account")
                )
                .compose(resp ->
                        mysqlClient.exec("create table if not exists iam_account\n" +
                                "(\n" +
                                "\tid bigint auto_increment primary key,\n" +
                                "\tname varchar(255) not null comment '账号名称',\n" +
                                "\topen_id varchar(100) not null comment 'Open Id',\n" +
                                "\tstatus varchar(50) not null comment '账号状态'\n" +
                                ")\n" +
                                "comment '账号'")
                )
                .compose(resp ->
                        mysqlClient.exec("insert into iam_account(name, open_id, status) values (?, ?, ?)", "孤岛旭日1", "xxxx", "ENABLED")
                )
                .compose(resp ->
                        mysqlClient.execBatch("insert into iam_account(name, open_id, status) values (?, ?, ?)", new ArrayList<>() {
                            {
                                add(new ArrayList<>() {
                                    {
                                        add("孤岛旭日2");
                                        add("xxxx");
                                        add("ENABLED");
                                    }
                                });
                                add(new ArrayList<>() {
                                    {
                                        add("孤岛旭日3");
                                        add("xxxx");
                                        add("ENABLED");
                                    }
                                });
                            }
                        })
                )
                .compose(resp ->
                        mysqlClient.exec("select * from iam_account")
                )
                .onSuccess(resp -> {
                    testContext.assertEquals(3, resp.size());
                    testContext.assertEquals("孤岛旭日1", resp.getJsonObject(0).getString("name"));
                    async.complete();
                });

    }

}
