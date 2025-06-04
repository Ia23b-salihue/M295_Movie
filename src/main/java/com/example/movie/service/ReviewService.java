package com.example.movie.service;

import com.example.movie.exception.MovieNotFoundException;
import com.example.movie.exception.ReviewNotFoundException;
import com.example.movie.model.Movie;
import com.example.movie.model.Review;
import com.example.movie.repository.MovieRepository;
import com.example.movie.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewService {

    private final Logger logger = LoggerFactory.getLogger(ReviewService.class);
    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;

    public ReviewService(ReviewRepository reviewRepository, MovieRepository movieRepository) {
        this.reviewRepository = reviewRepository;
        this.movieRepository = movieRepository;
    }

    public List<Review> getAllReviews() {
        logger.info("Get all reviews");
        return reviewRepository.findAll();
    }

    public List<Review> getReviewsByMovieId(Long movieId) {
        logger.info("Get reviews for movie ID: {}", movieId);
        return reviewRepository.findByMovieId(movieId);
    }

    public Optional<Review> getReviewById(Long id) {
        logger.info("Get review by ID: {}", id);
        return reviewRepository.findById(id);
    }

    private void attachAndValidateMovie(Review review) {
        if (review.getMovie() == null || review.getMovie().getId() == 0) {
            throw new IllegalArgumentException("Review must be linked to a movie");
        }
        Long movieId = review.getMovie().getId();
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(movieId));
        review.setMovie(movie);
    }

    public Review createReview(Review review) {
        logger.info("Create review by user: {}", review.getUsername());
        attachAndValidateMovie(review);
        return reviewRepository.save(review);
    }

    public List<Review> createReviewsBatch(List<Review> reviews) {
        logger.info("Create batch reviews, count: {}", reviews.size());
        for (Review review : reviews) {
            attachAndValidateMovie(review);
        }
        return reviewRepository.saveAll(reviews);
    }

    public Review updateReview(Long id, Review updatedReview) {
        logger.info("Update review with ID: {}", id);
        return reviewRepository.findById(id).map(review -> {
            review.setUsername(updatedReview.getUsername());
            review.setComment(updatedReview.getComment());
            review.setRating(updatedReview.getRating());
            if (updatedReview.getMovie() != null) {
                attachAndValidateMovie(updatedReview);
                review.setMovie(updatedReview.getMovie());
            }
            return reviewRepository.save(review);
        }).orElseThrow(() -> new ReviewNotFoundException(id));
    }

    public void deleteReviewById(Long id) {
        logger.info("Delete review with ID: {}", id);
        if (!reviewRepository.existsById(id)) {
            throw new ReviewNotFoundException(id);
        }
        reviewRepository.deleteById(id);
    }

    public void deleteAllReviews() {
        logger.info("Delete all reviews");
        reviewRepository.deleteAll();
    }
}
