/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.meta;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.json.JsonObject;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Merges provided metadata list with existing repodata.json.
 * @since 0.2
 */
public interface MergedJson {

    /**
     * Appends provided metadata to existing repodata.json.
     * @param items Items to add, filename <-> metadata json
     * @throws IOException On IO error
     */
    void merge(Map<String, JsonObject> items) throws IOException;

    /**
     * Implementation of {@link MergedJson} based on {@link com.fasterxml.jackson}.
     * @since 0.2
     */
    final class Jackson implements MergedJson {

        /**
         * Json generator.
         */
        private final JsonGenerator gnrt;

        /**
         * Json parser.
         */
        private final Optional<JsonParser> parser;

        /**
         * Ctor.
         * @param gnrt Json generator
         * @param parser Json parser
         */
        public Jackson(final JsonGenerator gnrt, final Optional<JsonParser> parser) {
            this.gnrt = gnrt;
            this.parser = parser;
        }

        @Override
        public void merge(final Map<String, JsonObject> items) throws IOException {
            if (this.parser.isPresent()) {
                throw new NotImplementedException("To be implemented");
            } else {
                this.gnrt.writeStartObject();
                this.gnrt.writeFieldName("packages");
                this.gnrt.writeStartObject();
                this.writePackages(items, ".tar.bz2");
                this.gnrt.writeEndObject();
                this.gnrt.writeFieldName("packages.conda");
                this.gnrt.writeStartObject();
                this.writePackages(items, ".conda");
                this.gnrt.writeEndObject();
            }
            this.gnrt.close();
        }

        /**
         * Writes packages (.tar.bz2 or .conda) to json generator.
         * @param items Items to write
         * @param type Packages type
         * @throws IOException On IO error
         */
        private void writePackages(final Map<String, JsonObject> items, final String type)
            throws IOException {
            for (final String pckg : items.keySet()) {
                if (pckg.endsWith(type)) {
                    this.gnrt.writeFieldName(pckg);
                    this.gnrt.setCodec(new ObjectMapper());
                    this.gnrt.writeTree(new ObjectMapper().readTree(items.get(pckg).toString()));
                }
            }
        }
    }
}
