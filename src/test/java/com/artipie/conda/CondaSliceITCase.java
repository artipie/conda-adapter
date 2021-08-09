/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.conda.http.CondaSlice;
import com.artipie.http.misc.RandomFreePort;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.StringContains;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

/**
 * Conda adapter integration test.
 * @since 0.3
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class CondaSliceITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Application port.
     */
    private int port;

    @BeforeEach
    void initialize() throws Exception {
        this.storage = new InMemoryStorage();
        this.port = new RandomFreePort().get();
        final String url = String.format("http://host.testcontainers.internal:%d", this.port);
        this.server = new VertxSliceServer(
            CondaSliceITCase.VERTX,
            new LoggingSlice(new CondaSlice(this.storage, url)),
            this.port
        );
        this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        Files.write(
            this.tmp.resolve(".condarc"),
            String.format("channels:\n  - %s\nanaconda_upload: True", url).getBytes()
        );
        FileUtils.copyDirectory(
            new TestResource("example-project").asPath().toFile(),
            this.tmp.toFile()
        );
        this.cntn = new GenericContainer<>("continuumio/miniconda3:4.10.3")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
    }

    @Test
    void anacondaCanLogin() throws Exception {
        this.exec("conda", "install", "-y", "anaconda-client");
        this.exec(
            "anaconda", "config", "--set", "url",
            String.format("http://host.testcontainers.internal:%d/", this.port),
            "-s"
        );
        MatcherAssert.assertThat(
            this.exec("anaconda", "login", "--username", "any", "--password", "any"),
            new StringContainsInOrder(
                new ListOf<>(
                    "Using Anaconda API: http://host.testcontainers.internal",
                    "any's login successful"
                )
            )
        );
    }

    @Test
    void canPublishWithCondaBuild() throws Exception {
        this.exec("conda", "install", "-y", "conda-build");
        this.exec("conda", "install", "-y", "conda-verify");
        this.exec("conda", "install", "-y", "anaconda-client");
        this.moveCondarc();
        this.exec(
            "anaconda", "config", "--set", "url",
            String.format("http://host.testcontainers.internal:%d/", this.port),
            "-s"
        );
        this.exec("conda", "config", "--set", "anaconda_upload", "yes");
        MatcherAssert.assertThat(
            "Login was not successful",
            this.exec("anaconda", "login", "--username", "any", "--password", "any"),
            new StringContains("any's login successful")
        );
        MatcherAssert.assertThat(
            "Package was not installed successfully",
            this.exec("conda", "build", "--output-folder", "./conda-out/", "./conda/"),
            new StringContainsInOrder(
                new ListOf<String>(
                    "Creating package \"example-package\"", "Creating release \"0.0.1\"",
                    // @checkstyle LineLengthCheck (1 line)
                    "Uploading file \"any/example-package/0.0.1/linux-64/example-package-0.0.1-0.tar.bz2\"",
                    "Upload complete", "conda package located at:"
                )
            )
        );
        MatcherAssert.assertThat(
            "Package not found in storage",
            this.storage.exists(new Key.From("linux-64/example-package-0.0.1-0.tar.bz2")).join(),
            new IsEqual<>(true)
        );
    }

    @Test
    void canInstall() throws Exception {
        this.moveCondarc();
        new TestResource("CondaSliceITCase/packages.json")
            .saveTo(this.storage, new Key.From("linux-64/repodata.json"));
        new TestResource("CondaSliceITCase/snappy-1.1.3-0.tar.bz2")
            .saveTo(this.storage, new Key.From("linux-64/snappy-1.1.3-0.tar.bz2"));
        MatcherAssert.assertThat(
            exec("conda", "install", "--verbose", "-y", "snappy"),
            new StringContainsInOrder(
                new ListOf<String>(
                    "The following packages will be downloaded:",
                    "http://host.testcontainers.internal",
                    "linux-64::snappy-1.1.3-0",
                    "Preparing transaction: ...working... done",
                    "Verifying transaction: ...working... done",
                    "Executing transaction: ...working... done"
                )
            )
        );
    }

    @AfterEach
    void stop() {
        this.server.stop();
        this.cntn.stop();
    }

    private String exec(final String... command) throws Exception {
        final Container.ExecResult res = this.cntn.execInContainer(command);
        Logger.debug(this, "Command:\n%s\nResult:\n%s", String.join(" ", command), res.toString());
        return res.toString();
    }

    private void moveCondarc() throws IOException, InterruptedException {
        this.cntn.execInContainer("mv", "/home/.condarc", "/root/");
        this.cntn.execInContainer("rm", "/home/.condarc");
    }
}
