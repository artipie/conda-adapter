/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.conda.AuthTokens;
import com.artipie.http.Headers;
import com.artipie.http.auth.Authentication;
import com.artipie.http.headers.Authorization;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.apache.commons.lang3.NotImplementedException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link GenerateTokenSlice}.
 * @since 0.5
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 * @checkstyle MagicNumberCheck (500 lines)
 */
class GenerateTokenSliceTest {

    /**
     * Test token.
     */
    private static final String TOKEN = "abc123";

    @Test
    void addsToken() {
        final String name = "Alice";
        final String pswd = "wonderland";
        MatcherAssert.assertThat(
            "Slice response in not 200 OK",
            new GenerateTokenSlice(
                new Authentication.Single(name, pswd),
                new FakeAuthTokens(),
                Duration.ofDays(5)
            ),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasStatus(RsStatus.OK),
                    new RsHasBody(
                        String.format("{\"token\":\"%s\"}", GenerateTokenSliceTest.TOKEN).getBytes()
                    )
                ),
                new RequestLine(RqMethod.POST, "/authentications"),
                new Headers.From(new Authorization.Basic(name, pswd)),
                Content.EMPTY
            )
        );
    }

    @Test
    void returnsUnauthorized() {
        MatcherAssert.assertThat(
            new GenerateTokenSlice(
                new Authentication.Single("Any", "123"),
                new FakeAuthTokens(), Duration.ofDays(2)
            ),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.UNAUTHORIZED),
                new RequestLine(RqMethod.POST, "/any/line")
            )
        );
    }

    /**
     * Fake implementation of {@link AuthTokens}.
     * @since 0.5
     */
    static class FakeAuthTokens implements AuthTokens {

        @Override
        public CompletionStage<Optional<TokenItem>> get(final String token) {
            throw new NotImplementedException("Not needed to implement");
        }

        @Override
        public CompletionStage<Optional<TokenItem>> find(final String username) {
            throw new NotImplementedException("Not be implemented");
        }

        @Override
        public CompletionStage<TokenItem> generate(final String name, final Duration ttl) {
            return CompletableFuture.completedFuture(
                new TokenItem(GenerateTokenSliceTest.TOKEN, name, Instant.now().plus(ttl))
            );
        }
    }

}
