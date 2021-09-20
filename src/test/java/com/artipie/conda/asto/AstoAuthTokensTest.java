/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.asto;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.conda.AuthTokens;
import java.time.Instant;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link AstoAuthTokens}.
 * @since 0.5
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class AstoAuthTokensTest {

    /**
     * Test resource path.
     */
    private static final String TOKENS_JSON = "AstoAuthTokensTest/tokens.json";

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void returnsEmptyIfTokensDoNotExist() {
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto, "1 day").get("000").toCompletableFuture()
                .join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsEmptyByUsernameIfTokensDoNotExist() {
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto, "1 day").find("Any").toCompletableFuture()
                .join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsTokenWhenFound() {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        final String token = "abc123";
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto, "1 year").get(token).toCompletableFuture()
                .join().get(),
            new IsEqual<>(
                // @checkstyle MagicNumberCheck (1 line)
                new AuthTokens.TokenItem(token, "alice", Instant.ofEpochMilli(4_108_568_400_000L))
            )
        );
    }

    @Test
    void returnsTokenByUsernameWhenFound() {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        final String name = "alice";
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto, "1 year").find(name).toCompletableFuture()
                .join().get(),
            new IsEqual<>(
                // @checkstyle MagicNumberCheck (1 line)
                new AuthTokens.TokenItem("abc123", name, Instant.ofEpochMilli(4_108_568_400_000L))
            )
        );
    }

    @Test
    void returnsEmptyWhenExpired() {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto, "1 month").get("xyz098").toCompletableFuture()
                .join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void returnsEmptyByUsernameWhenExpired() {
        new TestResource(AstoAuthTokensTest.TOKENS_JSON).saveTo(this.asto, AstoAuthTokens.TKNS);
        MatcherAssert.assertThat(
            new AstoAuthTokens(this.asto, "1 month").find("John").toCompletableFuture()
                .join().isPresent(),
            new IsEqual<>(false)
        );
    }
}
