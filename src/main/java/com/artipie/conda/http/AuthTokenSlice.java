/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.StandardRs;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * Slice for token authorization.
 * @since 0.4
 * @todo #32:30min Implement this slice properly to return authorization token for user. It serves
 *  on `POST /authentications`. For more details check swagger api page:
 *  https://api.anaconda.org/docs#!/authentication/post_authentications
 */
final class AuthTokenSlice implements Slice {

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new RsWithBody(
            StandardRs.OK,
            "{\"token\": \"ol-8e23b18a-1d14-43fc-a66d-61b211fad8c7\"}".getBytes()
        );
    }
}
