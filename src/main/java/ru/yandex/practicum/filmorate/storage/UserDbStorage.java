package ru.yandex.practicum.filmorate.storage;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Primary
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<User> USER_ROW_MAPPER = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        return user;
    };

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> users = jdbcTemplate.query(sql, USER_ROW_MAPPER);

        if (users.isEmpty()) return users;

        String friendsSql = "SELECT user_id, friend_id FROM friendships";
        Map<Integer, Set<Integer>> friendsMap = new HashMap<>();
        jdbcTemplate.query(friendsSql, rs -> {
            friendsMap
                    .computeIfAbsent(rs.getInt("user_id"), k -> new HashSet<>())
                    .add(rs.getInt("friend_id"));
        });

        for (User user : users) {
            user.setFriends(friendsMap.getOrDefault(user.getId(), new HashSet<>()));
        }

        return users;
    }

    @Override
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        User user = jdbcTemplate.query(sql, USER_ROW_MAPPER, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + id + " не найден"));
        enrich(user);
        return user;
    }

    @Override
    @Transactional
    public User save(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {

            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));

            return ps;
        }, keyHolder);

        user.setId(keyHolder.getKey().intValue());

        return user;
    }

    @Transactional
    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql,
                user.getEmail(), user.getLogin(), user.getName(),
                Date.valueOf(user.getBirthday()), user.getId());

        if (rows == 0) {
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден");
        }

        clearFriends(user.getId());
        saveFriends(user.getId(), user.getFriends());

        return user;
    }

    @Override
    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    @Override
    public boolean existsById(int id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, id);

        return count != null && count > 0;
    }

    private Set<Integer> loadFriends(int userId) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ?";
        return new HashSet<>(jdbcTemplate.queryForList(sql, Integer.class, userId));
    }

    private void saveFriends(int userId, Set<Integer> friends) {
        if (friends == null || friends.isEmpty()) return;

        String sql = "INSERT INTO friendships (user_id, friend_id, status) VALUES (?, ?, 'CONFIRMED')";

        List<Object[]> batchArgs = friends.stream()
                .map(friendId -> new Object[]{userId, friendId})
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void clearFriends(int userId) {
        jdbcTemplate.update("DELETE FROM friendships WHERE user_id = ?", userId);
    }

    private void enrich(User user) {
        user.setFriends(loadFriends(user.getId()));
    }
}
