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
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import com.jcabi.log.Logger;
import io.vertx.reactivex.core.Vertx;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
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

    @BeforeEach
    void initialize() throws Exception {
        this.storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            CondaSliceITCase.VERTX,
            new LoggingSlice(new CondaSlice(this.storage))
        );
        final int port = this.server.start();
        Testcontainers.exposeHostPorts(port);
        final Path setting = this.tmp.resolve(".condarc");
        Files.write(
            setting,
            String.format(
                "channels:\n  - http://host.testcontainers.internal:%d/", port
            ).getBytes()
        );
        this.cntn = new GenericContainer<>("continuumio/miniconda3:4.8.2")
            .withCommand("tail", "-f", "/dev/null")
            .withWorkingDirectory("/home/")
            .withFileSystemBind(this.tmp.toString(), "/home");
        this.cntn.start();
        this.cntn.execInContainer("mv", "/home/.condarc", "/opt/conda/");
    }

    @Test
    void canInstall() throws Exception {
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
        return res.getStdout();
    }
}
