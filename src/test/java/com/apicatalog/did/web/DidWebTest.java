package com.apicatalog.did.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DidWebTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource({ "vectors" })
    void testParse(String did, String url) {

        var didWeb = DidWeb.parse(did);
        assertEquals(url, didWeb.url().toString());
        assertEquals(did, didWeb.toString());
    }

    static Stream<Arguments> vectors() {
        return Stream.of(
                Arguments.of("did:web:w3c-ccg.github.io", "https://w3c-ccg.github.io/.well-known/did.json"),
                Arguments.of("did:web:w3c-ccg.github.io:user:alice", "https://w3c-ccg.github.io/user/alice/did.json"),
                Arguments.of("did:web:example.com%3A3000:user:alice", "https://example.com:3000/user/alice/did.json"),
                Arguments.of("did:web:example.com%3A3000:us%3Aer:alice",
                        "https://example.com:3000/us:er/alice/did.json"));
    }

}
