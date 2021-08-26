/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.misc.UncheckedIOScalar;
import com.artipie.conda.asto.AstoMergedJson;
import com.artipie.conda.meta.InfoIndex;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.json.Json;
import javax.json.JsonObject;
import org.apache.commons.codec.digest.DigestUtils;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.reactivestreams.Publisher;

/**
 * Slice to update the repository.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class UpdateSlice implements Slice {

    /**
     * Regex to obtain uploaded package architecture and name from request line.
     */
    private static final Pattern PKG = Pattern.compile(".*/((.*)/.*(\\.tar\\.bz2|\\.conda))$");

    /**
     * Temporary upload key.
     */
    private static final Key TMP = new Key.From("./upload");

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Abstract storage
     */
    public UpdateSlice(final Storage asto) {
        this.asto = asto;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body) {
        final Matcher matcher = UpdateSlice.PKG.matcher(new RequestLineFrom(line).uri().getPath());
        final Response res;
        if (matcher.matches()) {
            final Key temp = new Key.From(UpdateSlice.TMP, matcher.group(1));
            res = new AsyncResponse(
                this.asto.exists(new Key.From(matcher.group(1))).thenCompose(
                    main -> this.asto.exists(temp).thenApply(upl -> main || upl)
                ).thenCompose(
                    exists -> {
                        final CompletionStage<Response> resp;
                        if (exists) {
                            resp = CompletableFuture.completedFuture(
                                new RsWithStatus(RsStatus.BAD_REQUEST)
                            );
                        } else {
                            resp = this.asto.save(temp, new Content.From(body)).thenCompose(
                                empty -> this.asto.value(temp).thenCompose(
                                    val -> new PublisherAs(val).bytes().thenApply(
                                        bytes -> UpdateSlice.obtainInfoJson(matcher.group(1), bytes)
                                    )
                                ).thenCompose(
                                    json -> new AstoMergedJson(
                                        this.asto, new Key.From(matcher.group(2), "repodata.json")
                                    ).merge(
                                        new MapOf<String, JsonObject>(
                                            new MapEntry<>(matcher.group(1), json)
                                        )
                                    )
                                ).thenCompose(
                                    ignored -> this.asto.move(temp, new Key.From(matcher.group(1)))
                                ).thenApply(
                                    ignored -> new RsWithStatus(RsStatus.CREATED)
                                )
                            );
                        }
                        return resp;
                    }
                )
            );
        } else {
            res = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return res;
    }

    /**
     * Get info index json from uploaded package bytes.
     * @param name Package name
     * @param bytes Package bytes
     * @return JsonObject with package info
     */
    private static JsonObject obtainInfoJson(final String name, final byte[] bytes) {
        final InfoIndex info;
        final InputStream inp = new ByteArrayInputStream(bytes);
        if (name.endsWith("conda")) {
            info = new InfoIndex.Conda(inp);
        } else {
            info = new InfoIndex.TarBz(inp);
        }
        return Json.createObjectBuilder(new UncheckedIOScalar<>(info::json).value())
            .add("md5", DigestUtils.md5Hex(bytes))
            .add("sha256", DigestUtils.sha256Hex(bytes)).build();
    }
}
