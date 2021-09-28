/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.conda.AuthTokens;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthScheme;
import com.artipie.http.headers.WwwAuthenticate;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithHeaders;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.common.RsJson;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.json.Json;
import org.reactivestreams.Publisher;

/**
 * Slice for token authorization.
 * @since 0.4
 * @todo #32:30min Implement this slice properly to return authorization token for user. It serves
 *  on `POST /authentications`. For more details check swagger api page:
 *  https://api.anaconda.org/docs#!/authentication/post_authentications
 */
final class GenerateTokenSlice implements Slice {

    /**
     * Authentication.
     */
    private final Authentication auth;

    /**
     * Tokens.
     */
    private final AuthTokens tokens;

    /**
     * Tokens time to live.
     */
    private final Duration ttl;

    /**
     * Ctor.
     * @param auth Authentication
     * @param tokens Tokens
     * @param ttl Tokens time to live
     */
    GenerateTokenSlice(final Authentication auth, final AuthTokens tokens, final Duration ttl) {
        this.auth = auth;
        this.tokens = tokens;
        this.ttl = ttl;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            new BasicAuthScheme(this.auth).authenticate(headers).thenCompose(
                usr -> {
                    final CompletionStage<Response> res;
                    if (usr.user().isPresent()) {
                        res = this.tokens.generate(usr.user().get().name(), this.ttl).thenApply(
                            tkn -> new RsJson(
                                () -> Json.createObjectBuilder().add("token", tkn.token()).build(),
                                StandardCharsets.UTF_8
                            )
                        );
                    } else {
                        res = CompletableFuture.completedFuture(
                            new RsWithHeaders(
                                new RsWithStatus(RsStatus.UNAUTHORIZED),
                                new Headers.From(new WwwAuthenticate(usr.challenge()))
                            )
                        );
                    }
                    return res;
                }
            )
        );
    }
}
