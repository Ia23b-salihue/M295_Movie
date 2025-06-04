package com.example.movie.controller;

import com.example.movie.model.Movie;
import com.example.movie.service.MovieService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovieController.class)
@Import(com.example.movie.security.SecurityConfig.class)
class MovieControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private MovieService movieService;

    private String toJson(Movie m) {
        return String.format(
                "{\"id\":%d,\"title\":\"%s\",\"genre\":\"%s\",\"releaseDate\":\"%s\",\"ageRating\":%d,\"averageRating\":%.1f,\"recommended\":%s}",
                m.getId(),
                m.getTitle(),
                m.getGenre(),
                m.getReleaseDate().toString(),
                m.getAgeRating(),
                m.getAverageRating(),
                m.isRecommended()
        );
    }

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

    @Nested
    @DisplayName("GET‐Endpunkte: Zugriff für USER und ADMIN erlaubt, für anonym verboten")
    class GetEndpoints {

        @Test
        @DisplayName("GET /api/movies ‒ als USER erhält 200 + Liste")
        @WithMockUser(username = "user", roles = {"USER"})
        void getAll_AsUser_ShouldReturn200() throws Exception {
            List<Movie> list = Arrays.asList(createSampleMovie(1L), createSampleMovie(2L));
            when(movieService.getAllMovies()).thenReturn(list);

            mockMvc.perform(get("/api/movies"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[1].title", is("Inception")));

            verify(movieService, times(1)).getAllMovies();
        }

        @Test
        @DisplayName("GET /api/movies ‒ ohne Authentifizierung → 401 Unauthorized")
        void getAll_Anonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/movies"))
                    .andExpect(status().isUnauthorized());
            verify(movieService, never()).getAllMovies();
        }

        @Test
        @DisplayName("GET /api/movies/{id} ‒ als USER + existent → 200 + Movie")
        @WithMockUser(username = "user", roles = {"USER"})
        void getById_Exists_AsUser_ShouldReturn200() throws Exception {
            Movie m = createSampleMovie(1L);
            when(movieService.getMovieById(1L)).thenReturn(Optional.of(m));

            mockMvc.perform(get("/api/movies/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.title", is("Inception")));

            verify(movieService, times(1)).getMovieById(1L);
        }

        @Test
        @DisplayName("GET /api/movies/{id} ‒ als USER + nicht existent → 404 Not Found")
        @WithMockUser(username = "user", roles = {"USER"})
        void getById_NotFound_AsUser_ShouldReturn404() throws Exception {
            when(movieService.getMovieById(42L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/movies/42"))
                    .andExpect(status().isNotFound());

            verify(movieService, times(1)).getMovieById(42L);
        }

        @Test
        @DisplayName("GET /api/movies/{id} ‒ ohne Authentifizierung → 401")
        void getById_Anonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/movies/1"))
                    .andExpect(status().isUnauthorized());
            verify(movieService, never()).getMovieById(anyLong());
        }

        @Test
        @DisplayName("GET /api/movies/exists/{id} ‒ als USER → 200 + boolean")
        @WithMockUser(username = "user", roles = {"USER"})
        void existsById_AsUser_ShouldReturn200() throws Exception {
            when(movieService.existsById(5L)).thenReturn(true);

            mockMvc.perform(get("/api/movies/exists/5"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("true"));

            verify(movieService, times(1)).existsById(5L);
        }

        @Test
        @DisplayName("GET /api/movies/filter/recommended?recommended=true ‒ als USER → 200 + Liste")
        @WithMockUser(username = "user", roles = {"USER"})
        void filterRecommended_AsUser_ShouldReturn200() throws Exception {
            List<Movie> recommended = Arrays.asList(createSampleMovie(1L));
            when(movieService.getMoviesByRecommended(true)).thenReturn(recommended);

            mockMvc.perform(get("/api/movies/filter/recommended")
                            .param("recommended", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].recommended", is(true)));

            verify(movieService, times(1)).getMoviesByRecommended(true);
        }

        @Test
        @DisplayName("GET /api/movies/filter/genre?genre=Sci-Fi ‒ als USER → 200 + Liste")
        @WithMockUser(username = "user", roles = {"USER"})
        void filterGenre_AsUser_ShouldReturn200() throws Exception {
            List<Movie> sciFi = Arrays.asList(createSampleMovie(1L));
            when(movieService.getMoviesByGenre("Sci-Fi")).thenReturn(sciFi);

            mockMvc.perform(get("/api/movies/filter/genre")
                            .param("genre", "Sci-Fi"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].genre", is("Sci-Fi")));

            verify(movieService, times(1)).getMoviesByGenre("Sci-Fi");
        }

        @Test
        @DisplayName("GET‐Filter‐Endpunkte ‒ ohne Authentifizierung → 401")
        void filters_Anonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/movies/filter/genre").param("genre", "X"))
                    .andExpect(status().isUnauthorized());
            mockMvc.perform(get("/api/movies/filter/recommended").param("recommended", "false"))
                    .andExpect(status().isUnauthorized());
            verify(movieService, never()).getMoviesByGenre(anyString());
            verify(movieService, never()).getMoviesByRecommended(anyBoolean());
        }
    }

    @Nested
    @DisplayName("POST‐Endpunkte: nur ADMIN, USER=403, anonym=401")
    class PostEndpoints {

        @Test
        @DisplayName("POST /api/movies ‒ als ADMIN → 200 + erstellter Film")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void create_AsAdmin_ShouldReturn200() throws Exception {
            Movie m = createSampleMovie(7L);
            String payload = toJson(m);
            when(movieService.createMovie(any(Movie.class))).thenReturn(m);

            mockMvc.perform(post("/api/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(7)))
                    .andExpect(jsonPath("$.title", is("Inception")));

            verify(movieService, times(1)).createMovie(any(Movie.class));
        }

        @Test
        @DisplayName("POST /api/movies ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void create_AsUser_ShouldReturn403() throws Exception {
            Movie m = createSampleMovie(7L);
            String payload = toJson(m);

            mockMvc.perform(post("/api/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());

            verify(movieService, never()).createMovie(any(Movie.class));
        }

        @Test
        @DisplayName("POST /api/movies ‒ anonym → 401 Unauthorized")
        void create_Anonymous_ShouldReturn401() throws Exception {
            Movie m = createSampleMovie(7L);
            String payload = toJson(m);

            mockMvc.perform(post("/api/movies")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized());

            verify(movieService, never()).createMovie(any(Movie.class));
        }

        @Test
        @DisplayName("POST /api/movies/batch ‒ als ADMIN → 200 + Liste")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void createBatch_AsAdmin_ShouldReturn200() throws Exception {
            Movie m1 = createSampleMovie(1L);
            Movie m2 = createSampleMovie(2L);
            List<Movie> list = Arrays.asList(m1, m2);
            String payload = "[" + toJson(m1) + "," + toJson(m2) + "]";
            when(movieService.createMovies(anyList())).thenReturn(list);

            mockMvc.perform(post("/api/movies/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[1].id", is(2)));

            verify(movieService, times(1)).createMovies(anyList());
        }

        @Test
        @DisplayName("POST /api/movies/batch ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void createBatch_AsUser_ShouldReturn403() throws Exception {
            Movie m1 = createSampleMovie(1L);
            String payload = "[" + toJson(m1) + "]";
            mockMvc.perform(post("/api/movies/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
            verify(movieService, never()).createMovies(anyList());
        }

        @Test
        @DisplayName("POST /api/movies/batch ‒ anonym → 401")
        void createBatch_Anonymous_ShouldReturn401() throws Exception {
            Movie m1 = createSampleMovie(1L);
            String payload = "[" + toJson(m1) + "]";
            mockMvc.perform(post("/api/movies/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized());
            verify(movieService, never()).createMovies(anyList());
        }
    }

    @Nested
    @DisplayName("PUT‐Endpunkt: nur ADMIN, USER=403, anonym=401")
    class PutEndpoint {

        @Test
        @DisplayName("PUT /api/movies/{id} ‒ als ADMIN + existent → 200 + aktualisierter Film")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void update_AsAdmin_Exists_ShouldReturn200() throws Exception {
            Movie m = createSampleMovie(3L);
            String payload = toJson(m);
            when(movieService.updateMovie(eq(3L), any(Movie.class))).thenReturn(m);

            mockMvc.perform(put("/api/movies/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(3)));

            verify(movieService, times(1)).updateMovie(eq(3L), any(Movie.class));
        }

        @Test
        @DisplayName("PUT /api/movies/{id} ‒ als ADMIN + nicht existent → 404 Not Found")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void update_AsAdmin_NotFound_ShouldReturn404() throws Exception {
            Movie m = createSampleMovie(99L);
            String payload = toJson(m);
            when(movieService.updateMovie(eq(99L), any(Movie.class)))
                    .thenThrow(new RuntimeException("nicht gefunden"));

            mockMvc.perform(put("/api/movies/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound());

            verify(movieService, times(1)).updateMovie(eq(99L), any(Movie.class));
        }

        @Test
        @DisplayName("PUT /api/movies/{id} ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void update_AsUser_ShouldReturn403() throws Exception {
            Movie m = createSampleMovie(3L);
            String payload = toJson(m);

            mockMvc.perform(put("/api/movies/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());

            verify(movieService, never()).updateMovie(anyLong(), any(Movie.class));
        }

        @Test
        @DisplayName("PUT /api/movies/{id} ‒ anonym → 401")
        void update_Anonymous_ShouldReturn401() throws Exception {
            Movie m = createSampleMovie(3L);
            String payload = toJson(m);

            mockMvc.perform(put("/api/movies/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized());

            verify(movieService, never()).updateMovie(anyLong(), any(Movie.class));
        }
    }

    @Nested
    @DisplayName("DELETE‐Endpoints: nur ADMIN, USER=403, anonym=401")
    class DeleteEndpoints {

        @Test
        @DisplayName("DELETE /api/movies/{id} ‒ als ADMIN + existent → 204 No Content")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void deleteById_AsAdmin_Exists_ShouldReturn204() throws Exception {
            when(movieService.existsById(10L)).thenReturn(true);
            doNothing().when(movieService).deleteById(10L);

            mockMvc.perform(delete("/api/movies/10"))
                    .andExpect(status().isNoContent());

            verify(movieService, times(1)).existsById(10L);
            verify(movieService, times(1)).deleteById(10L);
        }

        @Test
        @DisplayName("DELETE /api/movies/{id} ‒ als ADMIN + nicht existent → 404 Not Found")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void deleteById_AsAdmin_NotFound_ShouldReturn404() throws Exception {
            when(movieService.existsById(99L)).thenReturn(false);

            mockMvc.perform(delete("/api/movies/99"))
                    .andExpect(status().isNotFound());

            verify(movieService, times(1)).existsById(99L);
            verify(movieService, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("DELETE /api/movies/{id} ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void deleteById_AsUser_ShouldReturn403() throws Exception {
            mockMvc.perform(delete("/api/movies/5"))
                    .andExpect(status().isForbidden());
            verify(movieService, never()).existsById(anyLong());
            verify(movieService, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("DELETE /api/movies/{id} ‒ anonym → 401")
        void deleteById_Anonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(delete("/api/movies/5"))
                    .andExpect(status().isUnauthorized());
            verify(movieService, never()).existsById(anyLong());
            verify(movieService, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("DELETE /api/movies/filter/releaseDate?date=2020-01-01 ‒ als ADMIN → 204 No Content")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void deleteByReleaseDate_AsAdmin_ShouldReturn204() throws Exception {
            doNothing().when(movieService).deleteByReleaseDateBefore(LocalDate.of(2020, 1, 1));

            mockMvc.perform(delete("/api/movies/filter/releaseDate")
                            .param("date", "2020-01-01"))
                    .andExpect(status().isNoContent());

            verify(movieService, times(1)).deleteByReleaseDateBefore(LocalDate.of(2020, 1, 1));
        }

        @Test
        @DisplayName("DELETE /api/movies/filter/releaseDate?date=2020-01-01 ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void deleteByReleaseDate_AsUser_ShouldReturn403() throws Exception {
            mockMvc.perform(delete("/api/movies/filter/releaseDate")
                            .param("date", "2020-01-01"))
                    .andExpect(status().isForbidden());

            verify(movieService, never()).deleteByReleaseDateBefore(any());
        }

        @Test
        @DisplayName("DELETE /api/movies/filter/releaseDate?date=2020-01-01 ‒ anonym → 401")
        void deleteByReleaseDate_Anonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(delete("/api/movies/filter/releaseDate")
                            .param("date", "2020-01-01"))
                    .andExpect(status().isUnauthorized());

            verify(movieService, never()).deleteByReleaseDateBefore(any());
        }

        @Test
        @DisplayName("DELETE /api/movies ‒ als ADMIN → 204 No Content")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void deleteAll_AsAdmin_ShouldReturn204() throws Exception {
            doNothing().when(movieService).deleteAll();

            mockMvc.perform(delete("/api/movies"))
                    .andExpect(status().isNoContent());

            verify(movieService, times(1)).deleteAll();
        }

        @Test
        @DisplayName("DELETE /api/movies ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void deleteAll_AsUser_ShouldReturn403() throws Exception {
            mockMvc.perform(delete("/api/movies"))
                    .andExpect(status().isForbidden());

            verify(movieService, never()).deleteAll();
        }

        @Test
        @DisplayName("DELETE /api/movies ‒ anonym → 401")
        void deleteAll_Anonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(delete("/api/movies"))
                    .andExpect(status().isUnauthorized());

            verify(movieService, never()).deleteAll();
        }
    }
}
