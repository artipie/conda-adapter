/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link CachedAuthTokens}.
 * @since 0.5
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CachedAuthTokensTest {

    @Test
    void getsFromCache() {
        final Cache<String, AuthTokens.TokenItem> cache =
            CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).softValues().build();
        final AuthTokens.TokenItem item = new AuthTokens.TokenItem("000", "Zero", Instant.MAX);
        cache.put(item.token(), item);
        MatcherAssert.assertThat(
            new CachedAuthTokens(cache, new FakeAuthTokens()).get(item.token())
                .toCompletableFuture().join().get(),
            new IsEqual<>(item)
        );
    }

    @Test
    void findsFromCache() {
        final Cache<String, AuthTokens.TokenItem> cache =
            CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).softValues().build();
        final AuthTokens.TokenItem item = new AuthTokens.TokenItem("abc", "Alice", Instant.MAX);
        cache.put(item.token(), item);
        MatcherAssert.assertThat(
            new CachedAuthTokens(cache, new FakeAuthTokens()).find(item.userName())
                .toCompletableFuture().join().get(),
            new IsEqual<>(item)
        );
    }

    @Test
    void addsToCache() {
        final Cache<String, AuthTokens.TokenItem> cache =
            CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).softValues().build();
        new CachedAuthTokens(cache, new FakeAuthTokens())
            .generate("Janette", Duration.ofDays(1)).toCompletableFuture().join();
        MatcherAssert.assertThat(
            cache.size(),
            new IsEqual<>(1L)
        );
    }

    /**
     * Fake implementation of {@link AuthTokens}.
     * @since 0.5
     */
    static class FakeAuthTokens implements AuthTokens {

        @Override
        public CompletionStage<Optional<TokenItem>> get(final String token) {
            return null;
        }

        @Override
        public CompletionStage<Optional<TokenItem>> find(final String username) {
            return null;
        }

        @Override
        public CompletionStage<TokenItem> generate(final String name, final Duration ttl) {
            return CompletableFuture.completedFuture(
                new TokenItem("abc123", "Janette", Instant.now().plus(ttl))
            );
        }
    }

}
