package ru.yandex.practicum.filmorate.model;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.storage.FilmDbStorage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(FilmDbStorage.class)
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;

    private Film makeFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание " + name);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new MpaRating(1, null));  // id=1 (G) из schema.sql
        return film;
    }

    @Test
    @DisplayName("save: сохраняет фильм с присвоенным ID")
    void saveShouldAssignId() {
        Film saved = filmStorage.save(makeFilm("Интерстеллар"));

        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getName()).isEqualTo("Интерстеллар");
    }

    @Test
    @DisplayName("findById: находит фильм с MPA и жанрами")
    void findByIdShouldEnrich() {
        Film film = makeFilm("Матрица");

        Set<Genre> genres = new HashSet<>();
        genres.add(new Genre(2, null));  // Драма
        genres.add(new Genre(4, null));  // Триллер
        film.setGenres(genres);

        Film saved = filmStorage.save(film);
        Film found = filmStorage.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getMpa()).isNotNull();
        assertThat(found.getMpa().getName()).isEqualTo("G");  // id=1 → G
        assertThat(found.getGenres()).hasSize(2);
    }

    @Test
    @DisplayName("findById: несуществующий ID → NotFoundException")
    void findByIdShouldThrowForUnknown() {
        assertThatThrownBy(() -> filmStorage.findById(9999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("findAll: возвращает все фильмы")
    void findAllShouldReturnAll() {
        filmStorage.save(makeFilm("Film 1"));
        filmStorage.save(makeFilm("Film 2"));

        List<Film> films = filmStorage.findAll();

        assertThat(films).hasSize(2);
    }

    @Test
    @DisplayName("update: обновляет фильм и его жанры")
    void updateShouldChangeGenres() {
        Film film = makeFilm("Test");

        Set<Genre> genres = new HashSet<>();
        genres.add(new Genre(2, null));
        film.setGenres(genres);

        Film saved = filmStorage.save(film);
        assertThat(filmStorage.findById(saved.getId()).getGenres()).hasSize(1);

        Set<Genre> newGenres = new HashSet<>();
        newGenres.add(new Genre(3, null));  // Мультфильм
        saved.setGenres(newGenres);
        filmStorage.update(saved);

        Film updated = filmStorage.findById(saved.getId());
        assertThat(updated.getGenres()).hasSize(1);
        assertThat(updated.getGenres().iterator().next().getId()).isEqualTo(3);
    }

    @Test
    @DisplayName("delete: удаляет фильм")
    void deleteShouldRemove() {
        Film saved = filmStorage.save(makeFilm("Delete me"));

        filmStorage.delete(saved.getId());

        assertThat(filmStorage.existsById(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("existsById: работает корректно")
    void existsByIdShouldWork() {
        Film saved = filmStorage.save(makeFilm("Exist"));

        assertThat(filmStorage.existsById(saved.getId())).isTrue();
        assertThat(filmStorage.existsById(9999)).isFalse();
    }
}