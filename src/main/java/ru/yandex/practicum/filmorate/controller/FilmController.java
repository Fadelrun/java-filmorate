package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {
    private final List<Film> films = new ArrayList<>();
    private int nextId = 1;

    @GetMapping
    public List<Film> getAllFilms() {
        log.info("GET /films - Получение всех фильмов. Количество: {}", films.size());
        return films;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film createFilm(@Valid @RequestBody Film film) {
        log.info("POST /films - Создание фильма: {}", film.getName());
        validateReleaseDate(film);

        film.setId(nextId++);
        films.add(film);

        log.info("Фильм создан с ID: {}", film.getId());

        return film;
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        log.info("PUT /films - Обновление фильма с ID: {}", film.getId());

        if (film.getId() == 0) {
            log.warn("ID фильма не указан");
            throw new ValidationException("ID фильма должен быть указан");
        }

        validateReleaseDate(film);

        Film existingFilm = films.stream()
                .filter(f -> f.getId() == film.getId())
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Фильм с ID {} не найден", film.getId());
                    return new NotFoundException("Фильм с указанным ID не найден");
                });

        existingFilm.setName(film.getName());
        existingFilm.setDescription(film.getDescription());
        existingFilm.setReleaseDate(film.getReleaseDate());
        existingFilm.setDuration(film.getDuration());

        log.info("Фильм с ID {} обновлен", film.getId());

        return existingFilm;
    }

    private void validateReleaseDate(Film film) {
        if (!film.isValidReleaseDate()) {
            log.warn("Некорректная дата релиза: {}", film.getReleaseDate());
            throw new ValidationException(
                    "Дата релиза не может быть ранее " + Film.MIN_RELEASE_DATE
            );
        }
    }
}
