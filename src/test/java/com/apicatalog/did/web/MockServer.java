package com.apicatalog.did.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MockServer implements AutoCloseable {

    String testBase;
    String resourceBase;

    final int port;

    final ExecutorService pool = Executors.newSingleThreadExecutor();

    final Map<String, Stub> stubs = new ConcurrentHashMap<>();

    ServerSocket server;

    Duration listen;

    volatile boolean running = true;

    volatile Thread thread;

    private record Stub(
            String acceptHeader,
            int statusCode,
            List<Entry<String, String>> responseHeaders,
            String resource) {
    }

    public MockServer() {
        this(0);
    }

    public MockServer(int port) {
        this.port = port;
    }

    private void serve() {
        while (running) {
            try (Socket socket = server.accept()) {
                handle(socket);
            } catch (IOException ignored) {
            }
        }
    }

    private void handle(Socket socket) throws IOException {

        final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        final var out = socket.getOutputStream();

        final var line = in.readLine();

        if (line == null || !line.startsWith("GET")) {
            return;
        }

        final var path = line.split(" ")[1];

        final var stub = stubs.get(path);

        if (listen != null) {
            try {
                thread = Thread.currentThread();

                Thread.sleep(listen.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            thread = null;
            listen = null;
        }

        if (stub == null) {
            var notFoundResponse = """
                                   HTTP/1.1 404 Not Found\r
                                   \r
                                   no stub for %s
                                   """.formatted(path);
            out.write(notFoundResponse.getBytes(StandardCharsets.UTF_8));
            out.flush();
            return;
        }

        // read headers
        var headers = new LinkedHashMap<String, String>();
        boolean nextHeader = false;

        do {
            var headerLine = in.readLine();

            nextHeader = headerLine != null && !headerLine.isBlank() && !headerLine.equals("\n")
                    && !headerLine.equals("\r");
            if (nextHeader) {
                var entry = headerLine.split(": ");
                headers.put(entry[0].toLowerCase(), entry[1].strip());
            }

        } while (nextHeader);

        var accept = headers.get("accept");

        if (stub.acceptHeader != null && !stub.acceptHeader.equals(accept)) {
            var notAcceptable = """
                                HTTP/1.1 406 Not Acceptable\r
                                \r
                                not acceptable media type %s for %s
                                """.formatted(accept, path);
            out.write(notAcceptable.getBytes(StandardCharsets.UTF_8));
            out.flush();
            return;
        }

        var headersString = stub.responseHeaders().stream()
                .map(e -> String.format("%s: %s", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\r\n"));

        out.write("""
                  HTTP/1.1 %d OK\r
                  %s\r
                  \r
                  """.formatted(stub.statusCode(), headersString)
                .getBytes(StandardCharsets.UTF_8));

        if (stub.resource != null) {
            try (var is = MockServer.class.getResourceAsStream(stub.resource)) {
                out.write(is.readAllBytes());
            }
        }
        out.flush();
    }

    public void start() throws IOException {
        server = new ServerSocket(port);
        pool.submit(this::serve);
    }

    @Override
    public void close() throws IOException {
        running = false;
        server.close();
        pool.shutdownNow();
    }

    public String baseUrl() {
        return "http://%s:%d".formatted(
                server.getInetAddress().getHostAddress(),
                server.getLocalPort());
    }

    public void setup(String path, String resource) throws IOException {
        stubs.put(path, new Stub(null, 200, List.of(Map.entry("Content-Type", "application/json")), resource));
    }

    public void listen(Duration duration) {
        this.listen = duration;
    }

    public void hangup() {
        if (thread != null) {
            thread.interrupt();
        }
        listen = null;
    }
}