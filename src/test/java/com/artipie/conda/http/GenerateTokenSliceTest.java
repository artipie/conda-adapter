/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.http.Headers;
import com.artipie.http.auth.Authentication;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.util.HashMap;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GenerateTokenSlice}.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class GenerateTokenSliceTest {

    @Test
    void addsToken() {
        final Map<String, String> tokens = new HashMap<>();
        final String name = "Alice";
        final String pswd = "wonderland";
        MatcherAssert.assertThat(
            "Slice response in not 200 OK",
            new GenerateTokenSlice(new Authentication.Single(name, pswd), tokens),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.OK),
                new RequestLine(RqMethod.POST, "/authentications"),
                new Headers.From(new Authorization.Basic(name, pswd)),
                Content.EMPTY
            )
        );
        MatcherAssert.assertThat(
            "Token map size is not 1",
            tokens.entrySet(),
            Matchers.hasSize(1)
        );
        MatcherAssert.assertThat(
            "Token map does not have token for user",
            tokens.containsValue(name),
            new IsEqual<>(true)
        );
    }

    @Test
    void returnsUnauthorized() {
        MatcherAssert.assertThat(
            new GenerateTokenSlice(
                new Authentication.Single("Any", "123"),
                new HashMap<>()
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.UNAUTHORIZED),
                new RequestLine(RqMethod.POST, "/any/line")
            )
        );
    }

}
