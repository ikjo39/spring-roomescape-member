package roomescape.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.domain.reservation.Reservation;
import roomescape.domain.reservation.ReservationDate;
import roomescape.domain.reservation.ReservationName;
import roomescape.domain.reservationtime.ReservationStartAt;
import roomescape.domain.reservationtime.ReservationTime;
import roomescape.domain.theme.Theme;
import roomescape.domain.theme.ThemeDescription;
import roomescape.domain.theme.ThemeName;
import roomescape.domain.theme.ThemeThumbnail;

@Repository
public class JdbcReservationDao implements ReservationDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReservationDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Reservation> readAll() {
        String sql = """
                SELECT
                    r.id AS reservation_id,
                    r.name,
                    r.`date`,
                    t.id AS time_id,
                    t.start_at AS time_value,
                    th.id AS theme_id,
                    th.name AS theme_name,
                    th.description AS theme_description,
                    th.thumbnail AS theme_thumbnail
                FROM
                    reservation r
                INNER JOIN
                    reservation_time t ON r.time_id = t.id
                INNER JOIN
                    theme th ON r.theme_id = th.id;
                """;
        return jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> getReservation(resultSet, getReservationTime(resultSet), getTheme(resultSet))
        );
    }

    @Override
    public Optional<Reservation> readById(Long id) {
        String sql = """
                SELECT
                    r.id AS reservation_id,
                    r.name,
                    r.`date`,
                    t.id AS time_id,
                    t.start_at AS time_value,
                    th.id AS theme_id,
                    th.name AS theme_name,
                    th.description AS theme_description,
                    th.thumbnail AS theme_thumbnail
                FROM
                    reservation r
                INNER JOIN
                    reservation_time t ON r.time_id = t.id
                INNER JOIN
                    theme th ON r.theme_id = th.id
                WHERE r.id = ?
                """;
        List<Reservation> reservations = jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> getReservation(resultSet, getReservationTime(resultSet), getTheme(resultSet)),
                id
        );
        if (reservations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(reservations.get(0));
    }

    @Override
    public List<ReservationTime> readTimesByDateAndThemeId(ReservationDate reservationDate, Long themeId) {
        String sql = """
                SELECT
                    t.id AS time_id,
                    t.start_at AS time_value,
                FROM
                    reservation r
                INNER JOIN
                    reservation_time t ON r.time_id = t.id
                WHERE r.`date` = ? AND r.theme_id = ?
                """;
        return jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> getReservationTime(resultSet),
                reservationDate.toStringDate(),
                themeId
        );
    }

    @Override
    public List<Theme> readPopularThemes(LocalDate startDate, LocalDate endDate) {
        String sql = """
                SELECT
                    th.id AS theme_id,
                    th.name AS theme_name,
                    th.description AS theme_description,
                    th.thumbnail AS theme_thumbnail
                FROM
                    reservation r
                INNER JOIN
                    theme th ON r.theme_id = th.id
                WHERE r.`date` BETWEEN ? AND ?
                GROUP BY theme_id
                ORDER BY COUNT(*) DESC, theme_id
                LIMIT 10;
                """;
        return jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> getTheme(resultSet),
                startDate,
                endDate
        );
    }

    @Override
    public Reservation create(Reservation reservation) {
        String sql = """
                INSERT
                INTO reservation
                    (name, date, time_id, theme_id)
                VALUES
                    (?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, reservation.getName().getValue());
                    ps.setDate(2, Date.valueOf(reservation.getDate().getValue()));
                    ps.setLong(3, reservation.getReservationTime().getId());
                    ps.setLong(4, reservation.getTheme().getId());
                    return ps;
                },
                keyHolder
        );
        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return new Reservation(
                id,
                reservation.getName(),
                reservation.getDate(),
                reservation.getReservationTime(),
                reservation.getTheme()
        );
    }

    @Override
    public boolean hasSame(Reservation reservation) {
        String sql = """
                SELECT
                CASE
                    WHEN EXISTS (SELECT 1 FROM reservation WHERE date = ? AND time_id = ? AND theme_id = ?)
                    THEN TRUE
                    ELSE FALSE
                END
                """;
        return jdbcTemplate.queryForObject(
                sql,
                boolean.class,
                reservation.getDate().toStringDate(),
                reservation.getReservationTime().getId(),
                reservation.getTheme().getId()
        );
    }

    @Override
    public void delete(Reservation reservation) {
        String sql = """
                DELETE
                FROM reservation
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, reservation.getId());
    }

    @Override
    public boolean existByTimeId(Long timeId) {
        String sql = """
                SELECT
                CASE
                    WHEN EXISTS (SELECT 1 FROM reservation WHERE time_id = ?)
                    THEN TRUE
                    ELSE FALSE
                END
                """;
        return jdbcTemplate.queryForObject(sql, boolean.class, timeId);
    }

    @Override
    public boolean existByThemeId(Long themeId) {
        String sql = """
                SELECT
                CASE
                    WHEN EXISTS (SELECT 1 FROM reservation WHERE theme_id = ?)
                    THEN TRUE
                    ELSE FALSE
                END
                """;
        return jdbcTemplate.queryForObject(sql, boolean.class, themeId);
    }

    private Reservation getReservation(ResultSet resultSet, ReservationTime reservationTime, Theme theme)
            throws SQLException {
        return new Reservation(
                resultSet.getLong("id"),
                new ReservationName(resultSet.getString("name")),
                ReservationDate.from(resultSet.getString("date")),
                reservationTime,
                theme
        );
    }

    private ReservationTime getReservationTime(ResultSet resultSet) throws SQLException {
        return new ReservationTime(
                resultSet.getLong("time_id"),
                ReservationStartAt.from(resultSet.getString("time_value"))
        );
    }

    private Theme getTheme(ResultSet resultSet) throws SQLException {
        return new Theme(
                resultSet.getLong("theme_id"),
                ThemeName.from(resultSet.getString("theme_name")),
                ThemeDescription.from(resultSet.getString("theme_description")),
                ThemeThumbnail.from(resultSet.getString("theme_thumbnail"))
        );
    }
}
