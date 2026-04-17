package ru.practicum.moviehub.model;

import java.util.Objects;

public class MovieMaker {
    public static Movie makeMovie(String title, int year) {
        return new Movie(Objects.hash(title, year), title, year);
    }
}
