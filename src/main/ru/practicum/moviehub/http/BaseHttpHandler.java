package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8"; // !!! Укажите содержимое заголовка Content-Type

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
            ex.getResponseHeaders().set("Content-Type", CT_JSON);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

            ex.sendResponseHeaders(status, bytes.length);

            try (OutputStream stream = ex.getResponseBody()) {
                stream.write(bytes);
            }
    }

    protected void sendNoContent(HttpExchange ex, int status) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, -1);
    }
}