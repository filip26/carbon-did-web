package com.apicatalog.did.web;

import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.apicatalog.did.DidDocument;

public record DidWebMetadata(
        DidWeb did,
        /// date
        Instant updated,
        /// expires
        Instant refresh) implements DidDocument.Metadata {

    public static DidWebMetadata of(DidWeb did, HttpResponse<InputStream> response) {

        Instant updated = toInstant(response.headers(), "date");
        Instant refresh = toInstant(response.headers(), "expires");

        return new DidWebMetadata(did, updated, refresh);
    }

    private static Instant toInstant(HttpHeaders headers, String name) {
        var value = headers.firstValue(name);
        if (value.isPresent()) {
            return OffsetDateTime
                    .parse(value.get(), DateTimeFormatter.RFC_1123_DATE_TIME)
                    .toInstant();
        }
        return null;
    }

}
