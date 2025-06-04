package com.example.movie.exception;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(Long id) {
        super("Review with ID " + id + " not found.");
    }
}
