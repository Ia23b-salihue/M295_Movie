package com.example.movie.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReviewTest {

    private Review review;
    private Movie movie;

    @BeforeEach
    void setUp() {
        review = new Review();
        movie = new Movie();
    }

    @Test
    void testIdGetterSetter() {
        review.setId(5L);
        assertEquals(5L, review.getId());
    }

    @Test
    void testUsernameGetterSetter() {
        review.setUsername("john_doe");
        assertEquals("john_doe", review.getUsername());
    }

    @Test
    void testCommentGetterSetter() {
        review.setComment("Great movie!");
        assertEquals("Great movie!", review.getComment());
    }

    @Test
    void testRatingGetterSetter() {
        review.setRating(4);
        assertEquals(4, review.getRating());
    }

    @Test
    void testMovieGetterSetter() {
        review.setMovie(movie);
        assertSame(movie, review.getMovie());
    }
}
