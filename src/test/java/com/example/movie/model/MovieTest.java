package com.example.movie.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MovieTest {

    private Movie movie;

    @BeforeEach
    void setUp() {
        movie = new Movie();
    }

    @Test
    void testIdGetterSetter() {
        movie.setId(10L);
        assertEquals(10L, movie.getId());
    }

    @Test
    void testTitleGetterSetter() {
        movie.setTitle("Inception");
        assertEquals("Inception", movie.getTitle());
    }

    @Test
    void testGenreGetterSetter() {
        movie.setGenre("Sci-Fi");
        assertEquals("Sci-Fi", movie.getGenre());
    }

    @Test
    void testReleaseDateGetterSetter() {
        LocalDate date = LocalDate.of(2020, 1, 1);
        movie.setReleaseDate(date);
        assertEquals(date, movie.getReleaseDate());
    }

    @Test
    void testAgeRatingGetterSetter() {
        movie.setAgeRating(12);
        assertEquals(12, movie.getAgeRating());
    }

    @Test
    void testAverageRatingGetterSetter() {
        movie.setAverageRating(4.5);
        assertEquals(4.5, movie.getAverageRating());
    }

    @Test
    void testRecommendedGetterSetter() {
        movie.setRecommended(true);
        assertTrue(movie.isRecommended());

        movie.setRecommended(false);
        assertFalse(movie.isRecommended());
    }

    @Test
    void testReviewsGetterSetter() {
        Review review1 = new Review();
        Review review2 = new Review();

        List<Review> reviews = new ArrayList<>();
        reviews.add(review1);
        reviews.add(review2);

        movie.setReviews(reviews);
        assertEquals(2, movie.getReviews().size());
        assertSame(review1, movie.getReviews().get(0));
        assertSame(review2, movie.getReviews().get(1));
    }
}
