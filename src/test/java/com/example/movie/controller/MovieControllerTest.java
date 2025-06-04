package com.example.movie.controller;

import com.example.movie.model.Movie;
import com.example.movie.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
@AutoConfigureMockMvc(addFilters = false)
class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MovieService movieService;

    private Movie createSampleMovie(Long id) {
        Movie m = new Movie();
        m.setId(id);
        m.setTitle("Inception");
        m.setGenre("Sci-Fi");
        m.setReleaseDate(LocalDate.of(2010, 7, 16));
        m.setAgeRating(13);
        m.setAverageRating(8.8);
        m.setRecommended(true);
        return m;
    }

    private String toJson(Movie m) throws Exception {
        return objectMapper.writeValueAsString(m);
    }

    @Nested
    @DisplayName("GET‐Endpunkte")
    class GetEndpoints {

        @Test
        @DisplayName("GET /api/movies ‒ liefert 200 + Liste")
        void getAll_ShouldReturn200AndList() throws Exception {
            List<Movie> list = Arrays.asList(createSampleMovie(1L), createSampleMovie(2L));
            when(movieService.getAllMovies()).thenReturn(list);

            mockMvc.perform(get("/api/movies"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[1].title", is("Inception")));
        }

        @Test
        @DisplayName("GET /api/movies/{id} ‒ existent → 200 + Movie")
        void getById_Exists_ShouldReturn200() throws Exception {
            Movie m = createSampleMovie(1L);
            when(movieService.getMovieById(1L)).thenReturn(Optional.of(m));

            mockMvc.perform(get("/api/movies/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.title", is("Inception")));
        }

        @Test
        @DisplayName("GET /api/movies/{id} ‒ nicht existent → 404")
        void getById_NotFound_ShouldReturn404() throws Exception {
            when(movieService.getMovieById(42L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/movies/42"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /api/movies/exists/{id} ‒ liefert boolean")
        void existsById_ShouldReturnBoolean() throws Exception {
            when(movieService.existsById(5L)).thenReturn(true);

            mockMvc.perform(get("/api/movies/exists/5"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));
        }

        @Test
        @DisplayName("GET /api/movies/filter/recommended?recommended=true ‒ liefert gefilterte Liste")
        void filterRecommended_ShouldReturnList() throws Exception {
            List<Movie> recommended = Arrays.asList(createSampleMovie(1L));
            when(movieService.getMoviesByRecommended(true)).thenReturn(recommended);

            mockMvc.perform(get("/api/movies/filter/recommended")
                            .param("recommended", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].recommended", is(true)));
        }

        @Test
        @DisplayName("GET /api/movies/filter/genre?genre=Sci-Fi ‒ liefert gefilterte Liste")
        void filterGenre_ShouldReturnList() throws Exception {
            List<Movie> sciFi = Arrays.asList(createSampleMovie(1L));
            when(movieService.getMoviesByGenre("Sci-Fi")).thenReturn(sciFi);

            mockMvc.perform(get("/api/movies/filter/genre")
                            .param("genre", "Sci-Fi"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].genre", is("Sci-Fi")));
        }
    }

    @Nested
    @DisplayName("POST‐Endpunkte")
    class PostEndpoints {

        @Test
        @DisplayName("POST /api/movies ‒ erstellt neuen Film, liefert 200 + Movie")
        void create_ShouldReturnCreatedMovie() throws Exception {
            Movie m = createSampleMovie(7L);
            String payload = toJson(m);
            when(movieService.createMovie(any(Movie.class))).thenReturn(m);

            mockMvc.perform(post("/api/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(7)))
                    .andExpect(jsonPath("$.title", is("Inception")));
        }

        @Test
        @DisplayName("POST /api/movies/batch ‒ erstellt mehrere Filme, liefert 200 + Liste")
        void createBatch_ShouldReturnList() throws Exception {
            Movie m1 = createSampleMovie(1L);
            Movie m2 = createSampleMovie(2L);
            List<Movie> list = Arrays.asList(m1, m2);
            String payload = "[" + toJson(m1) + "," + toJson(m2) + "]";
            when(movieService.createMovies(anyList())).thenReturn(list);

            mockMvc.perform(post("/api/movies/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[1].id", is(2)));
        }
    }

    @Nested
    @DisplayName("PUT‐Endpunkt")
    class PutEndpoint {

        @Test
        @DisplayName("PUT /api/movies/{id} ‒ existent → 200 + aktualisierter Film")
        void update_Exists_ShouldReturnUpdatedMovie() throws Exception {
            Movie m = createSampleMovie(3L);
            String payload = toJson(m);
            when(movieService.updateMovie(eq(3L), any(Movie.class))).thenReturn(m);

            mockMvc.perform(put("/api/movies/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.title", is("Inception")));
        }

        @Test
        @DisplayName("PUT /api/movies/{id} ‒ nicht existent → 404")
        void update_NotFound_ShouldReturn404() throws Exception {
            Movie m = createSampleMovie(99L);
            String payload = toJson(m);
            when(movieService.updateMovie(eq(99L), any(Movie.class)))
                    .thenThrow(new RuntimeException("nicht gefunden"));

            mockMvc.perform(put("/api/movies/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE‐Endpoints")
    class DeleteEndpoints {

        @Test
        @DisplayName("DELETE /api/movies/{id} ‒ existent → 204 No Content")
        void deleteById_Exists_ShouldReturn204() throws Exception {
            when(movieService.existsById(10L)).thenReturn(true);
            doNothing().when(movieService).deleteById(10L);

            mockMvc.perform(delete("/api/movies/10"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /api/movies/{id} ‒ nicht existent → 404")
        void deleteById_NotFound_ShouldReturn404() throws Exception {
            when(movieService.existsById(99L)).thenReturn(false);

            mockMvc.perform(delete("/api/movies/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /api/movies/filter/releaseDate?date=2020-01-01 ‒ löscht Filme vor Datum, liefert 204")
        void deleteByReleaseDate_ShouldReturn204() throws Exception {
            doNothing().when(movieService).deleteByReleaseDateBefore(LocalDate.of(2020, 1, 1));

            mockMvc.perform(delete("/api/movies/filter/releaseDate")
                            .param("date", "2020-01-01"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("DELETE /api/movies ‒ löscht alle Filme, liefert 204")
        void deleteAll_ShouldReturn204() throws Exception {
            doNothing().when(movieService).deleteAll();

            mockMvc.perform(delete("/api/movies"))
                    .andExpect(status().isNoContent());
        }
    }
}
