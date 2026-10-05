package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(GenreDbStorage.class)
class GenreDbStorageTest {

    private final GenreDbStorage genreStorage;

    @Test
    @DisplayName("findAll: возвращает 6 жанров из справочника")
    void findAllShouldReturnAll() {
        List<Genre> genres = genreStorage.findAll();
        assertThat(genres).hasSize(6);
        assertThat(genres.get(0).getName()).isEqualTo("Комедия");
    }

    @Test
    @DisplayName("findById: находит существующий жанр")
    void findByIdShouldFind() {
        Genre genre = genreStorage.findById(1);
        assertThat(genre.getId()).isEqualTo(1);
        assertThat(genre.getName()).isEqualTo("Комедия");
    }

    @Test
    @DisplayName("findById: несуществующий ID → NotFoundException")
    void findByIdShouldThrow() {
        assertThatThrownBy(() -> genreStorage.findById(999))
                .isInstanceOf(NotFoundException.class);
    }
}