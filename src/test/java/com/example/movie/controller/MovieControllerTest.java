package com.example.movie.controller;

import com.example.movie.model.Movie;
import com.example.movie.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MovieController.class)
@AutoConfigureMockMvc(addFilters = false) // disable security filters for controller-only tests
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieService movieService;

    private ObjectMapper objectMapper;
    private Movie sampleMovie;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // for LocalDate serialization

        sampleMovie = new Movie();
        sampleMovie.setId(1L);
        sampleMovie.setTitle("Test Movie");
        sampleMovie.setGenre("Drama");
        sampleMovie.setReleaseDate(LocalDate.of(2023, 5, 1));
        sampleMovie.setAgeRating(12);
        sampleMovie.setAverageRating(4.2);
        sampleMovie.setRecommended(true);
        sampleMovie.setReviews(Collections.emptyList());
    }

    // 1. GET /api/movies → returns list of all movies
    @Test
    void whenGetAllMovies_thenReturnsMovieList() throws Exception {
        when(movieService.getAllMovies()).thenReturn(List.of(sampleMovie));

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Movie"));

        Mockito.verify(movieService).getAllMovies();
    }

    // 2. GET /api/movies/{id} → found
    @Test
    void whenGetMovieByIdFound_thenReturnsMovie() throws Exception {
        when(movieService.getMovieById(1L)).thenReturn(Optional.of(sampleMovie));

        mockMvc.perform(get("/api/movies/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.genre").value("Drama"));

        Mockito.verify(movieService).getMovieById(1L);
    }

    // 3. GET /api/movies/{id} → not found
    @Test
    void whenGetMovieByIdNotFound_thenReturns404() throws Exception {
        when(movieService.getMovieById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/movies/1"))
                .andExpect(status().isNotFound());

        Mockito.verify(movieService).getMovieById(1L);
    }

    // 4. GET /api/movies/exists/{id} → returns boolean
    @Test
    void whenExistsById_thenReturnsTrue() throws Exception {
        when(movieService.existsById(1L)).thenReturn(true);

        mockMvc.perform(get("/api/movies/exists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").value(true));

        Mockito.verify(movieService).existsById(1L);
    }

    // 5. GET /api/movies/filter/recommended?recommended=true → returns filtered list
    @Test
    void whenGetByRecommended_thenReturnsFilteredList() throws Exception {
        when(movieService.getMoviesByRecommended(true)).thenReturn(List.of(sampleMovie));

        mockMvc.perform(get("/api/movies/filter/recommended")
                        .param("recommended", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].recommended").value(true));

        Mockito.verify(movieService).getMoviesByRecommended(true);
    }

    // 6. GET /api/movies/filter/genre?genre=Drama → returns filtered list
    @Test
    void whenGetByGenre_thenReturnsFilteredList() throws Exception {
        when(movieService.getMoviesByGenre("Drama")).thenReturn(List.of(sampleMovie));

        mockMvc.perform(get("/api/movies/filter/genre")
                        .param("genre", "Drama"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].genre").value("Drama"));

        Mockito.verify(movieService).getMoviesByGenre("Drama");
    }

    // 7. POST /api/movies → create new movie
    @Test
    void whenCreateMovie_thenReturnsCreatedMovie() throws Exception {
        when(movieService.createMovie(any(Movie.class))).thenReturn(sampleMovie);
        String payload = objectMapper.writeValueAsString(sampleMovie);

        mockMvc.perform(post("/api/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Movie"));

        Mockito.verify(movieService).createMovie(any(Movie.class));
    }

    // 8. POST /api/movies/batch → create multiple movies
    @Test
    void whenCreateBatch_thenReturnsCreatedMovies() throws Exception {
        Movie second = new Movie();
        second.setId(2L);
        second.setTitle("Another Movie");
        second.setGenre("Comedy");
        second.setReleaseDate(LocalDate.of(2022, 7, 10));
        second.setAgeRating(16);
        second.setAverageRating(3.9);
        second.setRecommended(false);
        second.setReviews(Collections.emptyList());

        List<Movie> batch = List.of(sampleMovie, second);
        when(movieService.createMovies(anyList())).thenReturn(batch);

        String payload = objectMapper.writeValueAsString(batch);
        mockMvc.perform(post("/api/movies/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("Another Movie"));

        Mockito.verify(movieService).createMovies(anyList());
    }

    // 9. PUT /api/movies/{id} → update existing movie
    @Test
    void whenUpdateMovieFound_thenReturnsUpdated() throws Exception {
        Movie updated = new Movie();
        updated.setId(1L);
        updated.setTitle("Updated Movie");
        updated.setGenre("Thriller");
        updated.setReleaseDate(LocalDate.of(2021, 12, 5));
        updated.setAgeRating(18);
        updated.setAverageRating(4.5);
        updated.setRecommended(false);
        updated.setReviews(Collections.emptyList());

        when(movieService.updateMovie(eq(1L), any(Movie.class))).thenReturn(updated);
        String payload = objectMapper.writeValueAsString(updated);

        mockMvc.perform(put("/api/movies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Updated Movie"))
                .andExpect(jsonPath("$.genre").value("Thriller"));

        Mockito.verify(movieService).updateMovie(eq(1L), any(Movie.class));
    }

    // 10. PUT /api/movies/{id} → update non-existent movie
    @Test
    void whenUpdateMovieNotFound_thenReturns404() throws Exception {
        when(movieService.updateMovie(eq(1L), any(Movie.class)))
                .thenThrow(new RuntimeException("Movie not found"));
        String payload = objectMapper.writeValueAsString(sampleMovie);

        mockMvc.perform(put("/api/movies/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());

        Mockito.verify(movieService).updateMovie(eq(1L), any(Movie.class));
    }

    // 11. DELETE /api/movies/{id} → delete existing
    @Test
    void whenDeleteByIdFound_thenNoContent() throws Exception {
        when(movieService.existsById(1L)).thenReturn(true);
        doNothing().when(movieService).deleteById(1L);

        mockMvc.perform(delete("/api/movies/1"))
                .andExpect(status().isNoContent());

        Mockito.verify(movieService).existsById(1L);
        Mockito.verify(movieService).deleteById(1L);
    }

    // 12. DELETE /api/movies/{id} → delete non-existent
    @Test
    void whenDeleteByIdNotFound_thenReturns404() throws Exception {
        when(movieService.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/movies/1"))
                .andExpect(status().isNotFound());

        Mockito.verify(movieService).existsById(1L);
        Mockito.verify(movieService, Mockito.never()).deleteById(anyLong());
    }

    // 13. DELETE /api/movies/filter/releaseDate?date=YYYY-MM-DD → delete by release date
    @Test
    void whenDeleteByReleaseDate_thenNoContent() throws Exception {
        LocalDate cutoff = LocalDate.of(2021, 1, 1);
        doNothing().when(movieService).deleteByReleaseDateBefore(cutoff);

        mockMvc.perform(delete("/api/movies/filter/releaseDate")
                        .param("date", "2021-01-01"))
                .andExpect(status().isNoContent());

        Mockito.verify(movieService).deleteByReleaseDateBefore(cutoff);
    }

    // 14. DELETE /api/movies → delete all
    @Test
    void whenDeleteAll_thenNoContent() throws Exception {
        doNothing().when(movieService).deleteAll();

        mockMvc.perform(delete("/api/movies"))
                .andExpect(status().isNoContent());

        Mockito.verify(movieService).deleteAll();
    }
}
