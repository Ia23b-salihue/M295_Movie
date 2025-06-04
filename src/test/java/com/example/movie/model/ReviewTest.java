package com.example.movie.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReviewTest {

    private Review review;

    @BeforeEach
    void setUp() {
        review = new Review();
    }

    @Test
    void testIdGetterSetter() {
        review.setId(100L);
        assertEquals(100L, review.getId());
    }

    @Test
    void testUsernameGetterSetter() {
        review.setUsername("MaxMustermann");
        assertEquals("MaxMustermann", review.getUsername());
    }

    @Test
    void testCommentGetterSetter() {
        String comment = "Sehr guter Film!";
        review.setComment(comment);
        assertEquals(comment, review.getComment());
    }

    @Test
    void testRatingGetterSetter() {
        review.setRating(7);
        assertEquals(7, review.getRating());
    }

    @Test
    void testMovieGetterSetter() {
        Movie movie = new Movie();
        movie.setId(1L);
        review.setMovie(movie);
        assertEquals(movie, review.getMovie());
        assertEquals(1L, review.getMovie().getId());
    }
}
