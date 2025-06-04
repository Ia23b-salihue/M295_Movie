package com.example.movie.service;

import com.example.movie.exception.MovieNotFoundException;
import com.example.movie.exception.ReviewNotFoundException;
import com.example.movie.model.Movie;
import com.example.movie.model.Review;
import com.example.movie.repository.MovieRepository;
import com.example.movie.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Movie buildMovie(Long id) {
        Movie m = new Movie();
        m.setId(id);
        m.setTitle("Movie " + id);
        return m;
    }

    private Review buildReview(Long id, Long movieId) {
        Review r = new Review();
        r.setId(id);
        r.setUsername("user" + id);
        r.setComment("Comment " + id);
        r.setRating(5);
        if (movieId != null) {
            Movie m = new Movie();
            m.setId(movieId);
            r.setMovie(m);
        }
        return r;
    }

    @Test
    void getAllReviews_returnsAll() {
        Review r1 = buildReview(1L, 10L);
        Review r2 = buildReview(2L, 10L);
        when(reviewRepository.findAll()).thenReturn(List.of(r1, r2));

        List<Review> result = reviewService.getAllReviews();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(r1));
        assertTrue(result.contains(r2));
        verify(reviewRepository, times(1)).findAll();
    }

    @Test
    void getReviewsByMovieId_returnsFilteredList() {
        Review r1 = buildReview(1L, 20L);
        when(reviewRepository.findByMovieId(20L)).thenReturn(List.of(r1));

        List<Review> result = reviewService.getReviewsByMovieId(20L);

        assertEquals(1, result.size());
        assertEquals(r1, result.get(0));
        verify(reviewRepository, times(1)).findByMovieId(20L);
    }

    @Test
    void getReviewById_existingId_returnsOptional() {
        Review r = buildReview(3L, 30L);
        when(reviewRepository.findById(3L)).thenReturn(Optional.of(r));

        Optional<Review> result = reviewService.getReviewById(3L);

        assertTrue(result.isPresent());
        assertEquals(r, result.get());
        verify(reviewRepository, times(1)).findById(3L);
    }

    @Test
    void getReviewById_nonExistingId_returnsEmpty() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Review> result = reviewService.getReviewById(99L);

        assertFalse(result.isPresent());
        verify(reviewRepository, times(1)).findById(99L);
    }

    @Test
    void createReview_validReview_savesAndReturns() {
        Review toCreate = buildReview(null, 40L);
        Movie m = buildMovie(40L);

        when(movieRepository.findById(40L)).thenReturn(Optional.of(m));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review saved = inv.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        Review saved = reviewService.createReview(toCreate);

        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        assertEquals(m, saved.getMovie());
        verify(movieRepository, times(1)).findById(40L);
        verify(reviewRepository, times(1)).save(toCreate);
    }

    @Test
    void createReview_noMovieAttached_throwsIllegalArgumentException() {
        Review toCreate = new Review();
        toCreate.setUsername("user");
        toCreate.setComment("no movie");
        toCreate.setRating(6);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(toCreate)
        );
        assertEquals("Review must be linked to a movie", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void createReview_movieNotFound_throwsMovieNotFoundException() {
        Review toCreate = buildReview(null, 50L);
        when(movieRepository.findById(50L)).thenReturn(Optional.empty());

        MovieNotFoundException ex = assertThrows(
                MovieNotFoundException.class,
                () -> reviewService.createReview(toCreate)
        );

        String expectedMessagePart = "Movie with ID " + 50L + " not found";
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains(expectedMessagePart),
                "Expected exception message to contain: " + expectedMessagePart + " but was: " + ex.getMessage());

        verify(reviewRepository, never()).save(any());
    }


    @Test
    void createReviewsBatch_allValid_savesAll() {
        Review r1 = buildReview(null, 60L);
        Review r2 = buildReview(null, 60L);
        Movie m = buildMovie(60L);
        when(movieRepository.findById(60L)).thenReturn(Optional.of(m));
        when(reviewRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Review> list = inv.getArgument(0);
            list.get(0).setId(10L);
            list.get(1).setId(11L);
            return list;
        });

        List<Review> input = List.of(r1, r2);
        List<Review> savedList = reviewService.createReviewsBatch(input);

        assertEquals(2, savedList.size());
        assertEquals(10L, savedList.get(0).getId());
        assertEquals(11L, savedList.get(1).getId());
        verify(movieRepository, times(2)).findById(60L);
        verify(reviewRepository, times(1)).saveAll(input);
    }

    @Test
    void createReviewsBatch_oneInvalid_throwsIllegalArgumentException() {
        Review r1 = buildReview(null, 70L);
        Review r2 = new Review();
        when(movieRepository.findById(70L)).thenReturn(Optional.of(buildMovie(70L)));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReviewsBatch(Arrays.asList(r1, r2))
        );
        assertEquals("Review must be linked to a movie", ex.getMessage());
        verify(reviewRepository, never()).saveAll(any());
    }

    @Test
    void updateReview_existingId_updatesAndReturns() {
        Long id = 5L;
        Review existing = buildReview(id, 80L);
        Movie oldMovie = buildMovie(80L);
        existing.setMovie(oldMovie);

        Review updatedData = buildReview(null, 90L);
        updatedData.setUsername("newUser");
        updatedData.setComment("updated comment");
        updatedData.setRating(9);

        Movie newMovie = buildMovie(90L);
        when(reviewRepository.findById(id)).thenReturn(Optional.of(existing));
        when(movieRepository.findById(90L)).thenReturn(Optional.of(newMovie));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review result = reviewService.updateReview(id, updatedData);

        assertEquals("newUser", result.getUsername());
        assertEquals("updated comment", result.getComment());
        assertEquals(9, result.getRating());
        assertEquals(newMovie, result.getMovie());
        verify(reviewRepository, times(1)).findById(id);
        verify(movieRepository, times(1)).findById(90L);
        verify(reviewRepository, times(1)).save(existing);
    }

    @Test
    void updateReview_noMovieChange_onlyUpdatesFields() {
        Long id = 6L;
        Review existing = buildReview(id, 100L);
        Movie m = buildMovie(100L);
        existing.setMovie(m);

        Review updatedData = new Review();
        updatedData.setUsername("userX");
        updatedData.setComment("commentX");
        updatedData.setRating(3);

        when(reviewRepository.findById(id)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        Review result = reviewService.updateReview(id, updatedData);

        assertEquals("userX", result.getUsername());
        assertEquals("commentX", result.getComment());
        assertEquals(3, result.getRating());
        assertEquals(m, result.getMovie());
        verify(reviewRepository, times(1)).findById(id);
        verify(movieRepository, never()).findById(any());
        verify(reviewRepository, times(1)).save(existing);
    }

    @Test
    void updateReview_nonExistingId_throwsReviewNotFoundException() {
        Long id = 99L;
        Review updatedData = buildReview(null, 110L);
        when(reviewRepository.findById(id)).thenReturn(Optional.empty());

        ReviewNotFoundException ex = assertThrows(
                ReviewNotFoundException.class,
                () -> reviewService.updateReview(id, updatedData)
        );

        String expectedMessagePart = "Review with ID " + id + " not found";
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains(expectedMessagePart),
                "Expected exception message to contain: " + expectedMessagePart + " but was: " + ex.getMessage());

        verify(reviewRepository, times(1)).findById(id);
        verify(reviewRepository, never()).save(any());
    }


    @Test
    void deleteReviewById_existingId_deletes() {
        Long id = 7L;
        when(reviewRepository.existsById(id)).thenReturn(true);
        doNothing().when(reviewRepository).deleteById(id);

        reviewService.deleteReviewById(id);

        verify(reviewRepository, times(1)).existsById(id);
        verify(reviewRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteReviewById_nonExistingId_throwsReviewNotFoundException() {
        Long id = 88L;
        when(reviewRepository.existsById(id)).thenReturn(false);

        ReviewNotFoundException ex = assertThrows(
                ReviewNotFoundException.class,
                () -> reviewService.deleteReviewById(id)
        );

        String expectedMessagePart = "Review with ID " + id + " not found";
        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains(expectedMessagePart),
                "Expected exception message to contain: " + expectedMessagePart + " but was: " + ex.getMessage());

        verify(reviewRepository, times(1)).existsById(id);
        verify(reviewRepository, never()).deleteById(any());
    }


    @Test
    void deleteAllReviews_delegatesToRepository() {
        doNothing().when(reviewRepository).deleteAll();

        reviewService.deleteAllReviews();

        verify(reviewRepository, times(1)).deleteAll();
    }
}
