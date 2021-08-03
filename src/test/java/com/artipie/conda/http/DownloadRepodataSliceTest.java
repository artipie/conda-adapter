/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.hm.RsHasBody;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link DownloadRepodataSlice}.
 * @since 0.4
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
                new RsHasBody(bytes),
                new RequestLine(RqMethod.GET, "/linux-64/repodata.json")
            )
        );
    }

    @Test
    void returnsEmptyJsonIfNotExists() {
        MatcherAssert.assertThat(
            new DownloadRepodataSlice(this.asto),
            new SliceHasResponse(
                new RsHasBody("{}".getBytes()),
                new RequestLine(RqMethod.GET, "/noarch/current_repodata.json")
            )
        );
    }
}
