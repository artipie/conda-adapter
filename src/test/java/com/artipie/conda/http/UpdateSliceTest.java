/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.ext.PublisherAs;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.http.Headers;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link UpdateSlice}.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class UpdateSliceTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @ParameterizedTest
    @CsvSource({
        "anaconda-navigator-1.8.4-py35_0.tar.bz2,addsPackageToEmptyRepo-1.json",
        "7zip-19.00-h59b6b97_2.conda,addsPackageToEmptyRepo-2.json"
    })
    void addsPackageToEmptyRepo(final String name, final String result) throws JSONException {
        final Key key = new Key.From("linux-64", name);
        MatcherAssert.assertThat(
            "Slice returned 201 CREATED",
            new UpdateSlice(this.asto),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.POST, String.format("/%s", key.string())),
                Headers.EMPTY,
                new Content.From(new TestResource(name).asBytes())
            )
        );
        MatcherAssert.assertThat(
            "Package was saved to storage",
            this.asto.exists(key).join(),
            new IsEqual<>(true)
        );
        JSONAssert.assertEquals(
            new PublisherAs(this.asto.value(new Key.From("linux-64", "repodata.json")).join())
                .asciiString().toCompletableFuture().join(),
            new String(
                new TestResource(String.format("UpdateSliceTest/%s", result)).asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    @ParameterizedTest
    @CsvSource({
        "anaconda-navigator-1.8.4-py35_0.tar.bz2,addsPackageToEmptyRepo-2.json",
        "7zip-19.00-h59b6b97_2.conda,addsPackageToEmptyRepo-1.json"
    })
    void addsPackageToRepo(final String name, final String index) throws JSONException {
        final Key arch = new Key.From("linux-64");
        final Key key = new Key.From(arch, name);
        this.asto.save(
            new Key.From(arch, "repodata.json"),
            new Content.From(new TestResource(String.format("UpdateSliceTest/%s", index)).asBytes())
        ).join();
        MatcherAssert.assertThat(
            "Slice returned 201 CREATED",
            new UpdateSlice(this.asto),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.POST, String.format("/%s", key.string())),
                Headers.EMPTY,
                new Content.From(new TestResource(name).asBytes())
            )
        );
        MatcherAssert.assertThat(
            "Package was saved to storage",
            this.asto.exists(key).join(),
            new IsEqual<>(true)
        );
        JSONAssert.assertEquals(
            new PublisherAs(this.asto.value(new Key.From("linux-64", "repodata.json")).join())
                .asciiString().toCompletableFuture().join(),
            new String(
                new TestResource("UpdateSliceTest/addsPackageToRepo.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }

    @Test
    void returnsBadRequestIfRequestLineIsIncorrect() {
        MatcherAssert.assertThat(
            new UpdateSlice(this.asto),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, "/any")
            )
        );
    }

    @Test
    void returnsBadRequestIfPackageAlreadyExists() {
        final String key = "linux-64/test.conda";
        this.asto.save(new Key.From(key), Content.EMPTY).join();
        MatcherAssert.assertThat(
            new UpdateSlice(this.asto),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.PUT, String.format("/%s", key))
            )
        );
    }

}
