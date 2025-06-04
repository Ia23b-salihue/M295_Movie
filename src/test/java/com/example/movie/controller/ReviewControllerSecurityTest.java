package com.example.movie.controller;

import com.example.movie.model.Movie;
import com.example.movie.model.Review;
import com.example.movie.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(com.example.movie.security.SecurityConfig.class)
class ReviewControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private ReviewService reviewService;

    private String toJson(Review r) {
        Long movieId = r.getMovie() != null ? r.getMovie().getId() : null;
        String movieJson = movieId != null
                ? String.format(",\"movie\":{\"id\":%d}", movieId)
                : "";
        return String.format(
                "{\"id\":%d,\"username\":\"%s\",\"comment\":\"%s\",\"rating\":%d%s}",
                r.getId(),
                r.getUsername(),
                r.getComment(),
                r.getRating(),
                movieJson
        );
    }

    private Review createSampleReview(Long id, Long movieId) {
        Review r = new Review();
        r.setId(id);
        r.setUsername("testuser");
        r.setComment("Great movie!");
        r.setRating(8);
        Movie m = new Movie();
        m.setId(movieId);
        r.setMovie(m);
        return r;
    }

    @Nested
    @DisplayName("GET‐Endpunkte: Zugriff für USER und ADMIN erlaubt, für anonym verboten")
    class GetEndpoints {

        @Test
        @DisplayName("GET /api/reviews ‒ als USER erhält 200 + Liste")
        @WithMockUser(username = "user", roles = {"USER"})
        void getAll_AsUser_ShouldReturn200() throws Exception {
            List<Review> list = Arrays.asList(
                    createSampleReview(1L, 10L),
                    createSampleReview(2L, 10L)
            );
            when(reviewService.getAllReviews()).thenReturn(list);

            mockMvc.perform(get("/api/reviews"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[1].username", is("testuser")));

            verify(reviewService, times(1)).getAllReviews();
        }

        @Test
        @DisplayName("GET /api/reviews ‒ ohne Authentifizierung → 401 Unauthorized")
        void getAll_Anonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/reviews"))
                    .andExpect(status().isUnauthorized());
            verify(reviewService, never()).getAllReviews();
        }

        @Test
        @DisplayName("GET /api/reviews?movieId=10 ‒ als USER erhält 200 + gefilterte Liste")
        @WithMockUser(username = "user", roles = {"USER"})
        void getAll_FilterByMovieId_AsUser_ShouldReturn200() throws Exception {
            List<Review> filtered = Arrays.asList(createSampleReview(3L, 10L));
            when(reviewService.getReviewsByMovieId(10L)).thenReturn(filtered);

            mockMvc.perform(get("/api/reviews")
                            .param("movieId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(3)))
                    .andExpect(jsonPath("$[0].username", is("testuser")))
                    .andExpect(jsonPath("$[0].rating", is(8)));

            verify(reviewService, times(1)).getReviewsByMovieId(10L);
        }

        @Test
        @DisplayName("GET /api/reviews/{id} ‒ als USER + existent → 200 + Review")
        @WithMockUser(username = "user", roles = {"USER"})
        void getById_Exists_AsUser_ShouldReturn200() throws Exception {
            Review r = createSampleReview(5L, 20L);
            when(reviewService.getReviewById(5L)).thenReturn(Optional.of(r));

            mockMvc.perform(get("/api/reviews/5"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(5)))
                    .andExpect(jsonPath("$.rating", is(8)))
                    .andExpect(jsonPath("$.username", is("testuser")))
                    .andExpect(jsonPath("$.comment", is("Great movie!")));

            verify(reviewService, times(1)).getReviewById(5L);
        }


        @Test
        @DisplayName("GET /api/reviews/{id} ‒ als USER + nicht existent → 404 Not Found")
        @WithMockUser(username = "user", roles = {"USER"})
        void getById_NotFound_AsUser_ShouldReturn404() throws Exception {
            when(reviewService.getReviewById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/reviews/99"))
                    .andExpect(status().isNotFound());

            verify(reviewService, times(1)).getReviewById(99L);
        }

        @Test
        @DisplayName("GET /api/reviews/{id} ‒ ohne Authentifizierung → 401")
        void getById_Anonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/api/reviews/1"))
                    .andExpect(status().isUnauthorized());
            verify(reviewService, never()).getReviewById(anyLong());
        }
    }

    @Nested
    @DisplayName("POST‐Endpunkte: nur ADMIN, USER=403, anonym=401")
    class PostEndpoints {

        @Test
        @DisplayName("POST /api/reviews ‒ als ADMIN → 200 + erstelltes Review")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void create_AsAdmin_ShouldReturn200() throws Exception {
            Review r = createSampleReview(7L, 30L);
            String payload = toJson(r);
            when(reviewService.createReview(any(Review.class))).thenReturn(r);

            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(7)))
                    .andExpect(jsonPath("$.username", is("testuser")));

            verify(reviewService, times(1)).createReview(any(Review.class));
        }

        @Test
        @DisplayName("POST /api/reviews ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void create_AsUser_ShouldReturn403() throws Exception {
            Review r = createSampleReview(7L, 30L);
            String payload = toJson(r);

            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());

            verify(reviewService, never()).createReview(any(Review.class));
        }

        @Test
        @DisplayName("POST /api/reviews ‒ anonym → 401 Unauthorized")
        void create_Anonymous_ShouldReturn401() throws Exception {
            Review r = createSampleReview(7L, 30L);
            String payload = toJson(r);

            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized());

            verify(reviewService, never()).createReview(any(Review.class));
        }

        @Test
        @DisplayName("POST /api/reviews/batch ‒ als ADMIN → 200 + Liste")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void createBatch_AsAdmin_ShouldReturn200() throws Exception {
            Review r1 = createSampleReview(1L, 40L);
            Review r2 = createSampleReview(2L, 40L);
            List<Review> list = Arrays.asList(r1, r2);
            String payload = "[" +
                    "{\"username\":\"testuser\",\"comment\":\"Great movie!\",\"rating\":8}," +
                    "{\"username\":\"testuser\",\"comment\":\"Great movie!\",\"rating\":8}" +
                    "]";

            when(reviewService.createReviewsBatch(anyList())).thenReturn(list);

            mockMvc.perform(post("/api/reviews/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].username", is("testuser")));

            verify(reviewService, times(1)).createReviewsBatch(anyList());
        }


        @Test
        @DisplayName("POST /api/reviews/batch ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void createBatch_AsUser_ShouldReturn403() throws Exception {
            Review r1 = createSampleReview(1L, 40L);
            String payload = "[" + toJson(r1) + "]";
            mockMvc.perform(post("/api/reviews/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());
            verify(reviewService, never()).createReviewsBatch(anyList());
        }

        @Test
        @DisplayName("POST /api/reviews/batch ‒ anonym → 401")
        void createBatch_Anonymous_ShouldReturn401() throws Exception {
            Review r1 = createSampleReview(1L, 40L);
            String payload = "[" + toJson(r1) + "]";
            mockMvc.perform(post("/api/reviews/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized());
            verify(reviewService, never()).createReviewsBatch(anyList());
        }
    }

    @Nested
    @DisplayName("PUT‐Endpunkt: nur ADMIN, USER=403, anonym=401")
    class PutEndpoint {

        @Test
        @DisplayName("PUT /api/reviews/{id} ‒ als ADMIN + existent → 200 + aktualisiertes Review")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void update_AsAdmin_Exists_ShouldReturn200() throws Exception {
            Review r = createSampleReview(3L, 50L);
            String payload = toJson(r);
            when(reviewService.updateReview(eq(3L), any(Review.class))).thenReturn(r);

            mockMvc.perform(put("/api/reviews/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(3)))
                    .andExpect(jsonPath("$.comment", is("Great movie!")));

            verify(reviewService, times(1)).updateReview(eq(3L), any(Review.class));
        }

        @Test
        @DisplayName("PUT /api/reviews/{id} ‒ als ADMIN + nicht existent → 404 Not Found")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void update_AsAdmin_NotFound_ShouldReturn404() throws Exception {
            Review r = createSampleReview(99L, 60L);
            String payload = toJson(r);
            when(reviewService.updateReview(eq(99L), any(Review.class)))
                    .thenThrow(new RuntimeException("nicht gefunden"));

            mockMvc.perform(put("/api/reviews/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isNotFound());

            verify(reviewService, times(1)).updateReview(eq(99L), any(Review.class));
        }

        @Test
        @DisplayName("PUT /api/reviews/{id} ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void update_AsUser_ShouldReturn403() throws Exception {
            Review r = createSampleReview(3L, 50L);
            String payload = toJson(r);

            mockMvc.perform(put("/api/reviews/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isForbidden());

            verify(reviewService, never()).updateReview(anyLong(), any(Review.class));
        }

        @Test
        @DisplayName("PUT /api/reviews/{id} ‒ anonym → 401")
        void update_Anonymous_ShouldReturn401() throws Exception {
            Review r = createSampleReview(3L, 50L);
            String payload = toJson(r);

            mockMvc.perform(put("/api/reviews/3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isUnauthorized());

            verify(reviewService, never()).updateReview(anyLong(), any(Review.class));
        }
    }

    @Nested
    @DisplayName("DELETE‐Endpoints: nur ADMIN, USER=403, anonym=401")
    class DeleteEndpoints {

        @Test
        @DisplayName("DELETE /api/reviews/{id} ‒ als ADMIN + existent → 204 No Content")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void deleteById_AsAdmin_Exists_ShouldReturn204() throws Exception {
            doNothing().when(reviewService).deleteReviewById(15L);

            mockMvc.perform(delete("/api/reviews/15"))
                    .andExpect(status().isNoContent());

            verify(reviewService, times(1)).deleteReviewById(15L);
        }

        @Test
        @DisplayName("DELETE /api/reviews/{id} ‒ als ADMIN + nicht existent → 404 Not Found")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void deleteById_AsAdmin_NotFound_ShouldReturn404() throws Exception {
            doThrow(new RuntimeException("nicht gefunden")).when(reviewService).deleteReviewById(99L);

            mockMvc.perform(delete("/api/reviews/99"))
                    .andExpect(status().isNotFound());

            verify(reviewService, times(1)).deleteReviewById(99L);
        }

        @Test
        @DisplayName("DELETE /api/reviews/{id} ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void deleteById_AsUser_ShouldReturn403() throws Exception {
            mockMvc.perform(delete("/api/reviews/5"))
                    .andExpect(status().isForbidden());
            verify(reviewService, never()).deleteReviewById(anyLong());
        }

        @Test
        @DisplayName("DELETE /api/reviews/{id} ‒ anonym → 401")
        void deleteById_Anonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(delete("/api/reviews/5"))
                    .andExpect(status().isUnauthorized());
            verify(reviewService, never()).deleteReviewById(anyLong());
        }

        @Test
        @DisplayName("DELETE /api/reviews ‒ als ADMIN → 204 No Content")
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void deleteAll_AsAdmin_ShouldReturn204() throws Exception {
            doNothing().when(reviewService).deleteAllReviews();

            mockMvc.perform(delete("/api/reviews"))
                    .andExpect(status().isNoContent());

            verify(reviewService, times(1)).deleteAllReviews();
        }

        @Test
        @DisplayName("DELETE /api/reviews ‒ als USER → 403 Forbidden")
        @WithMockUser(username = "user", roles = {"USER"})
        void deleteAll_AsUser_ShouldReturn403() throws Exception {
            mockMvc.perform(delete("/api/reviews"))
                    .andExpect(status().isForbidden());
            verify(reviewService, never()).deleteAllReviews();
        }

        @Test
        @DisplayName("DELETE /api/reviews ‒ anonym → 401")
        void deleteAll_Anonymous_ShouldReturn401() throws Exception {
            mockMvc.perform(delete("/api/reviews"))
                    .andExpect(status().isUnauthorized());
            verify(reviewService, never()).deleteAllReviews();
        }
    }
}
