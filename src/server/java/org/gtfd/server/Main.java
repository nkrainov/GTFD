package org.gtfd.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        Path base = Path.of("").toAbsolutePath();

        Path docJson = base.resolve("doc/api-docs.json");
        if (!Files.exists(docJson)) {
            System.err.println("doc.json not found in " + base);
            System.exit(1);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/doc", exchange -> {
            byte[] body = Files.readAllBytes(docJson);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/", exchange -> {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String uriPath = exchange.getRequestURI().getPath();
            if (uriPath.endsWith("/")) uriPath += "index.html";

            Path file = base.resolve(uriPath.substring(1)).normalize();
            if (!file.startsWith(base)) {
                exchange.sendResponseHeaders(403, -1);
                return;
            }

            if (!Files.exists(file) || Files.isDirectory(file)) {
                byte[] body = "404 Not Found".getBytes();
                exchange.sendResponseHeaders(404, body.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
                return;
            }

            byte[] body = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", detectContentType(file));
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.start();
        System.out.printf("Serving at http://localhost:%d%n", port);
        System.out.printf("API doc: http://localhost:%d/api/doc%n", port);
        System.out.println("Press Ctrl+C to stop.");
    }

    private static String detectContentType(Path file) {
        String name = file.getFileName().toString();
        if (name.endsWith(".html") || name.endsWith(".htm")) return "text/html; charset=utf-8";
        if (name.endsWith(".js"))   return "application/javascript; charset=utf-8";
        if (name.endsWith(".css"))  return "text/css; charset=utf-8";
        if (name.endsWith(".json")) return "application/json; charset=utf-8";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".svg"))  return "image/svg+xml";
        if (name.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }
}