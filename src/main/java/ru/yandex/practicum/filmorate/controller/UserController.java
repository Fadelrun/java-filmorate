package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {
    private final List<User> users = new ArrayList<>();
    private int nextId = 1;

    @GetMapping
    public List<User> getAllUsers() {
        log.info("GET /users - Получение всех пользователей. Количество: {}", users.size());
        return users;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public User createUser(@Valid @RequestBody User user) {
        log.info("POST /users - Создание пользователя: {}", user.getLogin());

        validateBirthday(user);

        setNameIfEmpty(user);

        user.setId(nextId++);
        users.add(user);

        log.info("Пользователь создан с ID: {}", user.getId());
        return user;
    }

    @PutMapping
    public User updateUser(@Valid @RequestBody User user) {
        log.info("PUT /users - Обновление пользователя с ID: {}", user.getId());

        if (user.getId() == 0) {
            log.warn("ID пользователя не указан");
            throw new ValidationException("ID пользователя должен быть указан");
        }

        validateBirthday(user);

        User existingUser = users.stream()
                .filter(u -> u.getId() == user.getId())
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Пользователь с ID {} не найден", user.getId());
                    return new NotFoundException("Пользователь с указанным ID не найден");
                });

        setNameIfEmpty(user);

        existingUser.setEmail(user.getEmail());
        existingUser.setLogin(user.getLogin());
        existingUser.setName(user.getName());
        existingUser.setBirthday(user.getBirthday());

        log.info("Пользователь с ID {} обновлен", user.getId());
        return existingUser;
    }

    private void validateBirthday(User user) {
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Некорректная дата рождения: {}", user.getBirthday());
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void setNameIfEmpty(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя заменено на логин: {}", user.getLogin());
        }
    }
}
