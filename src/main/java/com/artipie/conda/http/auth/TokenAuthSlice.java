/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http.auth;

import com.artipie.conda.AuthTokens;
import com.artipie.http.Slice;
import com.artipie.http.auth.AuthSlice;
import com.artipie.http.auth.Permission;

/**
 * Token authentication slice.
 * @since 0.5
 */
public final class TokenAuthSlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param origin Origin slice
     * @param perm Permissions
     * @param tokens Token authentication
     */
    public TokenAuthSlice(
        final Slice origin, final Permission perm, final AuthTokens tokens
    ) {
        super(new AuthSlice(origin, new TokenAuthScheme(new TokenAuth(tokens)), perm));
    }
}
