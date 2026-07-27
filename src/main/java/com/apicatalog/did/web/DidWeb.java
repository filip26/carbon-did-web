package com.apicatalog.did.web;

import java.net.URI;
import java.util.Objects;

import com.apicatalog.did.Did;
import com.apicatalog.did.DidUrl;

public record DidWeb(
        URI url,
        String methodSpecificId) {

    /** DID method name for {@code did:web}. */
    public static final String METHOD_NAME = "web";

    /**
     * Tests whether the given {@link Did} is a {@code did:web}.
     *
     * @param did the DID to test
     * @return {@code true} if the DID uses the {@code did:web} method
     */
    public static boolean isDidWeb(Did di) {
        return di != null && METHOD_NAME.equals(di.method());
    }

    /**
     * Tests whether the given {@link DidUrl} contains a {@code did:web}.
     *
     * @param url the DID URL to test
     * @return {@code true} if the DID uses the {@code did:web} method
     */
    public static boolean isDidWeb(DidUrl url) {
        return url != null && METHOD_NAME.equals(url.method());
    }

    @Override
    public final String toString() {
        return Did.SCHEME + ":" + METHOD_NAME + ":" + methodSpecificId;
    }

    public static DidWeb parse(final String did) {

        if (!did.startsWith(Did.SCHEME + ":" + METHOD_NAME + ":")) {
            throw new IllegalArgumentException();
        }

        var methodSpecificId = did.substring(Did.SCHEME.length() + METHOD_NAME.length() + 2);

        if (!Did.isValidMethodSpecificId(methodSpecificId)) {
            throw new IllegalArgumentException();
        }

        return from(methodSpecificId);
    }

    /**
     * Creates a new {@link DidWeb} instance from the given {@link Did}.
     *
     * @param did the {@link Did} to interpret as a {@code did:web}
     * @return a new {@link DidWeb} instance
     *
     * @throws IllegalArgumentException if the given {@link Did} is not a valid
     *                                  {@code did:web}
     */
    public static DidWeb from(final Did did) {
        Objects.requireNonNull(did);

        if (!METHOD_NAME.equalsIgnoreCase(did.method())) {
            throw new IllegalArgumentException(
                    "Not a did:web DID; unsupported method '" + did.method() + "'. DID [" + did + "].");
        }

        return from(did.methodSpecificId());
    }

    public static DidWeb from(final String methodSpecificId) {

        var parts = methodSpecificId.split(":", 2);

        var domain = parts[0];
        var path = parts.length == 2
                ? parts[1].replaceAll(":", "/")
                : ".well-known";

        return new DidWeb(
                URI.create("https://" + Did.decode(domain) + "/" + Did.decode(path) + "/did.json"),
                methodSpecificId);
    }
}
