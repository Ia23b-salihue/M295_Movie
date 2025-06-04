package com.example.movie.repository;

import com.example.movie.model.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {

    List<Movie> findByRecommended(boolean recommended);

    List<Movie> findByGenreContainingIgnoreCase(String genre);

    void deleteByReleaseDateBefore(LocalDate date);
}
