package ru.yandex.practicum.filmorate.storage;

import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Primary
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<Film> FILM_ROW_MAPPER = (rs, rowNum) -> {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        int mpaId = rs.getInt("mpa_rating_id");
        if (!rs.wasNull()) {
            film.setMpa(new MpaRating(mpaId, null));
        }

        return film;
    };

    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public List<Film> findAll() {
        String sql = "SELECT * FROM films ORDER BY id";
        List<Film> films = jdbcTemplate.query(sql, FILM_ROW_MAPPER);

        if (films.isEmpty()) return films;

        String mpaSql = """
            SELECT f.id AS film_id, m.id AS mpa_id, m.name AS mpa_name
            FROM films f
            JOIN mpa_ratings m ON f.mpa_rating_id = m.id
            """;
        Map<Integer, MpaRating> mpaMap = new HashMap<>();
        jdbcTemplate.query(mpaSql, rs -> {
            mpaMap.put(
                    rs.getInt("film_id"),
                    new MpaRating(rs.getInt("mpa_id"), rs.getString("mpa_name"))
            );
        });

        String genresSql = """
            SELECT fg.film_id, g.id AS genre_id, g.name AS genre_name
            FROM film_genres fg
            JOIN genres g ON fg.genre_id = g.id
            ORDER BY fg.film_id, g.id
            """;
        Map<Integer, Set<Genre>> genresMap = new HashMap<>();
        jdbcTemplate.query(genresSql, rs -> {
            genresMap
                    .computeIfAbsent(rs.getInt("film_id"), k -> new HashSet<>())
                    .add(new Genre(rs.getInt("genre_id"), rs.getString("genre_name")));
        });

        String likesSql = "SELECT film_id, user_id FROM likes";
        Map<Integer, Set<Integer>> likesMap = new HashMap<>();
        jdbcTemplate.query(likesSql, rs -> {
            likesMap
                    .computeIfAbsent(rs.getInt("film_id"), k -> new HashSet<>())
                    .add(rs.getInt("user_id"));
        });

        for (Film film : films) {
            film.setMpa(mpaMap.get(film.getId()));
            film.setGenres(genresMap.getOrDefault(film.getId(), new HashSet<>()));
            film.setLikes(likesMap.getOrDefault(film.getId(), new HashSet<>()));
        }

        return films;
    }

    @Override
    public Film findById(int id) {
        String sql = "SELECT * FROM films WHERE id = ?";
        Film film = jdbcTemplate.query(sql, FILM_ROW_MAPPER, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден"));

        enrich(film);
        return film;
    }

    @Override
    @Transactional
    public Film save(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        Integer mpaId = film.getMpa() != null ? film.getMpa().getId() : null;

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());

            if (mpaId != null) ps.setInt(5, mpaId);
            else ps.setNull(5, java.sql.Types.INTEGER);

            return ps;
        }, keyHolder);

        film.setId(keyHolder.getKey().intValue());
        saveGenres(film);
        return film;
    }

    @Override
    @Transactional
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_rating_id = ? WHERE id = ?";
        Integer mpaId = film.getMpa() != null ? film.getMpa().getId() : null;

        int rows = jdbcTemplate.update(sql,
                film.getName(), film.getDescription(),
                Date.valueOf(film.getReleaseDate()), film.getDuration(),
                mpaId, film.getId());

        if (rows == 0) {
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден");
        }

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());

        saveGenres(film);

        clearLikes(film.getId());
        saveLikes(film.getId(), film.getLikes());

        return film;
    }

    @Override
    public void delete(int id) {
        jdbcTemplate.update("DELETE FROM films WHERE id = ?", id);
    }

    @Override
    public boolean existsById(int id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM films WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private void enrich(Film film) {
        if (film.getMpa() != null) {
            String mpaSql = "SELECT name FROM mpa_ratings WHERE id = ?";
            String mpaName = jdbcTemplate.queryForObject(mpaSql, String.class, film.getMpa().getId());
            film.getMpa().setName(mpaName);
        }

        String genresSql = """
                SELECT g.id, g.name
                FROM genres g
                JOIN film_genres fg ON g.id = fg.genre_id
                WHERE fg.film_id = ?
                ORDER BY g.id
                """;
        Set<Genre> genres = new HashSet<>(jdbcTemplate.query(genresSql,
                (rs, rowNum) -> new Genre(rs.getInt("id"), rs.getString("name")),
                film.getId()));

        film.setGenres(genres);

        String likesSql = "SELECT user_id FROM likes WHERE film_id = ?";
        Set<Integer> likes = new HashSet<>(jdbcTemplate.queryForList(likesSql, Integer.class, film.getId()));
        film.setLikes(likes);
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) return;

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        List<Object[]> batchArgs = film.getGenres().stream()
                .map(genre -> new Object[]{film.getId(), genre.getId()})
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void saveLikes(int filmId, Set<Integer> likes) {
        if (likes == null || likes.isEmpty()) return;

        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";

        List<Object[]> batchArgs = likes.stream()
                .map(userId -> new Object[]{filmId, userId})
                .collect(Collectors.toList());

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private void clearLikes(int filmId) {
        jdbcTemplate.update("DELETE FROM likes WHERE film_id = ?", filmId);
    }
}
