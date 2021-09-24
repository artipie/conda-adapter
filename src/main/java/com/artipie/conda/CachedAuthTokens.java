/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda;

import com.artipie.asto.misc.UncheckedScalar;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

/**
 * Cached authentication tokens.
 * @since 0.5
 */
public final class CachedAuthTokens implements AuthTokens {

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
            CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).softValues().build(),
            origin
        );
    }

    @Override
    public CompletionStage<Optional<TokenItem>> get(final String token) {
        return this.checkAndCompute(
            Optional.ofNullable(this.cache.getIfPresent(token)),
            () -> this.origin.get(token)
        );
    }

    @Override
    public CompletionStage<Optional<TokenItem>> find(final String username) {
        return this.checkAndCompute(
            this.cache.asMap().values().stream()
                .filter(item -> item.userName().equals(username)).findFirst(),
            () -> this.origin.find(username)
        );
    }

    @Override
    public CompletionStage<TokenItem> generate(final String name, final Duration ttl) {
        return this.origin.generate(name, ttl).thenApply(
            tkn -> {
                this.cache.put(tkn.token(), tkn);
                return tkn;
            }
        );
    }

    /**
     * Checks if
     *  a) the token from cache is absent, calls provided compute and ads value to cache
     *  b) present and not expired, returns value from cache
     * and returns the result.
     * @param item Token item from cache, empty is not found
     * @param compute Action to compute if token is not present in cache
     * @return Completable action with the result
     */
    @SuppressWarnings("PMD.ConfusingTernary")
    private CompletionStage<Optional<TokenItem>> checkAndCompute(
        final Optional<TokenItem> item,
        final Callable<CompletionStage<Optional<TokenItem>>> compute
    ) {
        CompletionStage<Optional<TokenItem>> res =
            CompletableFuture.completedFuture(Optional.empty());
        if (!item.isPresent()) {
            res = new UncheckedScalar<>(compute::call).value().thenApply(
                tkn -> {
                    tkn.ifPresent(present -> this.cache.put(present.token(), present));
                    return tkn;
                }
            );
        } else if (!item.get().expired()) {
            res = CompletableFuture.completedFuture(item);
        }
        return res;
    }
}
