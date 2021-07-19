/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda;

import com.artipie.asto.ArtipieIOException;
import com.artipie.conda.meta.JsonMaid;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Conda repository repodata.
 * @since 0.1
 */
public interface CondaRepodata {

    /**
     * Removes records about conda packages from repodata file.
     * Output/Input streams are not closed by this implementation, these operation should
     * be done from outside.
     * @since 0.1
     */
    final class Remove {

        /**
         * Json repodata input stream.
         */
        private final InputStream input;

        /**
         * Json repodata output, where write the result.
         */
        private final OutputStream out;

        /**
         * Ctor.
         * @param input Json repodata input stream
         * @param out Json repodata output
         */
        public Remove(final InputStream input, final OutputStream out) {
            this.input = input;
            this.out = out;
        }

        /**
         * Removes items from repodata json.
         * @param checksums List of the checksums of the packages to remove.
         * @throws ArtipieIOException On IO errors
         */
        public void perform(final Set<String> checksums) {
            final JsonFactory factory = new JsonFactory();
            try {
                new JsonMaid.Jackson(
                    factory.createGenerator(this.out), factory.createParser(this.input)
                ).clean(checksums);
            } catch (final IOException err) {
                throw new ArtipieIOException(err);
            }
        }
    }

    /**
     * Appends records about conda packages from repodata file.
     * Output/Input streams are not closed by this implementation, these operations should
     * be done from outside.
     * @since 0.2
     */
    final class Append {

        /**
         * Json repodata input stream.
         */
        private final Optional<InputStream> input;

        /**
         * Json repodata output, where write the result.
         */
        private final OutputStream out;

        /**
         * Ctor.
         * @param input Json repodata input stream
         * @param out Json repodata output
         */
        public Append(final Optional<InputStream> input, final OutputStream out) {
            this.input = input;
            this.out = out;
        }

        /**
         * Ctor.
         * @param input Json repodata input stream
         * @param out Json repodata output
         */
        public Append(final InputStream input, final OutputStream out) {
            this(Optional.of(input), out);
        }

        /**
         * Ctor.
         * @param out Json repodata output
         */
        public Append(final OutputStream out) {
            this(Optional.empty(), out);
        }

        /**
         * Parses provided packages and appends metadata to the the provided `packages.json`.
         * @param packages Packages to add
         */
        public void perform(final List<PackageItem> packages) {
            throw new NotImplementedException("Not implemented yet");
        }

    }

    /**
     * Package item: .conda or tar.bz2 package as input stream, file name and checksums.
     * @since 0.2
     * @checkstyle ParameterNameCheck (100 lines)
     */
    final class PackageItem {

        /**
         * Package input stream.
         */
        private final InputStream input;

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
        public PackageItem(final InputStream input, final String filename, final String sha256,
            final String md5) {
            this.input = input;
            this.filename = filename;
            this.sha256 = sha256;
            this.md5 = md5;
        }
    }

}
