/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http.auth;

import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.TokenAuthentication;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Simple in memory implementation of {@link TokenAuthentication}.
 * @since 0.5
 */
public final class TokenAuth implements TokenAuthentication {

    /**
     * Tokens.
     */
    private final Map<String, Authentication.User> tokens;

    /**
     * Ctor.
     * @param tokens Tokens and users
     */
    public TokenAuth(final Map<String, Authentication.User> tokens) {
        this.tokens = tokens;
    }

    @Override
    public CompletionStage<Optional<Authentication.User>> user(final String token) {
        return CompletableFuture.completedFuture(Optional.ofNullable(this.tokens.get(token)));
    }
}
