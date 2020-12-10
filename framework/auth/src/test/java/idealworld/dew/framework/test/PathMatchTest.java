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

package idealworld.dew.framework.test;

import idealworld.dew.framework.fun.test.DewTest;
import idealworld.dew.framework.util.AntPathMatcher;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PathMatchTest extends DewTest {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private List<String> pathPatterns = new ArrayList<>() {
        {
            add("/app/{name}/**");
            add("/app/**");
            add("/app/{name}/{kind}/enabled");
            add("/app/{name}/{kind}/**");
        }
    };

    private List<String> matchPaths(String pathRequest) {
        var comparator = PATH_MATCHER.getPatternComparator(pathRequest);
        return pathPatterns
                .stream()
                .filter(pathPattern -> PATH_MATCHER.match(pathPattern, pathRequest)
                )
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Test
    public void testMatch(Vertx vertx, VertxTestContext testContext) {
        var matchedPaths = matchPaths("/app/n1/k1/enabled");
        Assertions.assertEquals(4, matchedPaths.size());
        Assertions.assertEquals("/app/{name}/{kind}/enabled", matchedPaths.get(0));

        matchedPaths = matchPaths("/app/n1/k1/disabled");
        Assertions.assertEquals(3, matchedPaths.size());
        Assertions.assertEquals("/app/{name}/{kind}/**", matchedPaths.get(0));
        Assertions.assertEquals("/app/{name}/**", matchedPaths.get(1));
        Assertions.assertEquals("/app/**", matchedPaths.get(2));

        matchedPaths = matchPaths("/app/n1/k1");
        Assertions.assertEquals("/app/{name}/{kind}/**", matchedPaths.get(0));
        Assertions.assertEquals("/app/{name}/**", matchedPaths.get(1));
        Assertions.assertEquals("/app/**", matchedPaths.get(2));

        matchedPaths = matchPaths("/app/n1");
        Assertions.assertEquals(2, matchedPaths.size());
        Assertions.assertEquals("/app/{name}/**", matchedPaths.get(0));
        Assertions.assertEquals("/app/**", matchedPaths.get(1));

        matchedPaths = matchPaths("/app1");
        Assertions.assertEquals(0, matchedPaths.size());

        testContext.completeNow();
    }

}
