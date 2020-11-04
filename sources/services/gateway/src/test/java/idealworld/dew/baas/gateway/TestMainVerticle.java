package idealworld.dew.baas.gateway;

import idealworld.dew.baas.helper.RedisTestHelper;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class TestMainVerticle {

    @Rule
    public RunTestOnContext rule = new RunTestOnContext(new VertxOptions()
            .setBlockedThreadCheckInterval(200000));

    @Before
    public void deploy_verticle(TestContext testContext) {
        System.getProperties().put("dew.profile", "test");
        RedisTestHelper.start();
        Vertx vertx = rule.vertx();
        vertx.deployVerticle(new GatewayApplication(), testContext.asyncAssertSuccess());
    }

    @Test
    public void verticle_deployed(TestContext testContext) throws Throwable {
        Async async = testContext.async();
        async.complete();
    }
}
