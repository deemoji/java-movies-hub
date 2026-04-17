package ru.practicum.moviehub.http;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieMaker;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {

    private final MoviesStore store;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String[] pathElements = exchange.getRequestURI().getPath().split("/");
        boolean hasYear = exchange.getRequestURI().getQuery() != null;
        if (method.equalsIgnoreCase("GET") && pathElements.length > 2) {
            handleGetMovieByIdRequest(exchange, pathElements[2]);
        }

        if (method.equalsIgnoreCase("GET") && hasYear) {
            handleGetMoviesByYearRequest(exchange);
        }

        if (method.equalsIgnoreCase("GET")) {
            handleGetMoviesRequest(exchange);
        }

        if (method.equalsIgnoreCase("POST")) {
            handlePostMoviesRequest(exchange);
        }

        if (method.equalsIgnoreCase("DELETE")) {
            handleDeleteMovieRequest(exchange, pathElements[2]);
        }
    }


    private void handleGetMovieByIdRequest(HttpExchange exchange, String idStr) throws IOException {
        try {
            int id = Integer.parseInt(idStr);
            Optional<Movie> movieOptional = store.getMovie(id);
            if (movieOptional.isPresent()) {
                sendJson(exchange, 200, gson.toJson(movieOptional.get()));
                return;
            }
            sendJson(exchange, 404, gson.toJson(new ErrorResponse("Фильм не найден", new String[]{})));
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, gson.toJson(new ErrorResponse("Невалидный ID", new String[] {})));
        }
    }

    private void handleGetMoviesByYearRequest(HttpExchange exchange) throws IOException {
        try {
            String yearStr = exchange.getRequestURI().getQuery().split("=")[1];
            int year = Integer.parseInt(yearStr);
            Movie[] moviesFiltered = store.getMovies(year);

            sendJson(exchange, 200, gson.toJson(List.of(moviesFiltered)));

        } catch (NumberFormatException e) {
            sendJson(exchange, 400, gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'", new String[] {})));
        }
    }

    private void handleGetMoviesRequest(HttpExchange exchange) throws IOException {
        List<Movie> movies = List.of(store.getMovies());

        String moviesJson = gson.toJson(movies);

        sendJson(exchange, 200, moviesJson);
    }


    private void handlePostMoviesRequest(HttpExchange exchange) throws IOException {
        String requestCt = exchange.getRequestHeaders().get("Content-Type").getFirst();
        if (!requestCt.equals(CT_JSON)) {
            ErrorResponse response = new ErrorResponse(
                    "Неподдерживаемый формат данных в запросе",
                    new String[] {}
            );

            sendJson(exchange, 415, gson.toJson(response));
            return;
        }

        String requestBody = "";
        try (InputStream stream = exchange.getRequestBody()) {
            requestBody = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        JsonElement jsonBody;
        try {
            jsonBody = JsonParser.parseString(requestBody);
        } catch (JsonSyntaxException e) {
            ErrorResponse errorResponse = new ErrorResponse(
                    "Невалидный JSON",
                    new String[] { }
            );
            sendJson(exchange, 400, gson.toJson(errorResponse));
            return;
        }

        if (jsonBody.isJsonObject()) {
            List<String> errorDetails = new ArrayList<>();
            JsonObject jsonObject = jsonBody.getAsJsonObject();
            String title = jsonObject.get("title").getAsString();
            if (title.isBlank()) {
                errorDetails.add("название не должно быть пустым");
            }
            if (title.length() >= 100) {
                errorDetails.add("название должно быть меньше 100 символов");
            }
            int year = jsonObject.get("year").getAsInt();
            int currentYear = LocalDate.now().getYear();
            if (year < 1888 || year > currentYear) {
                errorDetails.add("год должен быть между 1888 и " + currentYear);
            }

            if (errorDetails.isEmpty()) {
                Movie newMovie = MovieMaker.makeMovie(title, year);
                store.addMovie(newMovie);

                sendJson(exchange, 201, gson.toJson(newMovie));
                return;
            }
            ErrorResponse errorResponse = new ErrorResponse(
                    "Ошибка валидации",
                    errorDetails.toArray(new String[0])
            );
            sendJson(exchange, 422, gson.toJson(errorResponse));
            return;
        }
    }

    private void handleDeleteMovieRequest(HttpExchange exchange, String idStr) throws IOException {
        try {
            int id = Integer.parseInt(idStr);
            if (store.deleteMovie(id)) {
                sendNoContent(exchange, 204);
                return;
            }
            sendJson(exchange, 404, gson.toJson(new ErrorResponse("Фильм не найден", new String[]{})));
        } catch (NumberFormatException e) {
            sendJson(exchange, 400, gson.toJson(new ErrorResponse("Невалидный ID", new String[] {})));
        }
    }
}
