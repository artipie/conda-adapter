/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.asto;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Storage;
import com.artipie.asto.streams.StorageValuePipeline;
import com.artipie.conda.AuthTokens;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Class to clean expired tokens.
 * @since 0.5
 */
public final class AuthTokensMaid {

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Ctor.
     * @param asto Abstract storage
     */
    public AuthTokensMaid(final Storage asto) {
        this.asto = asto;
    }

    /**
     * Cleans expired tokens from storage tokens json {@link AstoAuthTokens#TKNS}.
     * @return Completable action
     */
    public CompletionStage<Void> clean() {
        return this.asto.exists(AstoAuthTokens.TKNS).thenCompose(
            exists -> {
                CompletionStage<Void> res = CompletableFuture.allOf();
                if (exists) {
                    res = new StorageValuePipeline(this.asto, AstoAuthTokens.TKNS).process(
                        (opt, out) ->
                            opt.ifPresent(inputStream -> copyValidTokens(inputStream, out))
                    );
                }
                return res;
            });
    }

    /**
     * Copies valid tokens from input to output streams.
     * @param input Input steam to read tokens from
     * @param out Output to write the result
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    private static void copyValidTokens(final InputStream input, final OutputStream out) {
        final JsonFactory fact = new JsonFactory();
        try (
            JsonGenerator gen = fact.createGenerator(out);
            JsonParser parser = fact.createParser(input)
        ) {
            gen.writeStartObject();
            gen.writeFieldName(AstoAuthTokens.TOKENS);
            gen.writeStartObject();
            JsonToken jtoken;
            while ((jtoken = parser.nextToken()) != null) {
                if (jtoken == JsonToken.FIELD_NAME
                    && !parser.getCurrentName().equals(AstoAuthTokens.TOKENS)) {
                    final String token = parser.getCurrentName();
                    parser.nextToken();
                    parser.setCodec(new ObjectMapper());
                    final ObjectNode node = parser.<ObjectNode>readValueAsTree();
                    final AuthTokens.TokenItem item = new AuthTokens.TokenItem(
                        token, node
                    );
                    if (!item.expired()) {
                        gen.writeFieldName(token);
                        gen.setCodec(new ObjectMapper());
                        gen.writeTree(node);
                    }
                }
            }
            gen.writeEndObject();
            gen.writeEndObject();
        } catch (final IOException err) {
            throw new ArtipieIOException(err);
        }
    }
}
