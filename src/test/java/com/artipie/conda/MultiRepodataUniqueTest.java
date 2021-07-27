/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda;

import com.artipie.asto.test.TestResource;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.cactoos.list.ListOf;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link MultiRepodata.Unique}.
 * @since 0.3
 */
class MultiRepodataUniqueTest {

    @Test
    void mergesPackages() throws UnsupportedEncodingException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        new MultiRepodata.Unique().merge(
            new ListOf<InputStream>(
                new TestResource("MultiRepodataUniqueTest/mergesPackages_input1.json")
                    .asInputStream(),
                new TestResource("MultiRepodataUniqueTest/mergesPackages_input2.json")
                    .asInputStream()
            ),
            res
        );
        JSONAssert.assertEquals(
            res.toString(StandardCharsets.UTF_8.name()),
            new String(
                new TestResource("MultiRepodataUniqueTest/mergesPackages_res.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            true
        );
    }
}
