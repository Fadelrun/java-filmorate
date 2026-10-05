package ru.yandex.practicum.filmorate.storage;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;

@Repository
public class MpaDbStorage implements MpaStorage {
    private final JdbcTemplate jdbcTemplate;
    private static final RowMapper<MpaRating> MPA_ROW_MAPPER = (rs, rowNum) ->
            new MpaRating(rs.getInt("id"), rs.getString("name"));

    public MpaDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<MpaRating> findAll() {
        String sql = "SELECT id, name FROM mpa_ratings ORDER BY id";
        return jdbcTemplate.query(sql, MPA_ROW_MAPPER);
    }

    @Override
    public MpaRating findById(int id) {
        String sql = "SELECT id, name FROM mpa_ratings WHERE id = ?";
        return jdbcTemplate.query(sql, MPA_ROW_MAPPER, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с ID " + id + " не найден"));
    }

    @Override
    public boolean existsById(int id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mpa_ratings WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }
}
