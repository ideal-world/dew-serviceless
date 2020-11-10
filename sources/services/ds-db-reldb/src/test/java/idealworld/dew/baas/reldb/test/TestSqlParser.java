package idealworld.dew.baas.reldb.test;

import idealworld.dew.baas.common.enumeration.OptActionKind;
import idealworld.dew.baas.reldb.process.SqlParser;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

public class TestSqlParser extends BasicTest {

    @Before
    public void before(TestContext testContext) {

    }

    @Test
    public void testSqlParser(TestContext testContext) {
        var sqlAst = SqlParser.parse("insert into t1(name,age) values (?, ?)");
        testContext.assertEquals(sqlAst.get(0).getActionKind(), OptActionKind.CREATE);
        testContext.assertEquals(sqlAst.get(0).getTable(), "t1");
        testContext.assertEquals(sqlAst.get(0).getEffectFields().get(0), "name");
        testContext.assertEquals(sqlAst.get(0).getEffectFields().get(1), "age");

        sqlAst = SqlParser.parse("delete from main " +
                "where age > ? and main.name != ? and status in (?, ?) " +
                "   or main.addr <> ? and org_code = (select t2.code from org t2 where t2.id = ?)" +
                "   or xx between ? and ?");
        testContext.assertEquals(sqlAst.get(0).getActionKind(), OptActionKind.FETCH);
        testContext.assertEquals(sqlAst.get(0).getTable(), "org");
        testContext.assertEquals(sqlAst.get(0).getReturnFields().get(0), "code");
        testContext.assertEquals(sqlAst.get(0).getCondFields().get(0), "id");
        testContext.assertEquals(sqlAst.get(1).getActionKind(), OptActionKind.DELETE);
        testContext.assertEquals(sqlAst.get(1).getTable(), "main");
        testContext.assertEquals(sqlAst.get(1).getCondFields().get(0), "age");
        testContext.assertEquals(sqlAst.get(1).getCondFields().get(1), "name");
        testContext.assertEquals(sqlAst.get(1).getCondFields().get(2), "status");
        testContext.assertEquals(sqlAst.get(1).getCondFields().get(3), "addr");
        testContext.assertEquals(sqlAst.get(1).getCondFields().get(4), "org_code");
        testContext.assertEquals(sqlAst.get(1).getCondFields().get(5), "xx");

        /*sqlAst = SqlParser.parse("select t1.name, t1.age from t1 where t1.age > ? and t1.name != ?");
        testContext.assertEquals(sqlAst.getActionKind(), OptActionKind.FETCH);
        testContext.assertEquals(sqlAst.getTableFields().get(0).getTable(), "t1");
        testContext.assertEquals(sqlAst.getTableFields().get(0).getCondFields().get(0), "name");
        testContext.assertEquals(sqlAst.getTableFields().get(0).getCondFields().get(1), "age");
        testContext.assertEquals(sqlAst.getTableFields().get(0).getReturnFields().get(0), "name");
        testContext.assertEquals(sqlAst.getTableFields().get(0).getReturnFields().get(1), "age");*/
    }

}
