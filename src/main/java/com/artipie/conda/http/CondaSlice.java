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
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rt.ByMethodsRule;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceDownload;
import com.artipie.http.slice.SliceSimple;

/**
 * Main conda entry point.
 * @since 0.4
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
@SuppressWarnings({"PMD.AvoidDuplicateLiterals", "PMD.ExcessiveMethodLength"})
public final class CondaSlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param storage Storage
     * @param url Application url
     */
    public CondaSlice(final Storage storage, final String url) {
        this(storage, Permissions.FREE, Authentication.ANONYMOUS, url);
    }

    /**
     * Ctor.
     * @param storage Storage
     * @param perms Permissions
     * @param users Users
     * @param url Application url
     * @checkstyle ParameterNumberCheck (5 lines)
     */
    public CondaSlice(final Storage storage, final Permissions perms, final Authentication users,
        final String url) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*repodata\\.json$"),
                        new ByMethodsRule(RqMethod.GET)
                    ),
                    new BasicAuthSlice(
                        new DownloadRepodataSlice(storage), users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*(\\.tar\\.bz2|\\.conda)$"),
                        new ByMethodsRule(RqMethod.GET)
                    ),
                    new BasicAuthSlice(
                        new SliceDownload(storage), users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath("/(stage|commit).*(\\.tar\\.bz2|\\.conda)$"),
                        new ByMethodsRule(RqMethod.POST)
                    ),
                    new BasicAuthSlice(
                        new PostStageCommitSlice(url), users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*(\\.tar\\.bz2|\\.conda)$"),
                        new ByMethodsRule(RqMethod.POST)
                    ),
                    new BasicAuthSlice(
                        new UpdateSlice(storage), users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*(package|release).*"), new ByMethodsRule(RqMethod.GET)
                    ),
                    new BasicAuthSlice(
                        new GetPackageSlice(), users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*(package|release).*"), new ByMethodsRule(RqMethod.POST)
                    ),
                    new BasicAuthSlice(
                        new PostPackageReleaseSlice(), users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(new ByMethodsRule(RqMethod.HEAD), new SliceSimple(StandardRs.OK)),
                new RtRulePath(
                    new RtRule.All(new RtRule.ByPath("/user"), new ByMethodsRule(RqMethod.GET)),
                    new BasicAuthSlice(
                        new GetUserSlice(), users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*authentication-type$"),
                        new ByMethodsRule(RqMethod.GET)
                    ),
                    new BasicAuthSlice(
                        new AuthTypeSlice(), users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*authentications$"), new ByMethodsRule(RqMethod.POST)
                    ),
                    new BasicAuthSlice(
                        new AuthTokenSlice(), users,
                        new Permission.ByName(perms, Action.Standard.WRITE)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new RtRule.ByPath(".*authentications$"), new ByMethodsRule(RqMethod.DELETE)
                    ),
                    new BasicAuthSlice(
                        new SliceSimple(new RsWithStatus(RsStatus.CREATED)), users,
                        new Permission.ByName(perms, Action.Standard.WRITE)
                    )
                ),
                new RtRulePath(RtRule.FALLBACK, new SliceSimple(StandardRs.NOT_FOUND))
            )
        );
    }
}
