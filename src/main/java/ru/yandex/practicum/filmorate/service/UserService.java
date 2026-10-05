package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class UserService {
    private final UserStorage userStorage;

    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public List<User> getAllUsers() {
        log.info("Получение всех пользователей");
        return userStorage.findAll();
    }

    public User getUserById(int id) {
        log.info("Получение пользователя с ID: {}", id);
        return userStorage.findById(id);
    }

    public User createUser(User user) {
        log.info("Создание пользователя: {}", user.getLogin());
        validateBirthday(user);
        setNameIfEmpty(user);
        return userStorage.save(user);
    }

    public User updateUser(User user) {
        log.info("Обновление пользователя с ID: {}", user.getId());

        if (user.getId() == 0) {
            throw new ValidationException("ID пользователя должен быть указан");
        }

        validateBirthday(user);
        setNameIfEmpty(user);
        return userStorage.update(user);
    }

    public void addFriend(int userId, int friendId) {
        log.info("Пользователь {} добавляет в друзья {}", userId, friendId);

        if (userId == friendId) {
            log.warn("Пользователь {} пытается добавить самого себя", userId);
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }

        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        user.addFriend(friendId);
        friend.addFriend(userId);

        userStorage.update(user);
        userStorage.update(friend);

        log.debug("Пользователи {} и {} теперь друзья", userId, friendId);
    }

    public void removeFriend(int userId, int friendId) {
        log.info("Пользователь {} удаляет из друзей {}", userId, friendId);

        User user = userStorage.findById(userId);
        User friend = userStorage.findById(friendId);

        user.removeFriend(friendId);
        friend.removeFriend(userId);

        userStorage.update(user);
        userStorage.update(friend);
    }

    public List<User> getFriends(int userId) {
        log.info("Получение друзей пользователя {}", userId);
        User user = userStorage.findById(userId);
        List<User> friends = new ArrayList<>();

        for (int friendId : user.getFriends()) {
            friends.add(userStorage.findById(friendId));
        }

        return friends;
    }

    public List<User> getCommonFriends(int userId, int otherId) {
        log.info("Поиск общих друзей у {} и {}", userId, otherId);
        User user = userStorage.findById(userId);
        User other = userStorage.findById(otherId);

        Set<Integer> commonIds = new HashSet<>(user.getFriends());

        commonIds.retainAll(other.getFriends());

        List<User> commonFriends = new ArrayList<>();

        for (int friendId : commonIds) {
            commonFriends.add(userStorage.findById(friendId));
        }

        return commonFriends;
    }

    private void validateBirthday(User user) {
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    private void setNameIfEmpty(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
