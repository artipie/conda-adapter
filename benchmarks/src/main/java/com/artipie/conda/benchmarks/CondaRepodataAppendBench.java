/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.benchmarks;

import com.artipie.asto.misc.UncheckedIOFunc;
import com.artipie.conda.CondaRepodata;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.codec.digest.DigestUtils;
import org.cactoos.scalar.Unchecked;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Benchmark for {@link CondaRepodata.Append}.
 * @since 0.2
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 5)
@Measurement(iterations = 20)
public class CondaRepodataAppendBench {

    /**
     * Benchmark directory.
     */
    private static final String BENCH_DIR = System.getenv("BENCH_DIR");

    /**
     * Benchmark repodata.json.
     */
    private byte[] repodata;

    /**
     * New packages to append.
     */
    private List<TestPackage> pckg;

    @Setup
    public void setup() throws IOException {
        if (CondaRepodataAppendBench.BENCH_DIR == null) {
            throw new IllegalStateException("BENCH_DIR environment variable must be set");
        }
        try (Stream<Path> stream = Files.list(Paths.get(CondaRepodataAppendBench.BENCH_DIR))) {
            final List<Path> files = stream.collect(Collectors.toList());
            this.repodata = files.stream()
                .filter(item -> item.toString().endsWith("repodata.json")).findFirst()
                .map(file -> new Unchecked<>(() -> Files.readAllBytes(file)).value())
                .orElseThrow(() -> new IllegalStateException("Benchmark data not found"));
            this.pckg = files.stream().filter(
                item -> item.toString().endsWith(".tar.bz2") || item.toString().endsWith(".conda")
            ).map(
                new UncheckedIOFunc<>(
                    item -> {
                        final byte[] bytes = Files.readAllBytes(item);
                        return new TestPackage(
                            bytes,
                            item.getFileName().toString(),
                            DigestUtils.sha256Hex(bytes),
                            DigestUtils.md5Hex(bytes)
                        );
                    }
                )
            ).collect(Collectors.toList());
        }
    }

    @Benchmark
    public void run(final Blackhole bhl) {
        new CondaRepodata.Append(
            new ByteArrayInputStream(this.repodata), new ByteArrayOutputStream()
        ).perform(
            this.pckg.stream().map(
                item -> new CondaRepodata.PackageItem(
                    new ByteArrayInputStream(item.input), item.filename, item.sha256, item.md5
                )
            ).collect(Collectors.toList())
        );
    }

    /**
     * Main.
     * @param args CLI args
     * @throws RunnerException On benchmark failure
     */
    public static void main(final String... args) throws RunnerException {
        new Runner(
            new OptionsBuilder()
                .include(CondaRepodataAppendBench.class.getSimpleName())
                .forks(1)
                .build()
        ).run();
    }

    /**
     * Package item: .conda or tar.bz2 package as bytes, file name and checksums.
     * @since 0.2
     * @checkstyle ParameterNameCheck (100 lines)
     */
    private static final class TestPackage {

        /**
         * Package bytes.
         */
        private final byte[] input;

        /**
         * Name of the file.
         */
        private final String filename;

        /**
         * Sha256 sum of the package.
         * @checkstyle MemberNameCheck (5 lines)
         */
        private final String sha256;

        /**
         * Md5 sum of the package.
         * @checkstyle MemberNameCheck (5 lines)
         */
        private final String md5;

        /**
         * Ctor.
         * @param input Package input stream
         * @param filename Name of the file
         * @param sha256 Sha256 sum of the package
         * @param md5 Md5 sum of the package
         * @checkstyle ParameterNumberCheck (5 lines)
         * @checkstyle ParameterNameCheck (5 lines)
         */
        public TestPackage(final byte[] input, final String filename, final String sha256,
                           final String md5) {
            this.input = input;
            this.filename = filename;
            this.sha256 = sha256;
            this.md5 = md5;
        }
    }
}
