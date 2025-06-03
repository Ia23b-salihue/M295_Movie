package com.example.movie.service;

import com.example.movie.model.Movie;
import com.example.movie.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MovieService {

    private final Logger logger = LoggerFactory.getLogger(MovieService.class);
    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<Movie> getAllMovies() {
        logger.info("Lese alle Filme");
        return movieRepository.findAll();
    }

    public Optional<Movie> getMovieById(Long id) {
        logger.info("Lese Film mit ID: {}", id);
        return movieRepository.findById(id);
    }

    public boolean existsById(Long id) {
        logger.info("Prüfe Existenz Film mit ID: {}", id);
        return movieRepository.existsById(id);
    }

    public List<Movie> getMoviesByRecommended(boolean recommended) {
        logger.info("Lese Filme mit recommended={}", recommended);
        return movieRepository.findByRecommended(recommended);
    }

    public List<Movie> getMoviesByGenre(String genre) {
        logger.info("Lese Filme mit Genre enthält: {}", genre);
        return movieRepository.findByGenreContainingIgnoreCase(genre);
    }

    public Movie createMovie(Movie movie) {
        logger.info("Erstelle neuen Film: {}", movie.getTitle());
        // Verbundene Reviews werden durch Cascade gespeichert
        return movieRepository.save(movie);
    }

    public List<Movie> createMovies(List<Movie> movies) {
        logger.info("Erstelle mehrere Filme: Anzahl={}", movies.size());
        return movieRepository.saveAll(movies);
    }

    public Movie updateMovie(Long id, Movie updatedMovie) {
        logger.info("Aktualisiere Film mit ID: {}", id);
        return movieRepository.findById(id).map(movie -> {
            movie.setTitle(updatedMovie.getTitle());
            movie.setGenre(updatedMovie.getGenre());
            movie.setReleaseDate(updatedMovie.getReleaseDate());
            movie.setAgeRating(updatedMovie.getAgeRating());
            movie.setAverageRating(updatedMovie.getAverageRating());
            movie.setRecommended(updatedMovie.isRecommended());

            // Reviews aktualisieren
            movie.getReviews().clear();
            if (updatedMovie.getReviews() != null) {
                updatedMovie.getReviews().forEach(review -> review.setMovie(movie));
                movie.getReviews().addAll(updatedMovie.getReviews());
            }

            return movieRepository.save(movie);
        }).orElseThrow(() -> new RuntimeException("Film nicht gefunden mit ID " + id));
    }

    public void deleteById(Long id) {
        logger.info("Lösche Film mit ID: {}", id);
        movieRepository.deleteById(id);
    }

    public void deleteByReleaseDateBefore(LocalDate date) {
        logger.info("Lösche Filme vor Datum: {}", date);
        movieRepository.deleteByReleaseDateBefore(date);
    }

    public void deleteAll() {
        logger.info("Lösche alle Filme");
        movieRepository.deleteAll();
    }
}
