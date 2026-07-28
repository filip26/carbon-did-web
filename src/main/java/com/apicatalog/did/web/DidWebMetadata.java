package com.apicatalog.did.web;

import java.net.URI;
import java.time.Instant;

import com.apicatalog.did.DidDocument;

public record DidWebMetadata(
        URI url,
        /// last modified
        Instant updated,
        /// Expires / Cache-Control
        Instant refresh
        ) implements DidDocument.Metadata {

}
