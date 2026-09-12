package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;
import java.util.List;

public interface UserStorage {
    List<User> findAll();
    User findById(int id);
    User save(User user);
    User update(User user);
    void delete(int id);
    boolean existsById(int id);
}