package com.example.movie.controller;

import com.example.movie.model.Movie;
import com.example.movie.model.Review;
import com.example.movie.service.ReviewService;
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
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

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

    private String toJson(Review r) throws Exception {
        return objectMapper.writeValueAsString(r);
    }

    @Nested
    @DisplayName("GET‐Endpunkte")
    class GetEndpoints {

        @Test
        @DisplayName("GET /api/reviews ‒ liefert 200 + Liste")
        void getAll_ShouldReturn200AndList() throws Exception {
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
        }

        @Test
        @DisplayName("GET /api/reviews?movieId=10 ‒ liefert gefilterte Liste")
        void getAll_FilterByMovieId_ShouldReturnList() throws Exception {
            List<Review> filtered = Arrays.asList(createSampleReview(3L, 10L));
            when(reviewService.getReviewsByMovieId(10L)).thenReturn(filtered);

            mockMvc.perform(get("/api/reviews")
                            .param("movieId", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(3)))
                    .andExpect(jsonPath("$[0].rating", is(8)));
        }

        @Test
        @DisplayName("GET /api/reviews/{id} ‒ existent → 200 + Review")
        void getById_Exists_ShouldReturn200() throws Exception {
            Review r = createSampleReview(5L, 20L);
            when(reviewService.getReviewById(5L)).thenReturn(Optional.of(r));

            mockMvc.perform(get("/api/reviews/5"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(5)))
                    .andExpect(jsonPath("$.username", is("testuser")))
                    .andExpect(jsonPath("$.rating", is(8)));
        }

        @Test
        @DisplayName("GET /api/reviews/{id} ‒ nicht existent → 404")
        void getById_NotFound_ShouldReturn404() throws Exception {
            when(reviewService.getReviewById(42L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/reviews/42"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST‐Endpunkte")
    class PostEndpoints {

        @Test
        @DisplayName("POST /api/reviews ‒ erstellt neues Review, liefert 200 + Review")
        void create_ShouldReturnCreatedReview() throws Exception {
            Review r = createSampleReview(7L, 30L);
            String payload = toJson(r);
            when(reviewService.createReview(any(Review.class))).thenReturn(r);

            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(7)))
                    .andExpect(jsonPath("$.username", is("testuser")))
                    .andExpect(jsonPath("$.rating", is(8)));
        }

        @Test
        @DisplayName("POST /api/reviews/batch ‒ erstellt mehrere Reviews, liefert 200 + Liste")
        void createBatch_ShouldReturnList() throws Exception {
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


        @Nested
        @DisplayName("PUT‐Endpunkt")
        class PutEndpoint {

            @Test
            @DisplayName("PUT /api/reviews/{id} ‒ existent → 200 + aktualisiertes Review")
            void update_Exists_ShouldReturnUpdatedReview() throws Exception {
                Review r = createSampleReview(3L, 50L);
                String payload = toJson(r);
                when(reviewService.updateReview(eq(3L), any(Review.class))).thenReturn(r);

                mockMvc.perform(put("/api/reviews/3")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                        .andExpect(jsonPath("$.id", is(3)))
                        .andExpect(jsonPath("$.comment", is("Great movie!")))
                        .andExpect(jsonPath("$.rating", is(8)));
            }

            @Test
            @DisplayName("PUT /api/reviews/{id} ‒ nicht existent → 404")
            void update_NotFound_ShouldReturn404() throws Exception {
                Review r = createSampleReview(99L, 60L);
                String payload = toJson(r);
                when(reviewService.updateReview(eq(99L), any(Review.class)))
                        .thenThrow(new RuntimeException("nicht gefunden"));

                mockMvc.perform(put("/api/reviews/99")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isNotFound());
            }
        }

        @Nested
        @DisplayName("DELETE‐Endpoints")
        class DeleteEndpoints {

            @Test
            @DisplayName("DELETE /api/reviews/{id} ‒ existent → 204 No Content")
            void deleteById_Exists_ShouldReturn204() throws Exception {
                doNothing().when(reviewService).deleteReviewById(15L);

                mockMvc.perform(delete("/api/reviews/15"))
                        .andExpect(status().isNoContent());
            }

            @Test
            @DisplayName("DELETE /api/reviews/{id} ‒ nicht existent → 404")
            void deleteById_NotFound_ShouldReturn404() throws Exception {
                doThrow(new RuntimeException("nicht gefunden"))
                        .when(reviewService).deleteReviewById(99L);

                mockMvc.perform(delete("/api/reviews/99"))
                        .andExpect(status().isNotFound());
            }


            @Test
            @DisplayName("DELETE /api/reviews ‒ löscht alle Reviews, liefert 204")
            void deleteAll_ShouldReturn204() throws Exception {
                doNothing().when(reviewService).deleteAllReviews();

                mockMvc.perform(delete("/api/reviews"))
                        .andExpect(status().isNoContent());
            }
        }
    }
}