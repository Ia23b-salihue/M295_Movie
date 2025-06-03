package com.example.movie.controller;

import com.example.movie.model.Movie;
import com.example.movie.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/movies")
@Tag(name = "Movie API", description = "CRUD-Operationen für Filme")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    @Operation(summary = "Alle Filme lesen")
    public List<Movie> getAll() {
        return movieService.getAllMovies();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Film mit ID lesen")
    public ResponseEntity<Movie> getById(@PathVariable Long id) {
        return movieService.getMovieById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/exists/{id}")
    @Operation(summary = "Prüfen ob Film mit ID existiert")
    public ResponseEntity<Boolean> existsById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.existsById(id));
    }

    @GetMapping("/filter/recommended")
    @Operation(summary = "Filme nach Empfehlung (boolean) filtern")
    public List<Movie> getByRecommended(@RequestParam boolean recommended) {
        return movieService.getMoviesByRecommended(recommended);
    }

    @GetMapping("/filter/genre")
    @Operation(summary = "Filme nach Genre (Text) filtern")
    public List<Movie> getByGenre(@RequestParam String genre) {
        return movieService.getMoviesByGenre(genre);
    }

    @PostMapping
    @Operation(summary = "Einen neuen Film erstellen")
    public ResponseEntity<Movie> create(@Valid @RequestBody Movie movie) {
        Movie created = movieService.createMovie(movie);
        return ResponseEntity.ok(created);
    }

    @PostMapping("/batch")
    @Operation(summary = "Mehrere Filme erstellen")
    public List<Movie> createBatch(@Valid @RequestBody List<Movie> movies) {
        return movieService.createMovies(movies);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Film mit ID aktualisieren")
    public ResponseEntity<Movie> update(@PathVariable Long id, @Valid @RequestBody Movie movie) {
        try {
            Movie updated = movieService.updateMovie(id, movie);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Film mit ID löschen")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        if (!movieService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        movieService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/filter/releaseDate")
    @Operation(summary = "Filme vor einem Datum löschen")
    public ResponseEntity<Void> deleteByReleaseDateBefore(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        movieService.deleteByReleaseDateBefore(date);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Alle Filme löschen")
    public ResponseEntity<Void> deleteAll() {
        movieService.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
