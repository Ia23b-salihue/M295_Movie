package com.example.movie.controller;

import com.example.movie.model.Movie;
import com.example.movie.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MovieController.class)
@Import({com.example.movie.security.SecurityConfig.class})
class MovieControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieService movieService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Movie sampleMovie;

    @BeforeEach
    void setUp() {
        // Register JavaTimeModule so that LocalDate can be serialized
        objectMapper.registerModule(new JavaTimeModule());

        sampleMovie = new Movie();
        sampleMovie.setId(1L);
        sampleMovie.setTitle("Test Movie");
        sampleMovie.setGenre("Drama");
        sampleMovie.setReleaseDate(LocalDate.of(2023, 5, 1));
        sampleMovie.setAgeRating(12);
        sampleMovie.setAverageRating(4.2);
        sampleMovie.setRecommended(true);
        sampleMovie.setReviews(List.of());
    }

    //--------------------------------------------------------------------------------
    // 1. Unauthenticated requests → 401 Unauthorized
    //--------------------------------------------------------------------------------

    @Test
    void whenGetAllWithoutAuth_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenGetByIdWithoutAuth_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/movies/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenPostWithoutAuth_thenUnauthorized() throws Exception {
        String body = objectMapper.writeValueAsString(sampleMovie);
        mockMvc.perform(
                        post("/api/movies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenDeleteWithoutAuth_thenUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/movies/1"))
                .andExpect(status().isUnauthorized());
    }

    //--------------------------------------------------------------------------------
    // 2. Authenticated as USER → no role‐restriction actually applies (because the antMatcher
    //    in SecurityConfig is "/movies/**", not "/api/movies/**"). Once you are authenticated,
    //    you can hit GET/POST/PUT/DELETE under "/api/movies/**".
    //--------------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void whenUserGetsAll_thenOk() throws Exception {
        when(movieService.getAllMovies()).thenReturn(List.of(sampleMovie));

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Validate JSON array with one element having "id":1
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(movieService, times(1)).getAllMovies();
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void whenUserGetsById_thenOk() throws Exception {
        when(movieService.getMovieById(1L)).thenReturn(Optional.of(sampleMovie));

        mockMvc.perform(get("/api/movies/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Test Movie"));

        verify(movieService, times(1)).getMovieById(1L);
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void whenUserPostsNewMovie_thenOk() throws Exception {
        when(movieService.createMovie(any(Movie.class))).thenReturn(sampleMovie);

        String body = objectMapper.writeValueAsString(sampleMovie);
        mockMvc.perform(
                        post("/api/movies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Movie"));

        verify(movieService, times(1)).createMovie(any(Movie.class));
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void whenUserUpdatesMovie_thenOk() throws Exception {
        Movie updated = new Movie();
        updated.setId(1L);
        updated.setTitle("Updated Title");
        updated.setGenre("Comedy");
        updated.setReleaseDate(LocalDate.of(2022, 8, 15));
        updated.setAgeRating(16);
        updated.setAverageRating(3.7);
        updated.setRecommended(false);
        updated.setReviews(List.of());

        when(movieService.updateMovie(eq(1L), any(Movie.class))).thenReturn(updated);

        String body = objectMapper.writeValueAsString(updated);
        mockMvc.perform(
                        put("/api/movies/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.genre").value("Comedy"));

        verify(movieService, times(1)).updateMovie(eq(1L), any(Movie.class));
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void whenUserDeletesExistingMovie_thenNoContent() throws Exception {
        // Stub existsById(true) → deleteById → 204
        when(movieService.existsById(1L)).thenReturn(true);
        doNothing().when(movieService).deleteById(1L);

        mockMvc.perform(delete("/api/movies/1"))
                .andExpect(status().isNoContent());

        verify(movieService, times(1)).existsById(1L);
        verify(movieService, times(1)).deleteById(1L);
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void whenUserDeletesNonExistingMovie_thenNotFound() throws Exception {
        when(movieService.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/movies/1"))
                .andExpect(status().isNotFound());

        verify(movieService, times(1)).existsById(1L);
        verify(movieService, never()).deleteById(anyLong());
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void whenUserDeletesByReleaseDate_thenNoContent() throws Exception {
        // DELETE /api/movies/filter/releaseDate?date=2021-01-01
        doNothing().when(movieService).deleteByReleaseDateBefore(LocalDate.of(2021, 1, 1));

        mockMvc.perform(
                        delete("/api/movies/filter/releaseDate")
                                .param("date", "2021-01-01")
                )
                .andExpect(status().isNoContent());

        verify(movieService, times(1)).deleteByReleaseDateBefore(LocalDate.of(2021, 1, 1));
    }

    @Test
    @WithMockUser(username = "user", roles = { "USER" })
    void whenUserDeletesAll_thenNoContent() throws Exception {
        doNothing().when(movieService).deleteAll();

        mockMvc.perform(delete("/api/movies"))
                .andExpect(status().isNoContent());

        verify(movieService, times(1)).deleteAll();
    }

    //--------------------------------------------------------------------------------
    // 3. Authenticated as ADMIN → exactly the same behavior in this configuration,
    //    because "/movies/**" does not match "/api/movies/**". We only verify
    //    that ADMIN can also successfully GET/POST/DELETE/etc.
    //--------------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void whenAdminGetsAll_thenOk() throws Exception {
        when(movieService.getAllMovies()).thenReturn(List.of(sampleMovie));

        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));

        verify(movieService, times(1)).getAllMovies();
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void whenAdminPostsNewMovie_thenOk() throws Exception {
        when(movieService.createMovie(any(Movie.class))).thenReturn(sampleMovie);

        String body = objectMapper.writeValueAsString(sampleMovie);
        mockMvc.perform(
                        post("/api/movies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Test Movie"));

        verify(movieService, times(1)).createMovie(any(Movie.class));
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    void whenAdminDeletesByReleaseDate_thenNoContent() throws Exception {
        doNothing().when(movieService).deleteByReleaseDateBefore(LocalDate.of(2020, 12, 31));

        mockMvc.perform(
                        delete("/api/movies/filter/releaseDate")
                                .param("date", "2020-12-31")
                )
                .andExpect(status().isNoContent());

        verify(movieService, times(1)).deleteByReleaseDateBefore(LocalDate.of(2020, 12, 31));
    }
}
