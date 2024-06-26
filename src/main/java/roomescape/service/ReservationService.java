package roomescape.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import roomescape.dao.ReservationDao;
import roomescape.dao.ReservationTimeDao;
import roomescape.dao.ThemeDao;
import roomescape.domain.reservation.Reservation;
import roomescape.domain.reservation.ReservationDate;
import roomescape.domain.reservationtime.ReservationTime;
import roomescape.domain.theme.Theme;
import roomescape.dto.reservation.AvailableReservationResponse;
import roomescape.dto.reservation.ReservationCreateRequest;
import roomescape.dto.reservation.ReservationResponse;

@Service
public class ReservationService {

    private final ReservationDao reservationDao;
    private final ReservationTimeDao reservationTimeDao;
    private final ThemeDao themeDao;

    public ReservationService(ReservationDao reservationDao, ReservationTimeDao reservationTimeDao, ThemeDao themeDao) {
        this.reservationDao = reservationDao;
        this.reservationTimeDao = reservationTimeDao;
        this.themeDao = themeDao;
    }

    public List<ReservationResponse> findAll() {
        List<Reservation> reservations = reservationDao.readAll();
        return reservations.stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public List<AvailableReservationResponse> findTimeByDateAndThemeID(String date, Long themeId, LocalDateTime now) {
        ReservationDate reservationDate = ReservationDate.from(date);
        List<ReservationTime> allTimes = reservationTimeDao.readAll();
        List<ReservationTime> filteredTimes = reservationDate.filterPastTime(allTimes, now);
        List<ReservationTime> times = reservationDao.readTimesByDateAndThemeId(reservationDate, themeId);
        return filteredTimes.stream()
                .map(filteredTime -> getAvailableReservationResponse(filteredTime, times))
                .toList();
    }

    public ReservationResponse add(ReservationCreateRequest request, LocalDateTime now) {
        ReservationTime reservationTime = findReservationTimeBy(request);
        Theme theme = findThemeBy(request);
        Reservation reservation = request.toDomain(reservationTime, theme);
        reservation.validatePast(reservationTime, now);
        validateDuplicate(reservation);
        return ReservationResponse.from(reservationDao.create(reservation));
    }

    public void delete(Long id) {
        validateNull(id);
        Reservation reservation = findReservationBy(id);
        reservationDao.delete(reservation);
    }

    private void validateDuplicate(Reservation reservation) {
        if (reservationDao.hasSame(reservation)) {
            throw new IllegalArgumentException("중복된 예약을 생성할 수 없습니다.");
        }
    }

    private void validateNull(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("예약 아이디는 비어있을 수 없습니다.");
        }
    }

    private AvailableReservationResponse getAvailableReservationResponse(ReservationTime filteredTime,
                                                                         List<ReservationTime> times) {
        return AvailableReservationResponse.of(
                filteredTime,
                times.stream().anyMatch(time -> Objects.equals(time.getId(), filteredTime.getId()))
        );
    }

    private Reservation findReservationBy(Long id) {
        return reservationDao.readById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 아이디를 가진 예약이 존재하지 않습니다."));
    }

    private ReservationTime findReservationTimeBy(ReservationCreateRequest request) {
        return reservationTimeDao.readById(request.getTimeId())
                .orElseThrow(() -> new IllegalArgumentException("예약 시간 아이디에 해당하는 예약 시간이 존재하지 않습니다."));
    }

    private Theme findThemeBy(ReservationCreateRequest request) {
        return themeDao.readById(request.getThemeId())
                .orElseThrow(() -> new IllegalArgumentException("테마 아이디에 해당하는 테마가 존재하지 않습니다."));
    }
}
