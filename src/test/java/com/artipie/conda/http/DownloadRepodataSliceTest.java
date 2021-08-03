/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.headers.ContentDisposition;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.RsHasHeaders;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DownloadRepodataSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
class DownloadRepodataSliceTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void returnsItemFromStorageIfExists() {
        final byte[] bytes = "data".getBytes();
        this.asto.save(
            new Key.From("linux-64/repodata.json"), new Content.From(bytes)
        ).join();
        MatcherAssert.assertThat(
            new DownloadRepodataSlice(this.asto),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(bytes),
                    new RsHasHeaders(
                        new ContentDisposition("attachment; filename=\"repodata.json\""),
                        new ContentLength(bytes.length)
                    )
                ),
                new RequestLine(RqMethod.GET, "/linux-64/repodata.json")
            )
        );
    }

    @Test
    void returnsEmptyJsonIfNotExists() {
        final byte[] bytes = "{}".getBytes();
        MatcherAssert.assertThat(
            new DownloadRepodataSlice(this.asto),
            new SliceHasResponse(
                Matchers.allOf(
                    new RsHasBody(bytes),
                    new RsHasHeaders(
                        new ContentDisposition("attachment; filename=\"current_repodata.json\""),
                        new ContentLength(bytes.length)
                    )
                ),
                new RequestLine(RqMethod.GET, "/noarch/current_repodata.json")
            )
        );
    }
}
