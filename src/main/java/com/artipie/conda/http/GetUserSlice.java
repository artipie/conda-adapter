/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.common.RsJson;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.json.Json;
import org.cactoos.io.ReaderOf;
import org.reactivestreams.Publisher;

/**
 * Slice to handle `GET /user` request.
 * @since 0.4
 * @todo #32:30min Implement this slice properly, check swagger docs for more details
 *  https://api.anaconda.org/docs#!/user/get_user
 *  Now it returns string stab.
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
final class GetUserSlice implements Slice {

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        return new RsJson(
            () -> Json.createReader(
                new ReaderOf(
                    String.join(
                        "\n",
                        "{",
                        "  \"company\": \"asas\",",
                        "  \"created_at\": \"2020-08-01 13:06:29.212000+00:00\",",
                        "  \"description\": \"asas\",",
                        "  \"location\": \"asas\",",
                        "  \"login\": \"any\",",
                        "  \"name\": \"asas\",",
                        "  \"url\": \"\",",
                        "  \"user_type\": \"user\"",
                        "}"
                    )
                )
            ).read(),
            StandardCharsets.UTF_8
        );
    }
}
