package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

@Repository
public class GenreDbStorage implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Genre> GENRE_ROW_MAPPER = (rs, rowNum) ->
            new Genre(rs.getInt("id"), rs.getString("name"));

    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Genre> findAll() {
        String sql = """
                SELECT id, name
                FROM genres
                ORDER BY id
                """;
        return jdbcTemplate.query(sql, GENRE_ROW_MAPPER);
    }

    @Override
    public Genre findById(int id) {
        String sql = """
                SELECT id, name
                FROM genres
                WHERE id = ?
                """;
        return jdbcTemplate.query(sql, GENRE_ROW_MAPPER, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Жанр с ID " + id + " не найден"));
    }

    @Override
    public boolean existsById(int id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM genres WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }
}
