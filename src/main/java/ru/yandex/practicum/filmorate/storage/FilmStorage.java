package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {
    List<Film> findAll();

    Film findById(int id);

    Film save(Film film);

    Film update(Film film);

    void delete(int id);

    boolean existsById(int id);
}