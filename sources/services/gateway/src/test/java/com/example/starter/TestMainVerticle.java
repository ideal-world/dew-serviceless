package com.example.starter;

import idealworld.dew.baas.gateway.GatewayApplication;
import io.vertx.core.Vertx;
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
  public RunTestOnContext rule = new RunTestOnContext();

  @Before
  public void deploy_verticle(TestContext testContext) {
    Vertx vertx = rule.vertx();
    vertx.deployVerticle(new GatewayApplication(), testContext.asyncAssertSuccess());
  }

  @Test
  public void verticle_deployed(TestContext testContext) throws Throwable {
    Async async = testContext.async();
    async.complete();
  }
}
