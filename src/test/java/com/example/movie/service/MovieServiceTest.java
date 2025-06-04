package com.example.movie.service;

import com.example.movie.model.Movie;
import com.example.movie.model.Review;
import com.example.movie.repository.MovieRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private Validator validator;

    @InjectMocks
    private MovieService movieService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Movie buildMovie(Long id) {
        Movie m = new Movie();
        m.setId(id);
        m.setTitle("Movie " + id);
        m.setGenre("Genre " + id);
        m.setReleaseDate(LocalDate.of(2024, 1, (int) Math.min(28, id)));
        m.setAgeRating(12);
        m.setAverageRating(4.5);
        m.setRecommended(id % 2 == 0);
        m.setReviews(new ArrayList<>());
        return m;
    }

    @Test
    void getAllMovies_returnsAll() {
        Movie m1 = buildMovie(1L);
        Movie m2 = buildMovie(2L);
        when(movieRepository.findAll()).thenReturn(List.of(m1, m2));

        List<Movie> result = movieService.getAllMovies();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(m1));
        assertTrue(result.contains(m2));
        verify(movieRepository, times(1)).findAll();
    }

    @Test
    void getMovieById_existingId_returnsOptional() {
        Movie m = buildMovie(1L);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(m));

        Optional<Movie> result = movieService.getMovieById(1L);

        assertTrue(result.isPresent());
        assertEquals(m, result.get());
        verify(movieRepository, times(1)).findById(1L);
    }

    @Test
    void getMovieById_nonExistingId_returnsEmpty() {
        when(movieRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Movie> result = movieService.getMovieById(99L);

        assertFalse(result.isPresent());
        verify(movieRepository, times(1)).findById(99L);
    }

    @Test
    void existsById_trueCase() {
        when(movieRepository.existsById(5L)).thenReturn(true);

        boolean exists = movieService.existsById(5L);

        assertTrue(exists);
        verify(movieRepository, times(1)).existsById(5L);
    }

    @Test
    void existsById_falseCase() {
        when(movieRepository.existsById(7L)).thenReturn(false);

        boolean exists = movieService.existsById(7L);

        assertFalse(exists);
        verify(movieRepository, times(1)).existsById(7L);
    }

    @Test
    void getMoviesByRecommended_true_returnsList() {
        Movie m1 = buildMovie(2L);
        when(movieRepository.findByRecommended(true)).thenReturn(List.of(m1));

        List<Movie> result = movieService.getMoviesByRecommended(true);

        assertEquals(1, result.size());
        assertEquals(m1, result.get(0));
        verify(movieRepository, times(1)).findByRecommended(true);
    }

    @Test
    void getMoviesByRecommended_false_returnsEmptyList() {
        when(movieRepository.findByRecommended(false)).thenReturn(Collections.emptyList());

        List<Movie> result = movieService.getMoviesByRecommended(false);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(movieRepository, times(1)).findByRecommended(false);
    }

    @Test
    void getMoviesByGenre_foundReturnsList() {
        Movie m1 = buildMovie(1L);
        Movie m2 = buildMovie(2L);
        when(movieRepository.findByGenreContainingIgnoreCase("Sci"))
                .thenReturn(List.of(m1, m2));

        List<Movie> result = movieService.getMoviesByGenre("Sci");

        assertEquals(2, result.size());
        verify(movieRepository, times(1)).findByGenreContainingIgnoreCase("Sci");
    }

    @Test
    void getMoviesByGenre_notFoundReturnsEmptyList() {
        when(movieRepository.findByGenreContainingIgnoreCase("Horror"))
                .thenReturn(Collections.emptyList());

        List<Movie> result = movieService.getMoviesByGenre("Horror");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(movieRepository, times(1)).findByGenreContainingIgnoreCase("Horror");
    }

    @Test
    void createMovie_validMovie_savesAndReturns() {
        Movie toCreate = buildMovie(3L);
        when(validator.validate(toCreate)).thenReturn(Collections.emptySet());
        when(movieRepository.save(toCreate)).thenReturn(toCreate);

        Movie saved = movieService.createMovie(toCreate);

        assertNotNull(saved);
        assertEquals(toCreate, saved);
        verify(validator, times(1)).validate(toCreate);
        verify(movieRepository, times(1)).save(toCreate);
    }

    @Test
    void createMovie_invalidMovie_throwsIllegalArgumentException() {
        Movie badMovie = buildMovie(4L);

        Path pathMock = mock(Path.class);
        when(pathMock.toString()).thenReturn("title");

        @SuppressWarnings("unchecked")
        ConstraintViolation<Movie> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(pathMock);
        when(violation.getMessage()).thenReturn("must not be blank");

        Set<ConstraintViolation<Movie>> violations = Set.of(violation);
        when(validator.validate(badMovie)).thenReturn(violations);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> movieService.createMovie(badMovie)
        );

        String msg = ex.getMessage();
        assertTrue(msg.contains("Movie Validation Fehler:"));
        assertTrue(msg.contains("title must not be blank"));

        verify(validator, times(1)).validate(badMovie);
        verify(movieRepository, never()).save(any());
    }

    @Test
    void createMovies_allValid_savesAll() {
        Movie m1 = buildMovie(5L);
        Movie m2 = buildMovie(6L);
        List<Movie> inputList = List.of(m1, m2);

        when(validator.validate(any(Movie.class))).thenReturn(Collections.emptySet());
        when(movieRepository.saveAll(inputList)).thenReturn(inputList);

        List<Movie> savedList = movieService.createMovies(inputList);

        assertEquals(2, savedList.size());
        verify(validator, times(1)).validate(m1);
        verify(validator, times(1)).validate(m2);
        verify(movieRepository, times(1)).saveAll(inputList);
    }

    @Test
    void createMovies_oneInvalid_throwsIllegalArgumentException_beforeSaving() {
        Movie good = buildMovie(7L);
        Movie bad = buildMovie(8L);

        when(validator.validate(good)).thenReturn(Collections.emptySet());

        Path pathMock = mock(Path.class);
        when(pathMock.toString()).thenReturn("genre");

        @SuppressWarnings("unchecked")
        ConstraintViolation<Movie> vio = mock(ConstraintViolation.class);
        when(vio.getPropertyPath()).thenReturn(pathMock);
        when(vio.getMessage()).thenReturn("must not be empty");

        Set<ConstraintViolation<Movie>> oneViolation = Set.of(vio);
        when(validator.validate(bad)).thenReturn(oneViolation);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> movieService.createMovies(List.of(good, bad))
        );

        String msg = ex.getMessage();
        assertTrue(msg.contains("Movie Validation Fehler:"));

        verify(validator, times(1)).validate(good);
        verify(validator, times(1)).validate(bad);
        verify(movieRepository, never()).saveAll(any());
    }

    @Test
    void updateMovie_existingWithoutReviews_updatesFieldsOnly() {
        Long id = 10L;
        Movie existing = buildMovie(id);
        existing.setReviews(new ArrayList<>());

        Movie updated = buildMovie(id);
        updated.setTitle("New Title");
        updated.setGenre("New Genre");
        updated.setReleaseDate(LocalDate.of(2021, 5, 20));
        updated.setAgeRating(18);
        updated.setAverageRating(3.8);
        updated.setRecommended(false);
        updated.setReviews(null);

        when(validator.validate(updated)).thenReturn(Collections.emptySet());
        when(movieRepository.findById(id)).thenReturn(Optional.of(existing));
        when(movieRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        Movie result = movieService.updateMovie(id, updated);

        assertNotNull(result);
        assertEquals("New Title", result.getTitle());
        assertEquals("New Genre", result.getGenre());
        assertEquals(LocalDate.of(2021, 5, 20), result.getReleaseDate());
        assertEquals(18, result.getAgeRating());
        assertEquals(3.8, result.getAverageRating());
        assertFalse(result.isRecommended());
        assertTrue(result.getReviews().isEmpty());

        verify(validator, times(1)).validate(updated);
        verify(movieRepository, times(1)).findById(id);
        verify(movieRepository, times(1)).save(existing);
    }

    @Test
    void updateMovie_existingWithReviews_replacesReviewsAndUpdates() {
        Long id = 11L;
        Movie existing = buildMovie(id);
        Review oldReview = new Review();
        existing.getReviews().add(oldReview);

        Movie updated = buildMovie(id);
        updated.setTitle("Updated With Reviews");
        updated.setGenre("Genre X");
        updated.setReleaseDate(LocalDate.of(2020, 2, 2));
        updated.setAgeRating(16);
        updated.setAverageRating(2.2);
        updated.setRecommended(true);

        Review newR1 = mock(Review.class);
        Review newR2 = mock(Review.class);
        updated.setReviews(new ArrayList<>(List.of(newR1, newR2)));

        when(validator.validate(updated)).thenReturn(Collections.emptySet());
        when(movieRepository.findById(id)).thenReturn(Optional.of(existing));
        when(movieRepository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        Movie result = movieService.updateMovie(id, updated);

        assertEquals("Updated With Reviews", result.getTitle());
        assertEquals("Genre X", result.getGenre());
        assertEquals(LocalDate.of(2020, 2, 2), result.getReleaseDate());
        assertEquals(16, result.getAgeRating());
        assertEquals(2.2, result.getAverageRating());
        assertTrue(result.isRecommended());

        assertTrue(result.getReviews().size() == 2);
        verify(newR1, times(1)).setMovie(existing);
        verify(newR2, times(1)).setMovie(existing);

        verify(validator, times(1)).validate(updated);
        verify(movieRepository, times(1)).findById(id);
        verify(movieRepository, times(1)).save(existing);
    }

    @Test
    void updateMovie_notFound_throwsRuntimeException() {
        Long id = 99L;
        Movie toUpdate = buildMovie(id);
        when(validator.validate(toUpdate)).thenReturn(Collections.emptySet());
        when(movieRepository.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> movieService.updateMovie(id, toUpdate)
        );
        assertTrue(ex.getMessage().contains("Film nicht gefunden mit ID " + id));

        verify(validator, times(1)).validate(toUpdate);
        verify(movieRepository, times(1)).findById(id);
        verify(movieRepository, never()).save(any());
    }

    @Test
    void deleteById_delegatesToRepository() {
        Long id = 15L;
        doNothing().when(movieRepository).deleteById(id);

        movieService.deleteById(id);

        verify(movieRepository, times(1)).deleteById(id);
    }

    @Test
    void deleteByReleaseDateBefore_delegatesToRepository() {
        LocalDate cutoff = LocalDate.of(2019, 12, 31);
        doNothing().when(movieRepository).deleteByReleaseDateBefore(cutoff);

        movieService.deleteByReleaseDateBefore(cutoff);

        verify(movieRepository, times(1)).deleteByReleaseDateBefore(cutoff);
    }

    @Test
    void deleteAll_delegatesToRepository() {
        doNothing().when(movieRepository).deleteAll();

        movieService.deleteAll();

        verify(movieRepository, times(1)).deleteAll();
    }
}
