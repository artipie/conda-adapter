/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

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
import java.util.Map;
import javax.json.Json;
import org.apache.commons.lang3.RandomStringUtils;
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
    private final Map<String, String> tokens;

    /**
     * Ctor.
     * @param auth Authentication
     * @param tokens Tokens
     */
    GenerateTokenSlice(final Authentication auth, final Map<String, String> tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new AsyncResponse(
            new BasicAuthScheme(this.auth).authenticate(headers).thenApply(
                usr -> {
                    final Response res;
                    if (usr.user().isPresent()) {
                        final String token = RandomStringUtils.random(30, true, true);
                        usr.user().map(user -> this.tokens.put(token, user.name()));
                        res = new RsJson(
                            () -> Json.createObjectBuilder().add("token", token).build(),
                            StandardCharsets.UTF_8
                        );
                    } else {
                        res = new RsWithHeaders(
                            new RsWithStatus(RsStatus.UNAUTHORIZED),
                            new Headers.From(new WwwAuthenticate(usr.challenge()))
                        );
                    }
                    return res;
                }
            )
        );
    }
}
