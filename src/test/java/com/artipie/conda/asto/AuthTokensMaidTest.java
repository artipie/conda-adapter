/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import java.nio.charset.StandardCharsets;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsEmptyCollection;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * Test for {@link AuthTokensMaid}.
 * @since 0.5
 */
class AuthTokensMaidTest {

    /**
     * Test storage.
     */
    private Storage asto;

    @BeforeEach
    void init() {
        this.asto = new InMemoryStorage();
    }

    @Test
    void removesExpiredTokens() throws JSONException {
        new TestResource("AuthTokensMaidTest/tokens.json").saveTo(this.asto, AstoAuthTokens.TKNS);
        new AuthTokensMaid(this.asto).clean().toCompletableFuture().join();
        JSONAssert.assertEquals(
            new String(
                new BlockingStorage(this.asto).value(AstoAuthTokens.TKNS), StandardCharsets.UTF_8
            ),
            String.join(
                "\n",
                "{",
                "  \"tokens\": {",
                "    \"abc123\": {",
                "      \"name\": \"alice\",",
                "      \"expire\": 4108568400000",
                "    }",
                "  }",
                "}"
            ),
            true
        );
    }

    @Test
    void doesNothingIfTokensDoNotExists() {
        new AuthTokensMaid(this.asto).clean().toCompletableFuture().join();
        MatcherAssert.assertThat(
            this.asto.list(Key.ROOT).join(),
            new IsEmptyCollection<>()
        );
    }
}
