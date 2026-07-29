package com.apicatalog.did.web;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.apicatalog.did.DidUrl;
import com.apicatalog.did.adapter.DidDocumentAdapter;
import com.apicatalog.did.adapter.JsonWebKeyAdapter;
import com.apicatalog.did.adapter.MultiKeyAdapter;
import com.apicatalog.did.primitive.JsonWebKey;
import com.apicatalog.did.primitive.MultiKey;
import com.apicatalog.multibase.MultibaseDecoder;
import com.apicatalog.tree.io.Tree;
import com.apicatalog.tree.io.jakcson.Jackson2Parser;
import com.fasterxml.jackson.core.JsonFactory;

class DidWebResolverTest {

    static HttpClient CLIENT = null;
    static MockServer SERVER = null;
    static DidWebResolver.Loader LOADER = null;

    @BeforeAll
    static void startMockServer() throws IOException {
        SERVER = new MockServer();
        SERVER.start();
        CLIENT = HttpClient.newBuilder()
                .followRedirects(Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(1))
                .build();

        LOADER = new DidWebHttpLoader(
                DidWebResolverTest::read,
                DidDocumentAdapter.newBuilder()
                        .context(_ -> true)
                        .method(MultiKey.TYPE_NAME,
                                _ -> true,
                                new MultiKeyAdapter(MultibaseDecoder.getInstance()::decode))
                        .method(JsonWebKey.TYPE_NAME,
                                _ -> true,
                                new JsonWebKeyAdapter())
                        .build()::readDocument,
//                        Map.of(/*TODO service adapters */))::readDocument,
                CLIENT);
    }

    @AfterAll
    static void stopMockServer() throws IOException {
        if (SERVER != null) {
            SERVER.close();
            SERVER = null;
        }
        if (CLIENT != null) {
            CLIENT = null;
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource({ "vectors" })
    void testDocumentResolve(String did, String resource) throws IOException {

        var didWeb = DidWeb.parse(did);
        SERVER.setup(didWeb.url().getPath(), resource);

        var resolver = new DidWebResolver(
                // for test purpose, need to rewrite the host
                _didWeb -> LOADER.loadDocument(
                        new DidWeb(
                                URI.create(SERVER.baseUrl() + _didWeb.url().getPath()),
                                _didWeb.methodSpecificId())));

        var resolved = resolver.resolve(DidUrl.parse(did), Map.of());

        assertNotNull(resolved);
        IO.println(resolved);
    }

    static Map<String, Object> read(InputStream is) throws IOException {
        try (var parser = Jackson2Parser.newParser(is, JsonFactory.builder().build())) {
            return Tree.read(parser);
        }
    }

    static Stream<Arguments> vectors() {
        return Stream.of(
                Arguments.of(
                        "did:web:example.com",
                        "jwk-vector-1.json",
                        Map.of("expires", "Tue, 19 Jan 2038 03:14:07 GMT",
                                "date", "Sun, 06 Nov 1994 08:49:37 GMT")));
    }
}
