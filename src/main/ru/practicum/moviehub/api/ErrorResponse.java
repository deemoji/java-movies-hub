package ru.practicum.moviehub.api;

import java.util.Arrays;
import java.util.Objects;

public class ErrorResponse {
    private final String error;
    private final String[] details;

    public ErrorResponse(String error, String[] details) {
        this.error = error;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public String[] getDetails() {
        return details;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorResponse that)) return false;
        return Objects.equals(error, that.error) && Objects.deepEquals(details, that.details);
    }

    @Override
    public int hashCode() {
        return Objects.hash(error, Arrays.hashCode(details));
    }
}