package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class MoviesStore {

    private Map<Integer, Movie> movies = new LinkedHashMap<>();

    public void addMovie(Movie movie) {
        movies.put(movie.getId(), movie);
    }

    public Movie[] getMovies() {
        return movies.values().toArray(new Movie[0]);
    }

    public Movie[] getMovies(int year) {
        return movies.values()
                .stream()
                .filter(movie -> movie.getYear() == year)
                .toArray(Movie[]::new);
    }

    public Optional<Movie> getMovie(int id) {
        if (movies.containsKey(id)) {
            return Optional.of(movies.get(id));
        }
        return Optional.empty();
    }

    public boolean deleteMovie(int id) {
        boolean containsKey = movies.containsKey(id);
        movies.remove(id);
        return containsKey;
    }

    public void clear() {
        movies.clear();
    }

}