/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link CachedAuthTokens}.
 * @since 0.5
 * @checkstyle MagicNumberCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class CachedAuthTokensTest {

    /**
     * Test cache.
     */
    private Cache<String, AuthTokens.TokenItem> cache;

    @BeforeEach
    void init() {
        this.cache = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES)
            .softValues().build();
    }

    @Test
    void getsFromCache() {
        final AuthTokens.TokenItem item = new AuthTokens.TokenItem("000", "Zero", Instant.MAX);
        this.cache.put(item.token(), item);
        MatcherAssert.assertThat(
            new CachedAuthTokens(this.cache, new FakeAuthTokens()).get(item.token())
                .toCompletableFuture().join().get(),
            new IsEqual<>(item)
        );
    }

    @Test
    void getsFromOriginAndAddsToCache() {
        final AuthTokens.TokenItem item = new AuthTokens.TokenItem("12", "OneTwo", Instant.MAX);
        MatcherAssert.assertThat(
            "Did not get token from origin",
            new CachedAuthTokens(this.cache, new FakeAuthTokens(item)).get(item.token())
                .toCompletableFuture().join().get(),
            new IsEqual<>(item)
        );
        MatcherAssert.assertThat(
            "Did not add token item to cache",
            this.cache.getIfPresent(item.token()),
            new IsEqual<>(item)
        );
    }

    @Test
    void returnsEmptyIfTokenIsExpired() {
        final AuthTokens.TokenItem item = new AuthTokens.TokenItem("03", "ZeroThree", Instant.MIN);
        this.cache.put(item.token(), item);
        MatcherAssert.assertThat(
            new CachedAuthTokens(this.cache, new FakeAuthTokens()).get(item.token())
                .toCompletableFuture().join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void findsFromCache() {
        final AuthTokens.TokenItem item = new AuthTokens.TokenItem("abc", "Alice", Instant.MAX);
        this.cache.put(item.token(), item);
        MatcherAssert.assertThat(
            new CachedAuthTokens(this.cache, new FakeAuthTokens()).find(item.userName())
                .toCompletableFuture().join().get(),
            new IsEqual<>(item)
        );
    }

    @Test
    void findsFromOriginAndAddsToCache() {
        final AuthTokens.TokenItem item = new AuthTokens.TokenItem("09", "ZeroNine", Instant.MAX);
        MatcherAssert.assertThat(
            "Did not get token item from origin",
            new CachedAuthTokens(this.cache, new FakeAuthTokens(item)).find(item.userName())
                .toCompletableFuture().join().get(),
            new IsEqual<>(item)
        );
        MatcherAssert.assertThat(
            "Did not add token item to cache",
            this.cache.getIfPresent(item.token()),
            new IsEqual<>(item)
        );
    }

    @Test
    void returnsEmptyIfTokenIsExpiredByUsername() {
        final AuthTokens.TokenItem item = new AuthTokens.TokenItem("04", "ZeroFour", Instant.MIN);
        this.cache.put(item.token(), item);
        MatcherAssert.assertThat(
            new CachedAuthTokens(this.cache, new FakeAuthTokens()).find(item.userName())
                .toCompletableFuture().join().isPresent(),
            new IsEqual<>(false)
        );
    }

    @Test
    void addsToCache() {
        final AuthTokens.TokenItem item = new CachedAuthTokens(this.cache, new FakeAuthTokens())
            .generate("Janette", Duration.ofDays(1)).toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.cache.getIfPresent(item.token()),
            new IsEqual<>(item)
        );
    }

    /**
     * Fake implementation of {@link AuthTokens}.
     * @since 0.5
     * @checkstyle JavadocVariableCheck (500 lines)
     */
    static class FakeAuthTokens implements AuthTokens {

        private final List<TokenItem> tokens;

        FakeAuthTokens(final List<TokenItem> tokens) {
            this.tokens = tokens;
        }

        FakeAuthTokens() {
            this(new ArrayList<>(1));
        }

        FakeAuthTokens(final TokenItem tkn) {
            this(new ListOf<>(tkn));
        }

        @Override
        public CompletionStage<Optional<TokenItem>> get(final String token) {
            if (this.tokens.isEmpty()) {
                throw new IllegalStateException();
            }
            return CompletableFuture.completedFuture(
                this.tokens.stream().filter(item -> item.token().equals(token)).findFirst()
            );
        }

        @Override
        public CompletionStage<Optional<TokenItem>> find(final String username) {
            if (this.tokens.isEmpty()) {
                throw new IllegalStateException();
            }
            return CompletableFuture.completedFuture(
                this.tokens.stream().filter(item -> item.userName().equals(username)).findFirst()
            );
        }

        @Override
        public CompletionStage<TokenItem> generate(final String name, final Duration ttl) {
            final TokenItem value = new TokenItem("abc123", "Janette", Instant.now().plus(ttl));
            this.tokens.add(value);
            return CompletableFuture.completedFuture(value);
        }
    }

}
