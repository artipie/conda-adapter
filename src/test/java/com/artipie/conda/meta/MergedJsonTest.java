/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.meta;

import com.artipie.asto.test.TestResource;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.json.Json;
import javax.json.JsonObject;
import org.cactoos.map.MapEntry;
import org.cactoos.map.MapOf;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link MergedJson.Jackson}.
 * @since 0.2
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class MergedJsonTest {

    @Test
    void addsTarAndCondaPackages() throws IOException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        new MergedJson.Jackson(
            factory.createGenerator(res).useDefaultPrettyPrinter(),
            Optional.empty()
        ).merge(
            new MapOf<String, JsonObject>(
                this.packageItem("decorator-4.2.1-py27_0.tar.bz2", "decorator-tar.json"),
                this.packageItem("notebook-6.1.1-py38_0.conda", "notebook-conda.json"),
                this.packageItem("pyqt-5.6.0-py36h0386399_5.tar.bz2", "pyqt-tar.json"),
                this.packageItem("tenacity-6.2.0-py37_0.conda", "tenacity-conda.json")
            )
        );
        JSONAssert.assertEquals(
            new String(
                new TestResource("MergedJsonTest/addsTarAndCondaPackages.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void addsTarPackages() throws IOException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        new MergedJson.Jackson(
            factory.createGenerator(res).useDefaultPrettyPrinter(),
            Optional.empty()
        ).merge(
            new MapOf<String, JsonObject>(
                this.packageItem("decorator-4.2.1-py27_0.tar.bz2", "decorator-tar.json"),
                this.packageItem("pyqt-5.6.0-py36h0386399_5.tar.bz2", "pyqt-tar.json")
            )
        );
        JSONAssert.assertEquals(
            new String(
                new TestResource("MergedJsonTest/addsTarPackages.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    @Test
    void addsCondaPackages() throws IOException, JSONException {
        final ByteArrayOutputStream res = new ByteArrayOutputStream();
        final JsonFactory factory = new JsonFactory();
        new MergedJson.Jackson(
            factory.createGenerator(res).useDefaultPrettyPrinter(),
            Optional.empty()
        ).merge(
            new MapOf<String, JsonObject>(
                this.packageItem("notebook-6.1.1-py38_0.conda", "notebook-conda.json"),
                this.packageItem("tenacity-6.2.0-py37_0.conda", "tenacity-conda.json")
            )
        );
        JSONAssert.assertEquals(
            new String(
                new TestResource("MergedJsonTest/addsCondaPackages.json").asBytes(),
                StandardCharsets.UTF_8
            ),
            res.toString(StandardCharsets.UTF_8.name()),
            true
        );
    }

    private MapEntry<String, JsonObject> packageItem(final String filename, final String resourse) {
        return new MapEntry<String, JsonObject>(
            filename,
            Json.createReader(
                new TestResource(String.format("MergedJsonTest/%s", resourse)).asInputStream()
            ).readObject()
        );
    }

}
