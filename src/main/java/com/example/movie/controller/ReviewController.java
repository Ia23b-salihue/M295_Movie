package com.example.movie.controller;

import com.example.movie.model.Review;
import com.example.movie.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Review API", description = "CRUD operations for reviews")
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    @Operation(summary = "Get all reviews or filter by movieId")
    public List<Review> getAll(@RequestParam(required = false) Long movieId) {
        if (movieId != null) {
            return reviewService.getReviewsByMovieId(movieId);
        }
        return reviewService.getAllReviews();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get review by ID")
    public ResponseEntity<Review> getById(@PathVariable Long id) {
        return reviewService.getReviewById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new review")
    public ResponseEntity<?> create(@Valid @RequestBody Review review) {
        try {
            Review created = reviewService.createReview(review);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/batch")
    @Operation(summary = "Create multiple reviews")
    public ResponseEntity<?> createBatch(@Valid @RequestBody List<Review> reviews) {
        try {
            List<Review> created = reviewService.createReviewsBatch(reviews);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update review by ID")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody Review review) {
        try {
            Review updated = reviewService.updateReview(id, review);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete review by ID")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        try {
            reviewService.deleteReviewById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping
    @Operation(summary = "Delete all reviews")
    public ResponseEntity<Void> deleteAll() {
        reviewService.deleteAllReviews();
        return ResponseEntity.noContent().build();
    }
}
