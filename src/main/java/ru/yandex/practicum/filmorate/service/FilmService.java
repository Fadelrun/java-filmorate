package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private static final int DEFAULT_POPULAR_COUNT = 10;

    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public List<Film> getAllFilms() {
        log.info("Получение всех фильмов");
        return filmStorage.findAll();
    }

    public Film getFilmById(int id) {
        log.info("Получение фильма с ID: {}", id);
        return filmStorage.findById(id);
    }

    public Film createFilm(Film film){
        log.info("Создание фильма: {}", film.getName());
        validateReleaseDate(film);
        return filmStorage.save(film);
    }

    public Film updateFilm(Film film) {
        log.info("Обновление фильма с ID: {}", film.getId());

        if (film.getId() == 0) {
            log.warn("ID фильма не указан");
            throw new ValidationException("ID фильма должен быть указан");
        }

        validateReleaseDate(film);
        return filmStorage.update(film);
    }

    public void addLike(int filmId, int userId) {
        log.info("Пользователь {} ставит лайк фильму {}", userId, filmId);
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        Film film = filmStorage.findById(filmId);
        film.addLike(userId);

        filmStorage.update(film);

        log.debug("Лайк добавлен. Всего лайков у фильма {}: {}",
                filmId, film.getLikesCount());
    }

    public void removeLike(int filmId, int userId) {
        log.info("Пользователь {} убирает лайк с фильма {}", userId, filmId);

        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        Film film = filmStorage.findById(filmId);
        film.removeLike(userId);

        filmStorage.update(film);

        log.debug("Лайк удален. Всего лайков у фильма {}: {}",
                filmId, film.getLikesCount());
    }

    public List<Film> getPopularFilms(int count) {
        log.info("Получение {} популярных фильмов", count);

        if (count <= 0) {
            count = DEFAULT_POPULAR_COUNT;
        }

        final int limit = count;

        return filmStorage.findAll().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikesCount(), f1.getLikesCount()))
                .limit(limit)
                .collect(Collectors.toList());
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
