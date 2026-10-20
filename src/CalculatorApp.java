import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class CalculatorApp {

    public static void main(String[] args) throws IOException {
        // Start built-in HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Map routes to handlers
        server.createContext("/", new PageHandler());
        server.createContext("/style.css", new CssHandler());
        server.createContext("/calculate", new CalculateHandler());
        server.setExecutor(null);

        System.out.println("Calculator app running!");
        System.out.println("Open your browser and visit: http://localhost:8080");
        server.start();
    }

    // Serves the initial index.html page
    static class PageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = Files.readString(new File("index.html").toPath());

            // Clean up placeholder tags for initial load
            html = html.replace("{{NUM1}}", "")
                    .replace("{{NUM2}}", "")
                    .replace("{{RESULT}}", "--")
                    .replace("{{SEL_ADD}}", "")
                    .replace("{{SEL_SUB}}", "")
                    .replace("{{SEL_MUL}}", "")
                    .replace("{{SEL_DIV}}", "");

            sendResponse(exchange, html, "text/html");
        }
    }

    // Serves the CSS file
    static class CssHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File cssFile = new File("style.css");
            if (cssFile.exists()) {
                String css = Files.readString(cssFile.toPath());
                sendResponse(exchange, css, "text/css");
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        }
    }

    // Processes calculations from form submission
    static class CalculateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, String> params = parseForm(body);

                double n1 = Double.parseDouble(params.getOrDefault("num1", "0"));
                double n2 = Double.parseDouble(params.getOrDefault("num2", "0"));
                String op = params.getOrDefault("operation", "add");

                String resultText;
                switch (op) {
                    case "add":
                        resultText = String.valueOf(n1 + n2);
                        break;
                    case "subtract":
                        resultText = String.valueOf(n1 - n2);
                        break;
                    case "multiply":
                        resultText = String.valueOf(n1 * n2);
                        break;
                    case "divide":
                        resultText = (n2 == 0) ? "Cannot divide by 0" : String.valueOf(n1 / n2);
                        break;
                    default:
                        resultText = "Invalid Operation";
                }

                // Inject dynamic calculation results into the HTML template
                String html = Files.readString(new File("index.html").toPath());
                html = html.replace("{{NUM1}}", String.valueOf(n1))
                        .replace("{{NUM2}}", String.valueOf(n2))
                        .replace("{{RESULT}}", resultText)
                        .replace("{{SEL_ADD}}", op.equals("add") ? "selected" : "")
                        .replace("{{SEL_SUB}}", op.equals("subtract") ? "selected" : "")
                        .replace("{{SEL_MUL}}", op.equals("multiply") ? "selected" : "")
                        .replace("{{SEL_DIV}}", op.equals("divide") ? "selected" : "");

                sendResponse(exchange, html, "text/html");
            }
        }
    }

    // Helper: Parse form URL-encoded body
    private static Map<String, String> parseForm(String formData) {
        Map<String, String> map = new HashMap<>();
        for (String pair : formData.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    // Helper: Send HTTP response
    private static void sendResponse(HttpExchange exchange, String content, String contentType) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
