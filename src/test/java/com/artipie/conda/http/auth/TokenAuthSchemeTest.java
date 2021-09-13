/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http.auth;

import com.artipie.http.Headers;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.TokenAuthentication;
import com.artipie.http.headers.Authorization;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link TokenAuthScheme}.
 * @since 0.5
 */
class TokenAuthSchemeTest {

    /**
     * Test token.
     */
    private static final String TKN = "abc123";

    @Test
    void canAuthorizeByHeader() {
        MatcherAssert.assertThat(
            new TokenAuthScheme(new TestTokenAuth()).authenticate(
                new Headers.From(new Authorization.Token(TokenAuthSchemeTest.TKN)),
                "GET /not/used HTTP/1.1"
            ).toCompletableFuture().join().user().isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    void canAuthorizeByRqLine() {
        MatcherAssert.assertThat(
            new TokenAuthScheme(new TestTokenAuth()).authenticate(
                Headers.EMPTY,
                String.format("GET /t/%s/my-repo/repodata.json HTTP/1.1", TokenAuthSchemeTest.TKN)
            ).toCompletableFuture().join().user().isPresent(),
            new IsEqual<>(true)
        );
    }

    @Test
    void doesNotAuthorizeIfTokenIsNotPresent() {
        MatcherAssert.assertThat(
            new TokenAuthScheme(new TestTokenAuth()).authenticate(
                Headers.EMPTY,
                "GET /any HTTP/1.1"
            ).toCompletableFuture().join().user().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void doesNotAuthorizeByWrongTokenInHeader() {
        MatcherAssert.assertThat(
            new TokenAuthScheme(new TestTokenAuth()).authenticate(
                new Headers.From(new Authorization.Token("098xyz")),
                "GET /ignored HTTP/1.1"
            ).toCompletableFuture().join().user().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void doesNotAuthorizeByWrongTokenInRqLine() {
        MatcherAssert.assertThat(
            new TokenAuthScheme(new TestTokenAuth()).authenticate(
                Headers.EMPTY,
                "GET /t/any/my-conda/repodata.json HTTP/1.1"
            ).toCompletableFuture().join().user().isPresent(),
            new IsEqual<>(false)
        );
    }

    /**
     * Test token auth.
     * @since 0.5
     */
    private static final class TestTokenAuth implements TokenAuthentication {

        @Override
        public CompletionStage<Optional<Authentication.User>> user(final String token) {
            Optional<Authentication.User> res = Optional.empty();
            if (token.equals(TokenAuthSchemeTest.TKN)) {
                res = Optional.of(new Authentication.User("Alice"));
            }
            return CompletableFuture.completedFuture(res);
        }
    }

}
