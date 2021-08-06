/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/conda-adapter/LICENSE
 */
package com.artipie.conda.http;

import com.artipie.asto.Storage;
import com.artipie.http.Slice;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rs.common.RsJson;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import org.cactoos.io.ReaderOf;

/**
 * Main conda entry point.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class CondaSlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param storage Storage
     */
    public CondaSlice(final Storage storage) {
        this(storage, Permissions.FREE, Authentication.ANONYMOUS);
    }

    /**
     * Ctor.
     * @param storage Storage
     * @param perms Permissions
     * @param users Users
     */
    public CondaSlice(final Storage storage, final Permissions perms, final Authentication users) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*repodata\\.json$"),
                        new ByMethodsRule(RqMethod.GET)
                    ),
                    new BasicAuthSlice(
                        new DownloadRepodataSlice(storage),
                        users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*(\\.tar\\.bz2|\\.conda)$"),
                        new ByMethodsRule(RqMethod.GET)
                    ),
                    new BasicAuthSlice(
                        new SliceDownload(storage),
                        users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*authentication-type$"),
                        new ByMethodsRule(RqMethod.GET)
                    ),
                    new BasicAuthSlice(
                        new SliceSimple(
                            new RsJson(
                                () -> Json.createReader(
                                    new ReaderOf("{\"authentication_type\": \"password\"}")
                                ).read(),
                                StandardCharsets.UTF_8
                            )
                        ),
                        users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*authentications$"),
                        new ByMethodsRule(RqMethod.POST)
                    ),
                    new BasicAuthSlice(
                        new AuthTokenSlice(),
                        users,
                        new Permission.ByName(perms, Action.Standard.WRITE)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.HEAD)
                    ),
                    new SliceSimple(StandardRs.OK)
                )
            )
        );
    }
}
