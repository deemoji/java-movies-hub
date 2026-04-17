package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieMaker;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static MoviesStore store;
    private static HttpClient client;
    private static Gson gson;
    private static boolean serverIsCreated = false;

    @BeforeAll
    static void beforeAll() {
        try {
            store = new MoviesStore();
            server = new MoviesServer(store, 8080);
            client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build();
            gson = new Gson();
            serverIsCreated = true;
            server.start();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @BeforeEach
    void beforeEach() {
        store.clear();
    }

    @AfterAll
    static void afterAll() {
        if (serverIsCreated) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpResponse<String> resp =
                makeRequest(BASE + "/movies", RequestType.GET);
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenMultipleElements_returnsElementsSuccessfully() throws Exception {
        String[] titles = { "AAA", "AAБ", "AAВ" };
        int[] years = { 1999, 2001, 2002 };

        List<Movie> expectedMovies = List.of(
                MovieMaker.makeMovie(titles[0], years[0]),
                MovieMaker.makeMovie(titles[1], years[1]),
                MovieMaker.makeMovie(titles[2], years[2])
        );

        expectedMovies.forEach(movie -> store.addMovie(movie));

        HttpResponse<String> resp =
                makeRequest(BASE + "/movies", RequestType.GET);
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String moviesArrayJson = resp.body().trim();

        JsonElement jsonElement = JsonParser.parseString(moviesArrayJson);

        assertTrue(jsonElement.isJsonArray(), "Ожидается JSON-массив");
        List<Movie> movies = gson.fromJson(moviesArrayJson, new ListOfMoviesTypeToken());

        assertEquals(expectedMovies, movies, "Список из ответа сервера должен совпадать с ожидаемым списком");
    }

    @Test
    void postMovies_whenMoviePostSuccess_returnsMovieJson() throws Exception {
        String movieTitle = "AAA";
        int movieYear = 1888;
        String movieJson = "{\"title\": \"" + movieTitle + "\", \"year\": " + movieYear + "}";
        HttpResponse<String> response = makeRequest(BASE + "/movies", RequestType.POST, movieJson);

        assertEquals(201, response.statusCode(), "POST /movies должен вернуть 201");

        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String responseJson = response.body().trim();

        JsonElement jsonElement = JsonParser.parseString(responseJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");

        Movie movieResponse = gson.fromJson(jsonElement, Movie.class);
        Movie expectedMovie = new Movie(Objects.hash(movieTitle, movieYear), movieTitle, movieYear);
        assertEquals(
                expectedMovie,
                movieResponse,
                "Объекты типа Movie должны совпадать"
        );
        assertEquals(
                expectedMovie,
                store.getMovies()[0],
                "Ожидаемый объект должен находиться в хранилище"
        );
    }

    @Test
    void postMovies_whenMovieTitleIsEmpty_returnsError() throws Exception {
        String movieTitle = " ";
        int movieYear = 1888;
        String movieJson = "{\"title\": \"" + movieTitle + "\", \"year\": " + movieYear + "}";
        HttpResponse<String> response = makeRequest(BASE + "/movies", RequestType.POST, movieJson);

        assertEquals(422, response.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertEquals(0, store.getMovies().length, "Хранилище не должно содержать фильмы");

        String responseJson = response.body().trim();
        JsonElement jsonElement = JsonParser.parseString(responseJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");

        ErrorResponse errorResponse = gson.fromJson(jsonElement, ErrorResponse.class);
        ErrorResponse expectedResponse = new ErrorResponse(
                "Ошибка валидации",
                new String[] {"название не должно быть пустым"}
        );

        assertEquals(expectedResponse, errorResponse, "Объекты ErrorResponse должны совпадать");
    }

    @Test
    void postMovies_whenMovieTitleLengthIs100_returnsError() throws Exception {
        String movieTitle = "A".repeat(100);
        int movieYear = 1888;
        String movieJson = "{\"title\": \"" + movieTitle + "\", \"year\": " + movieYear + "}";
        HttpResponse<String> response = makeRequest(BASE + "/movies", RequestType.POST, movieJson);

        assertEquals(422, response.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertEquals(0, store.getMovies().length, "Хранилище не должно содержать фильмы");

        String responseJson = response.body().trim();
        JsonElement jsonElement = JsonParser.parseString(responseJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");

        ErrorResponse errorResponse = gson.fromJson(jsonElement, ErrorResponse.class);
        ErrorResponse expectedResponse = new ErrorResponse(
                "Ошибка валидации",
                new String[] {"название должно быть меньше 100 символов"}
        );
        assertEquals(expectedResponse, errorResponse, "Объекты ErrorResponse должны совпадать");
    }

    @Test
    void postMovies_whenMovieYear1887_returnsError() throws Exception {
        String movieTitle = "AАА";
        int movieYear = 1887;
        String movieJson = "{\"title\": \"" + movieTitle + "\", \"year\": " + movieYear + "}";
        HttpResponse<String> response = makeRequest(BASE + "/movies", RequestType.POST, movieJson);

        assertEquals(422, response.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertEquals(0, store.getMovies().length, "Хранилище не должно содержать фильмы");

        String responseJson = response.body().trim();
        JsonElement jsonElement = JsonParser.parseString(responseJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");

        ErrorResponse errorResponse = gson.fromJson(jsonElement, ErrorResponse.class);
        ErrorResponse expectedResponse = new ErrorResponse(
                "Ошибка валидации",
                new String[] {"год должен быть между 1888 и 2026"}
        );
        assertEquals(expectedResponse, errorResponse, "Объекты ErrorResponse должны совпадать");
    }

    @Test
    void postMovies_whenMovieYearIsBiggerThanCurrent_returnsError() throws Exception {
        String movieTitle = "AАА";
        int movieYear = LocalDate.now().getYear() + 1;
        String movieJson = "{\"title\": \"" + movieTitle + "\", \"year\": " + movieYear + "}";
        HttpResponse<String> response = makeRequest(BASE + "/movies", RequestType.POST, movieJson);

        assertEquals(422, response.statusCode(), "POST /movies должен вернуть 422");

        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertEquals(0, store.getMovies().length, "Хранилище не должно содержать фильмы");

        String responseJson = response.body().trim();
        JsonElement jsonElement = JsonParser.parseString(responseJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");

        ErrorResponse errorResponse = gson.fromJson(jsonElement, ErrorResponse.class);
        ErrorResponse expectedResponse = new ErrorResponse(
                "Ошибка валидации",
                new String[] {"год должен быть между 1888 и " + LocalDate.now().getYear()}
        );
        assertEquals(expectedResponse, errorResponse, "Объекты ErrorResponse должны совпадать");
    }

    @Test
    void postMovies_whenContentTypeIsWrong_returnsError() throws Exception {
        String movieTitle = "AАА";
        int movieYear = LocalDate.now().getYear();
        String movieJson = "{\"title\": \"" + movieTitle + "\", \"year\": " + movieYear + "}";
        HttpResponse<String> response = makeRequest(
                BASE + "/movies",
                RequestType.POST,
                "text/plain",
                movieJson);

        assertEquals(415, response.statusCode(), "POST /movies должен вернуть 415");

        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertEquals(0, store.getMovies().length, "Хранилище не должно содержать фильмы");

        String responseJson = response.body().trim();
        JsonElement jsonElement = JsonParser.parseString(responseJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");

        ErrorResponse errorResponse = gson.fromJson(jsonElement, ErrorResponse.class);
        ErrorResponse expectedResponse = new ErrorResponse(
                "Неподдерживаемый формат данных в запросе",
                new String[] {}
        );
        assertEquals(expectedResponse, errorResponse, "Объекты ErrorResponse должны совпадать");
    }

    @Test
    void postMovies_whenResultJsonIsWrong_returnsError() throws Exception {
        String movieTitle = "AАА";
        int movieYear = LocalDate.now().getYear();
        // Отсутствует закрывающая скобка
        String movieJson = "{\"title\": \"" + movieTitle + "\", \"year\": " + movieYear;
        HttpResponse<String> response = makeRequest(BASE + "/movies", RequestType.POST, movieJson);

        assertEquals(400, response.statusCode(), "POST /movies должен вернуть 400");

        String contentTypeHeaderValue =
                response.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");
        assertEquals(0, store.getMovies().length, "Хранилище не должно содержать фильмы");

        String responseJson = response.body().trim();
        JsonElement jsonElement = JsonParser.parseString(responseJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");

        ErrorResponse errorResponse = gson.fromJson(jsonElement, ErrorResponse.class);
        ErrorResponse expectedResponse = new ErrorResponse(
                "Невалидный JSON",
                new String[] {}
        );
        assertEquals(expectedResponse, errorResponse, "Объекты ErrorResponse должны совпадать");
    }

    @Test
    void getMovieById_whenIdIsCorrect_returnsMovieJson() throws Exception {
        String[] titles = { "AAA", "AAБ", "AAВ" };
        int[] years = { 1999, 2001, 2002 };

        List<Movie> movies = List.of(
                MovieMaker.makeMovie(titles[0], years[0]),
                MovieMaker.makeMovie(titles[1], years[1]),
                MovieMaker.makeMovie(titles[2], years[2])
        );

        movies.forEach(movie -> store.addMovie(movie));
        Movie expectedMovie = movies.get(1);
        HttpResponse<String> resp = makeRequest(BASE + "/movies/" + expectedMovie.getId(), RequestType.GET);
        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies/" + expectedMovie.getId() + " должен вернуть 200"
        );

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String movieJson = resp.body().trim();

        JsonElement jsonElement = JsonParser.parseString(movieJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");
        Movie responseMovie = gson.fromJson(jsonElement, Movie.class);

        assertEquals(expectedMovie, responseMovie, "Ответ сервера и ожидаемый результат должны совпадать");
    }

    @Test
    void getMovieById_whenIdNotFound_returnsError() throws Exception {
        String[] titles = { "AAA", "AAБ", "AAВ" };
        int[] years = { 1999, 2001, 2002 };

        List<Movie> movies = List.of(
                MovieMaker.makeMovie(titles[0], years[0]),
                MovieMaker.makeMovie(titles[1], years[1]),
                MovieMaker.makeMovie(titles[2], years[2])
        );

        movies.forEach(movie -> store.addMovie(movie));
        Movie oddMovie = MovieMaker.makeMovie("ААГ", 2021);
        HttpResponse<String> resp = makeRequest(BASE + "/movies/" + oddMovie.getId(), RequestType.GET);
        assertEquals(
                404,
                resp.statusCode(),
                "GET /movies/" + oddMovie.getId() + " должен вернуть 404"
        );

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String movieJson = resp.body().trim();

        JsonElement jsonElement = JsonParser.parseString(movieJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");
        ErrorResponse responseError = gson.fromJson(jsonElement, ErrorResponse.class);
        assertEquals(
                new ErrorResponse("Фильм не найден", new String[]{}),
                responseError,
                "Ответ сервера и ожидаемый результат должны совпадать"
        );
    }

    @Test
    void getMovieById_whenIdIsNotANumber_returnsError() throws Exception {
        String wrongId = "abc";
        HttpResponse<String> resp = makeRequest(BASE + "/movies/" + wrongId, RequestType.GET);
        assertEquals(
                400,
                resp.statusCode(),
                "GET /movies/" + wrongId + " должен вернуть 400"
        );

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String movieJson = resp.body().trim();

        JsonElement jsonElement = JsonParser.parseString(movieJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");
        ErrorResponse responseError = gson.fromJson(jsonElement, ErrorResponse.class);
        assertEquals(
                new ErrorResponse("Невалидный ID", new String[]{}),
                responseError,
                "Ответ сервера и ожидаемый результат должны совпадать"
        );
    }

    @Test
    void deleteMovieById_whenIdIsCorrect_returnsMovieJson() throws Exception {
        String[] titles = { "AAA", "AAБ", "AAВ" };
        int[] years = { 1999, 2001, 2002 };

        List<Movie> movies = List.of(
                MovieMaker.makeMovie(titles[0], years[0]),
                MovieMaker.makeMovie(titles[1], years[1]),
                MovieMaker.makeMovie(titles[2], years[2])
        );

        movies.forEach(movie -> store.addMovie(movie));
        Movie movieToDelete = movies.get(1);
        HttpResponse<String> resp = makeRequest(BASE + "/movies/" + movieToDelete.getId(), RequestType.DELETE);
        assertEquals(
                204,
                resp.statusCode(),
                "DELETE /movies/" + movieToDelete.getId() + " должен вернуть 204"
        );

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String body = resp.body().trim();

        assertTrue(body.isEmpty(), "Тело ответа должно быть пустым");
        assertTrue(
                store.getMovie(movieToDelete.getId()).isEmpty(),
                "Фильм с id - " + movieToDelete.getId() + "должен отсутствовать в хранилище");
        assertEquals(
                movies.size() - 1,
                store.getMovies().length,
                "Количество фильмов в хранилище должно быть на одно меньше"
        );
    }

    @Test
    void deleteMovieById_whenIdNotFound_returnsError() throws Exception {
        String[] titles = { "AAA", "AAБ", "AAВ" };
        int[] years = { 1999, 2001, 2002 };

        List<Movie> movies = List.of(
                MovieMaker.makeMovie(titles[0], years[0]),
                MovieMaker.makeMovie(titles[1], years[1]),
                MovieMaker.makeMovie(titles[2], years[2])
        );

        movies.forEach(movie -> store.addMovie(movie));
        Movie movieToDelete = MovieMaker.makeMovie("ААГ", 2021);
        HttpResponse<String> resp = makeRequest(BASE + "/movies/" + movieToDelete.getId(), RequestType.DELETE);
        assertEquals(
                404,
                resp.statusCode(),
                "DELETE /movies/" + movieToDelete.getId() + " должен вернуть 404"
        );

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String movieJson = resp.body().trim();

        JsonElement jsonElement = JsonParser.parseString(movieJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");
        ErrorResponse responseError = gson.fromJson(jsonElement, ErrorResponse.class);
        assertEquals(
                new ErrorResponse("Фильм не найден", new String[]{}),
                responseError,
                "Ответ сервера и ожидаемый результат должны совпадать"
        );

        assertEquals(movies.size(), store.getMovies().length, "Количество элементов не должно изменяться");
    }

    @Test
    void deleteMovieById_whenIdIsNotANumber_returnsError() throws Exception {
        String wrongId = "abc";
        HttpResponse<String> resp = makeRequest(BASE + "/movies/" + wrongId, RequestType.DELETE);
        assertEquals(
                400,
                resp.statusCode(),
                "DELETE /movies/" + wrongId + " должен вернуть 400"
        );

        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String movieJson = resp.body().trim();

        JsonElement jsonElement = JsonParser.parseString(movieJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");
        ErrorResponse responseError = gson.fromJson(jsonElement, ErrorResponse.class);
        assertEquals(
                new ErrorResponse("Невалидный ID", new String[]{}),
                responseError,
                "Ответ сервера и ожидаемый результат должны совпадать"
        );
    }

    @Test
    void getMoviesByYear_whenYearIsCorrect_returnsMovieArray() throws Exception {
        String[] titles = { "AAA", "AAБ", "AAВ" };
        int[] years = { 1999, 1999, 2002 };

        List<Movie> movies = List.of(
                MovieMaker.makeMovie(titles[0], years[0]),
                MovieMaker.makeMovie(titles[1], years[1]),
                MovieMaker.makeMovie(titles[2], years[2])
        );
        movies.forEach(movie -> store.addMovie(movie));

        int yearRequest = 1999;
        HttpResponse<String> resp = makeRequest(BASE + "/movies?year=" + yearRequest, RequestType.GET);
        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies?year=" + yearRequest + " должен вернуть 200"
        );
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String moviesArrayJson = resp.body().trim();

        JsonElement jsonElement = JsonParser.parseString(moviesArrayJson);

        assertTrue(jsonElement.isJsonArray(), "Ожидается JSON-массив");
        List<Movie> moviesResponse = gson.fromJson(moviesArrayJson, new ListOfMoviesTypeToken());

        assertEquals(List.of(movies.get(0), movies.get(1)), moviesResponse, "Список из ответа сервера должен совпадать с ожидаемым списком");
    }

    @Test
    void getMoviesByYear_whenYearNotFound_returnsEmptyArray() throws Exception {
        String[] titles = { "AAA", "AAБ", "AAВ" };
        int[] years = { 1999, 1999, 2002 };

        List<Movie> movies = List.of(
                MovieMaker.makeMovie(titles[0], years[0]),
                MovieMaker.makeMovie(titles[1], years[1]),
                MovieMaker.makeMovie(titles[2], years[2])
        );
        movies.forEach(movie -> store.addMovie(movie));

        int yearRequest = 2003;
        HttpResponse<String> resp = makeRequest(BASE + "/movies?year=" + yearRequest, RequestType.GET);
        assertEquals(
                200,
                resp.statusCode(),
                "GET /movies?year=" + yearRequest + " должен вернуть 200"
        );
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String moviesArrayJson = resp.body().trim();

        JsonElement jsonElement = JsonParser.parseString(moviesArrayJson);

        assertTrue(jsonElement.isJsonArray(), "Ожидается JSON-массив");
        List<Movie> moviesResponse = gson.fromJson(moviesArrayJson, new ListOfMoviesTypeToken());
        assertEquals(
                List.of(),
                moviesResponse,
                "Список из ответа сервера должен быть пустым"
        );
    }

    @Test
    void getMoviesByYear_whenYearIsNotANumber_returnsError() throws Exception {
        String corruptYear = "20a03";
        HttpResponse<String> resp = makeRequest(BASE + "/movies?year=" + corruptYear, RequestType.GET);
        assertEquals(
                400,
                resp.statusCode(),
                "GET /movies?year=" + corruptYear + " должен вернуть 400"
        );
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        String responseJson = resp.body().trim();

        JsonElement jsonElement = JsonParser.parseString(responseJson);

        assertTrue(jsonElement.isJsonObject(), "Ожидается JSON-объект");
        ErrorResponse response = gson.fromJson(jsonElement, ErrorResponse.class);
        assertEquals(
                new ErrorResponse("Некорректный параметр запроса — 'year'", new String[]{}),
                response,
                "Ответ сервера и ожидаемый результат должны совпадать"
        );
    }

    private HttpResponse<String> makeRequest(String uriString, RequestType requestType) throws Exception {
        return makeRequest(uriString, requestType, "");
    }

    private HttpResponse<String> makeRequest(String uriString, RequestType requestType, String json) throws Exception {
        return makeRequest(uriString,requestType, "application/json; charset=UTF-8", json);
    }

    private HttpResponse<String> makeRequest(String uriString, RequestType requestType, String contentType, String json) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(uriString))
                .header("Content-Type", contentType);

        switch (requestType) {
            case GET -> builder = builder.GET();
            case POST -> builder = builder.POST(HttpRequest.BodyPublishers.ofString(json));
            case DELETE -> builder = builder.DELETE();
        }
        HttpRequest req = builder.build();
        return client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

    }
}