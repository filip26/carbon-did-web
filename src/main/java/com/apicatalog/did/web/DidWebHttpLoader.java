package com.apicatalog.did.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;

import com.apicatalog.did.DidDocument;
import com.apicatalog.did.DidDocument.WithMetadata;
import com.apicatalog.did.web.DidWebResolver.DocumentAdapter;

public class DidWebHttpLoader implements DidWebResolver.Loader {

    @FunctionalInterface
    public interface DocumentParser {
        Map<String, Object> parseDocument(InputStream is) throws IOException;
    }

    private final DocumentParser parser;
    private final DocumentAdapter mapAdapter;
    private final HttpClient httpClient;

    private Duration timeout;

    public DidWebHttpLoader(
            DocumentParser parser,
            DocumentAdapter adapter,
            HttpClient httpClient) {
        this.parser = parser;
        this.mapAdapter = adapter;
        this.httpClient = httpClient;
        this.timeout = Duration.ofSeconds(1);
    }

    @Override
    public WithMetadata loadDocument(DidWeb did) {

        var request = HttpRequest.newBuilder(did.url()).GET().timeout(timeout).build();

        try {
            var response = httpClient.send(request, BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IllegalArgumentException();
            }

            try (var is = response.body()) {

                var map = parser.parseDocument(is);

                var doc = mapAdapter.readDocument(did.toDid(), map);

                return new WithMetadata(
                        ResponseMetadata.from(response), 
                        doc);
            }

        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public void timeout(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration timeout() {
        return timeout;
    }

    private static record ResponseMetadata(

    ) implements DidDocument.Metadata {

        static ResponseMetadata from(HttpResponse<InputStream> response) {
            // TODO write some metadata
            return new ResponseMetadata();
        }

    }

}
