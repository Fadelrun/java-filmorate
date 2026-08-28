package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class FilmTest {
    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldCreateValidFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertTrue(violations.isEmpty(), "Валидный фильм не должен иметь ошибок");
        assertTrue(film.isValidReleaseDate(), "Дата релиза должна быть валидной");
    }

    @Test
    void shouldNotCreateFilmWithEmptyName() {
        Film film = new Film();
        film.setName("");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty(), "Пустое название должно вызывать ошибку");
        assertEquals(1, violations.size(), "Должна быть ровно одна ошибка");
        assertEquals("Название фильма не может быть пустым", violations.iterator().next().getMessage());
    }

    @Test
    void shouldNotCreateFilmWithDescriptionLongerThan200() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("a".repeat(201));
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty(), "Описание длиннее 200 символов должно вызывать ошибку");
        assertEquals(1, violations.size(), "Должна быть ровно одна ошибка");
        assertEquals("Описание не может превышать 200 символов",
                violations.iterator().next().getMessage());
    }

    @Test
    void shouldNotCreateFilmWithReleaseDateBefore1895() {
        Film film = new Film();
        film.setName("");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(1800, 1, 1));
        film.setDuration(120);

        assertFalse(film.isValidReleaseDate(), "Дата релиза ранее 1895 должна быть невалидной");
    }

    @Test
    void shouldNotCreateFilmWithNegativeDuration() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(-10);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty(), "Отрицательная продолжительность должна вызывать ошибку");
        assertEquals(1, violations.size(), "Должна быть ровно одна ошибка");
        assertEquals("Продолжительность должна быть положительным числом",
                violations.iterator().next().getMessage());
    }

    @Test
    void shouldCreateFilmWithExact200CharactersDescription() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("a".repeat(200));
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty(), "Описание из 200 символов должно быть валидным");
    }

    @Test
    void shouldCreateFilmWithReleaseDateOn1895_12_28() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setDuration(120);

        assertTrue(film.isValidReleaseDate(), "Дата релиза 28.12.1895 должна быть валидной");
    }
}
