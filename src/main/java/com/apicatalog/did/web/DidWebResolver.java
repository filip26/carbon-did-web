package com.apicatalog.did.web;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import com.apicatalog.did.Did;
import com.apicatalog.did.DidDocument;
import com.apicatalog.did.DidDocument.Relationship;
import com.apicatalog.did.DidUrl;
import com.apicatalog.did.VerificationMethod;

/**
 * {@link DidResolver} implementation for the {@code did:web} method.
 *
 */
public class DidWebResolver implements
        VerificationMethod.Resolver,
        VerificationMethod.Dereferencer,
        DidDocument.Resolver {

    @FunctionalInterface
    public interface DocumentAdapter {
        DidDocument readDocument(Did did, Map<String, Object> document);
    }

    @FunctionalInterface
    public interface Loader {
        DidDocument.WithMetadata loadDocument(DidWeb did);
    }

    @FunctionalInterface
    public interface MethodAdapter {
        VerificationMethod readMethod(Collection<String> context, Map<String, Object> method);
    }

    public static final String DEFAULT_CONTEXT = "https://www.w3.org/ns/did/v1.1";

    private final Loader loader;

    public DidWebResolver(
            Loader loader) {
        this.loader = loader;
    }

    @Override
    public DidDocument.WithMetadata resolve(DidUrl url, Map<String, Object> options) {

        if (!DidWeb.isDidWeb(url)) {
            throw new IllegalArgumentException();
        }

        if (url.query() != null || url.path() != null) {
            throw new IllegalArgumentException();
        }

        var didWeb = DidWeb.from(url.methodSpecificId());

        return loader.loadDocument(didWeb);
    }

    @Override
    public Optional<VerificationMethod> resolveMethod(DidUrl url, Relationship rel, Map<String, Object> options) {

        if (url.fragment() == null || url.fragment().isBlank()) {
            throw new IllegalArgumentException();
        }

        var document = resolve(url, options);

        if (document.metadata() != null) {
            if (document.metadata().isDeactivated()) {
                return Optional.empty();
            }
        }

        return findMethod(document.document(), url, rel);
    }

    @Override
    public Optional<VerificationMethod> findMethod(DidDocument document, DidUrl url, Relationship rel) {

        if (url.fragment() == null || url.fragment().isBlank()) {
            throw new IllegalArgumentException();
        }

        var methods = document.methods(rel);

        if (methods == null || methods.isEmpty()) {
            return Optional.empty();
        }

        for (var method : methods) {
            if (method.id().equals(url)) {
                return Optional.of(method);
            }
        }

        return Optional.empty();
    }
}
