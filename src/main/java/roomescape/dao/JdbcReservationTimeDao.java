package roomescape.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import roomescape.domain.reservationtime.ReservationStartAt;
import roomescape.domain.reservationtime.ReservationTime;

@Repository
public class JdbcReservationTimeDao implements ReservationTimeDao {

    private final JdbcTemplate jdbcTemplate;

    public JdbcReservationTimeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ReservationTime> readAll() {
        String sql = """
                SELECT
                id, start_at
                FROM reservation_time
                """;
        return jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> getReservationTime(resultSet)
        );
    }

    @Override
    public Optional<ReservationTime> readById(long id) {
        String sql = """
                SELECT
                id, start_at
                FROM reservation_time
                WHERE id = ?
                """;
        List<ReservationTime> reservationTimes = jdbcTemplate.query(
                sql,
                (resultSet, rowNum) -> getReservationTime(resultSet),
                id
        );
        if (reservationTimes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(reservationTimes.get(0));
    }

    @Override
    public ReservationTime create(ReservationTime reservationTime) {
        String sql = """
                INSERT
                INTO reservation_time
                    (start_at)
                VALUES
                    (?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    ps.setTime(1, Time.valueOf(reservationTime.getStartAt().getValue()));
                    return ps;
                },
                keyHolder
        );
        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return new ReservationTime(id, reservationTime.getStartAt());
    }

    @Override
    public boolean exist(long id) {
        String sql = """
                SELECT
                CASE
                    WHEN EXISTS (SELECT 1 FROM reservation_time WHERE id = ?)
                    THEN TRUE
                    ELSE FALSE
                END
                """;
        return jdbcTemplate.queryForObject(sql, boolean.class, id);
    }

    @Override
    public boolean exist(ReservationTime reservationTime) {
        String sql = """
                SELECT
                CASE
                    WHEN EXISTS (SELECT 1 FROM reservation_time WHERE start_at = ?)
                    THEN TRUE
                    ELSE FALSE
                END
                """;
        return jdbcTemplate.queryForObject(sql, boolean.class, reservationTime.getStartAt().toStringTime());
    }

    @Override
    public void delete(long id) {
        String sql = """
                DELETE
                FROM reservation_time
                WHERE id = ?
                """;
        jdbcTemplate.update(sql, id);
    }

    private ReservationTime getReservationTime(ResultSet resultSet) throws SQLException {
        return new ReservationTime(
                resultSet.getLong("id"),
                ReservationStartAt.from(resultSet.getString("start_at"))
        );
    }
}
