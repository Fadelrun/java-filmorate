package ru.yandex.practicum.filmorate.model;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import(UserDbStorage.class)
class UserDbStorageTest {

    private final UserDbStorage userStorage;

    private User makeUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    @Test
    @DisplayName("save: сохраняет пользователя и присваивает ID")
    void saveShouldAssignId() {
        User user = makeUser("test@mail.ru", "testuser");

        User saved = userStorage.save(user);

        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getEmail()).isEqualTo("test@mail.ru");
        assertThat(saved.getLogin()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("findById: находит существующего пользователя")
    void findByIdShouldFindExisting() {
        User saved = userStorage.save(makeUser("a@mail.ru", "anya"));

        User found = userStorage.findById(saved.getId());

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getEmail()).isEqualTo("a@mail.ru");
        assertThat(found.getLogin()).isEqualTo("anya");
    }

    @Test
    @DisplayName("findById: несуществующий ID → NotFoundException")
    void findByIdShouldThrowForUnknown() {
        assertThatThrownBy(() -> userStorage.findById(9999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    @DisplayName("findAll: возвращает всех пользователей")
    void findAllShouldReturnAll() {
        userStorage.save(makeUser("a@mail.ru", "a"));
        userStorage.save(makeUser("b@mail.ru", "b"));
        userStorage.save(makeUser("c@mail.ru", "c"));

        List<User> users = userStorage.findAll();

        assertThat(users).hasSize(3);
    }

    @Test
    @DisplayName("update: обновляет данные пользователя")
    void updateShouldChangeData() {
        User saved = userStorage.save(makeUser("old@mail.ru", "old"));

        saved.setEmail("new@mail.ru");
        saved.setName("Новое имя");
        userStorage.update(saved);

        User updated = userStorage.findById(saved.getId());
        assertThat(updated.getEmail()).isEqualTo("new@mail.ru");
        assertThat(updated.getName()).isEqualTo("Новое имя");
    }

    @Test
    @DisplayName("delete: удаляет пользователя")
    void deleteShouldRemoveUser() {
        User saved = userStorage.save(makeUser("del@mail.ru", "del"));

        userStorage.delete(saved.getId());

        assertThat(userStorage.existsById(saved.getId())).isFalse();
    }

    @Test
    @DisplayName("existsById: true для существующего, false для несуществующего")
    void existsByIdShouldWork() {
        User saved = userStorage.save(makeUser("exist@mail.ru", "exist"));

        assertThat(userStorage.existsById(saved.getId())).isTrue();
        assertThat(userStorage.existsById(9999)).isFalse();
    }
}