/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Cached authentication tokens.
 * @since 0.5
 */
public final class CachedAuthTokens implements AuthTokens {

    /**
     * One hour in millis.
     */
    private static final int HOUR = 60 * 1000;

    /**
     * Tokens cache.
     */
    private final Cache<String, TokenItem> cache;

    /**
     * Origin.
     */
    private final AuthTokens origin;

    /**
     * Ctor.
     * @param cache Tokens cache
     * @param origin Origin AuthTokens
     */
    public CachedAuthTokens(final Cache<String, TokenItem> cache, final AuthTokens origin) {
        this.cache = cache;
        this.origin = origin;
    }

    /**
     * Ctor.
     * @param origin Origin AuthTokens
     */
    public CachedAuthTokens(final AuthTokens origin) {
        this(
            CacheBuilder.newBuilder()
                .expireAfterAccess(CachedAuthTokens.HOUR, TimeUnit.MILLISECONDS)
                .softValues().build(),
            origin
        );
    }

    @Override
    public CompletionStage<Optional<TokenItem>> get(final String token) {
        final TokenItem item = this.cache.getIfPresent(token);
        CompletionStage<Optional<TokenItem>> res = CompletableFuture
            .completedFuture(Optional.empty());
        if (item == null) {
            res = this.origin.get(token).thenApply(
                tkn -> {
                    tkn.ifPresent(present -> this.cache.put(token, present));
                    return tkn;
                }
            );
        } else if (!item.expired()) {
            res = CompletableFuture.completedFuture(Optional.of(item));
        }
        return res;
    }

    @Override
    @SuppressWarnings("PMD.ConfusingTernary")
    public CompletionStage<Optional<TokenItem>> find(final String username) {
        final Optional<TokenItem> token = this.cache.asMap().values().stream()
            .filter(item -> item.userName().equals(username)).findFirst();
        CompletionStage<Optional<TokenItem>> res =
            CompletableFuture.completedFuture(Optional.empty());
        if (!token.isPresent()) {
            res = this.origin.find(username).thenApply(
                tkn -> {
                    tkn.ifPresent(present -> this.cache.put(present.token(), present));
                    return tkn;
                }
            );
        } else if (!token.get().expired()) {
            res = CompletableFuture.completedFuture(token);
        }
        return res;
    }

    @Override
    public CompletionStage<String> generate(final String name, final Duration ttl) {
        throw new NotImplementedException("Not yet implemented");
    }
}
