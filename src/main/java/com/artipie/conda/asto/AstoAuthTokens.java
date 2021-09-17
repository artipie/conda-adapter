/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.asto;

import com.artipie.asto.ArtipieIOException;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.streams.ContentAsStream;
import com.artipie.conda.AuthTokens;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Asto implementation of {@link AuthTokens}: adds/updates tokens in storage. Tokens
 * are stored in storage by key `.tokens.json` in json format:
 * {
 *   "tokens": {
 *     "abc123": {
 *       "name": "alice",
 *       "expire": 1505739175210
 *     },
 *     "xyz098": {
 *       "name": "John",
 *       "expire": 1516376429792
 *     }
 *   }
 * }
 * @since 0.5
 * @checkstyle ConstantUsageCheck (20 lines)
 */
@SuppressWarnings({"PMD.UnusedPrivateField", "PMD.SingularField"})
public final class AstoAuthTokens implements AuthTokens {

    /**
     * Tokens storage item key.
     */
    static final Key TKNS = new Key.From(".tokens.json");

    /**
     * Abstract storage.
     */
    private final Storage asto;

    /**
     * Time to live.
     */
    private final String ttl;

    /**
     * Ctor.
     * @param asto Abstract storage
     * @param ttl Time to live
     */
    public AstoAuthTokens(final Storage asto, final String ttl) {
        this.asto = asto;
        this.ttl = ttl;
    }

    @Override
    public CompletionStage<Optional<TokenItem>> get(final String token) {
        return this.asto.exists(AstoAuthTokens.TKNS).thenCompose(
            exists -> {
                CompletionStage<Optional<TokenItem>> res =
                    CompletableFuture.completedFuture(Optional.empty());
                if (exists) {
                    res = this.asto.value(AstoAuthTokens.TKNS).thenCompose(
                        pub -> new ContentAsStream<Optional<TokenItem>>(pub).process(
                            input -> AstoAuthTokens.find(input, token)
                        )
                    );
                }
                return res;
            }
        );
    }

    @Override
    public CompletionStage<Optional<TokenItem>> find(final String token) {
        return null;
    }

    @Override
    public CompletionStage<String> generate(final String name) {
        return null;
    }

    /**
     * Find token item to by token string.
     * @param input Input stream with json file
     * @param token Token to search for
     * @return Token item if found
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    private static Optional<TokenItem> find(final InputStream input, final String token) {
        try {
            final JsonParser parser = new JsonFactory().createParser(input);
            JsonToken jtoken;
            Optional<TokenItem> result = Optional.empty();
            while ((jtoken = parser.nextToken()) != null) {
                if (jtoken == JsonToken.FIELD_NAME && parser.getCurrentName().equals(token)) {
                    parser.nextToken();
                    parser.setCodec(new ObjectMapper());
                    final TokenItem item = new TokenItem(
                        token, parser.<ObjectNode>readValueAsTree()
                    );
                    if (!item.expired()) {
                        result = Optional.of(item);
                    }
                }
            }
            return result;
        } catch (final IOException err) {
            throw new ArtipieIOException(err);
        }
    }
}
