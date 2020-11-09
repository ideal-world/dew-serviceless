package idealworld.dew.baas.reldb.test;

import io.vertx.core.Future;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
@Slf4j
public abstract class BasicTest {

    @Rule
    public RunTestOnContext rule = new RunTestOnContext(new VertxOptions()
            .setBlockedThreadCheckInterval(200000));

    @SneakyThrows
    protected <E> E await(Future<E> future, Async async) {
        while (!future.isComplete()) {
            async.wait(100);
        }
        return future.result();
    }

    protected <E> E await(Future<E> future) {
        while (!future.isComplete()) {
        }
        return future.result();
    }
}
