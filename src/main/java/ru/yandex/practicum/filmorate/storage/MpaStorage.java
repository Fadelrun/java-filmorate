package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;

public interface MpaStorage {
    List<MpaRating> findAll();

    MpaRating findById(int id);
}
