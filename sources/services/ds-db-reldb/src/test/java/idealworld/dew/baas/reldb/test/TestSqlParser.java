package idealworld.dew.baas.reldb.test;

import idealworld.dew.baas.common.enumeration.OptActionKind;
import idealworld.dew.baas.reldb.process.SqlParser;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSqlParser extends BasicTest {

    @Test
    public void testSqlParser(Vertx vertx, VertxTestContext testContext) {
        var sqlAst = SqlParser.parse("insert into t1(name,age) values (?, ?)");
        Assertions.assertEquals(sqlAst.get(0).getActionKind(), OptActionKind.CREATE);
        Assertions.assertEquals(sqlAst.get(0).getTable(), "t1");
        Assertions.assertEquals(sqlAst.get(0).getEffectFields().get(0), "name");
        Assertions.assertEquals(sqlAst.get(0).getEffectFields().get(1), "age");

        sqlAst = SqlParser.parse("update main set name = ? and age = ? where id = ?");
        Assertions.assertEquals(sqlAst.get(0).getActionKind(), OptActionKind.MODIFY);
        Assertions.assertEquals(sqlAst.get(0).getTable(), "main");
        Assertions.assertEquals(sqlAst.get(0).getEffectFields().get(0), "name");
        Assertions.assertEquals(sqlAst.get(0).getEffectFields().get(1), "age");
        Assertions.assertEquals(sqlAst.get(0).getCondFields().get(0), "id");

        sqlAst = SqlParser.parse("delete from main " +
                "where age > ? and main.name != ? and status in (?, ?) " +
                "   or main.addr <> ? and org_code = (select t2.code from org t2 where t2.id = ?)" +
                "   or xx between ? and ?");
        Assertions.assertEquals(sqlAst.get(0).getActionKind(), OptActionKind.FETCH);
        Assertions.assertEquals(sqlAst.get(0).getTable(), "org");
        Assertions.assertEquals(sqlAst.get(0).getReturnFields().get(0), "code");
        Assertions.assertEquals(sqlAst.get(0).getCondFields().get(0), "id");
        Assertions.assertEquals(sqlAst.get(1).getActionKind(), OptActionKind.DELETE);
        Assertions.assertEquals(sqlAst.get(1).getTable(), "main");
        Assertions.assertEquals(sqlAst.get(1).getCondFields().get(0), "age");
        Assertions.assertEquals(sqlAst.get(1).getCondFields().get(1), "name");
        Assertions.assertEquals(sqlAst.get(1).getCondFields().get(2), "status");
        Assertions.assertEquals(sqlAst.get(1).getCondFields().get(3), "addr");
        Assertions.assertEquals(sqlAst.get(1).getCondFields().get(4), "org_code");
        Assertions.assertEquals(sqlAst.get(1).getCondFields().get(5), "xx");

        sqlAst = SqlParser.parse("select t1.name, t1.age from main t1 where t1.age > ?");
        Assertions.assertEquals(sqlAst.get(0).getActionKind(), OptActionKind.FETCH);
        Assertions.assertEquals(sqlAst.get(0).getTable(), "main");
        Assertions.assertEquals(sqlAst.get(0).getReturnFields().get(0), "name");
        Assertions.assertEquals(sqlAst.get(0).getReturnFields().get(1), "age");
        Assertions.assertEquals(sqlAst.get(0).getCondFields().get(0), "age");

        sqlAst = SqlParser.parse("select t1.name, t1.age from main t1 left join org o on t1.org_id = o.id and o.status = 1 where t1.age > ? and t1.enabled = true");
        Assertions.assertEquals(sqlAst.get(0).getActionKind(), OptActionKind.FETCH);
        Assertions.assertEquals(sqlAst.get(1).getActionKind(), OptActionKind.FETCH);
        Assertions.assertEquals(sqlAst.get(0).getTable(), "org");
        Assertions.assertEquals(sqlAst.get(0).getCondFields().get(0), "id");
        Assertions.assertEquals(sqlAst.get(0).getCondFields().get(1), "status");
        Assertions.assertEquals(sqlAst.get(1).getTable(), "main");
        Assertions.assertEquals(sqlAst.get(1).getReturnFields().get(0), "name");
        Assertions.assertEquals(sqlAst.get(1).getReturnFields().get(1), "age");
        Assertions.assertEquals(sqlAst.get(1).getCondFields().get(0), "org_id");
        Assertions.assertEquals(sqlAst.get(1).getCondFields().get(1), "age");
        Assertions.assertEquals(sqlAst.get(1).getCondFields().get(2), "enabled");
        testContext.completeNow();
    }

}
