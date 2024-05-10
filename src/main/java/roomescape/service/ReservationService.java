package roomescape.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import roomescape.dao.MemberDao;
import roomescape.dao.ReservationDao;
import roomescape.dao.ReservationTimeDao;
import roomescape.dao.ThemeDao;
import roomescape.domain.member.Member;
import roomescape.domain.reservation.Reservation;
import roomescape.domain.reservation.ReservationDate;
import roomescape.domain.reservationtime.ReservationTime;
import roomescape.domain.theme.Theme;
import roomescape.dto.reservation.AdminReservationCreateRequest;
import roomescape.dto.reservation.AvailableReservationResponse;
import roomescape.dto.reservation.ReservationCreateRequest;
import roomescape.dto.reservation.ReservationFilterRequest;
import roomescape.dto.reservation.ReservationResponse;

@Service
public class ReservationService {

    private final ReservationDao reservationDao;
    private final ReservationTimeDao reservationTimeDao;
    private final MemberDao memberDao;
    private final ThemeDao themeDao;

    public ReservationService(ReservationDao reservationDao,
                              ReservationTimeDao reservationTimeDao,
                              MemberDao memberDao,
                              ThemeDao themeDao) {
        this.reservationDao = reservationDao;
        this.reservationTimeDao = reservationTimeDao;
        this.memberDao = memberDao;
        this.themeDao = themeDao;
    }

    public List<ReservationResponse> findAll() {
        List<Reservation> reservations = reservationDao.readAll();
        return reservations.stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public List<ReservationResponse> findFiltered(ReservationFilterRequest request) {
        ReservationDate from = ReservationDate.from(request.getDateFrom());
        ReservationDate to = ReservationDate.from(request.getDateTo());
        Member member = findMemberById(request.getMemberId());
        Theme theme = findThemeBy(request.getThemeId());

        validateFromDateIsNotAfterToDate(from, to);

        List<Reservation> reservations =
                reservationDao.readAllByMemberAndThemeAndDateBetweenFromAndTo(member, theme, from, to);
        return reservations.stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public List<AvailableReservationResponse> findTimeByDateAndThemeID(String date, Long themeId) {
        ReservationDate reservationDate = ReservationDate.from(date);
        List<ReservationTime> allTimes = reservationTimeDao.readAll();
        List<ReservationTime> times = reservationDao.readTimesByDateAndThemeId(reservationDate, themeId);
        return allTimes.stream()
                .map(filteredTime -> getAvailableReservationResponse(filteredTime, times))
                .toList();
    }

    public ReservationResponse add(AdminReservationCreateRequest request, LocalDateTime now) {
        ReservationTime reservationTime = findReservationTimeBy(request.getTimeId());
        Theme theme = findThemeBy(request.getThemeId());
        Member member = findMemberById(request.getMemberId());
        Reservation reservation = request.toDomain(member, reservationTime, theme);
        reservation.validatePast(now);
        validateDuplicate(reservation);
        return ReservationResponse.from(reservationDao.create(reservation));
    }

    public ReservationResponse add(Member member, ReservationCreateRequest request, LocalDateTime now) {
        ReservationTime reservationTime = findReservationTimeBy(request.getTimeId());
        Theme theme = findThemeBy(request.getThemeId());
        Reservation reservation = request.toDomain(member, reservationTime, theme);
        reservation.validatePast(now);
        validateDuplicate(reservation);
        return ReservationResponse.from(reservationDao.create(reservation));
    }

    public void delete(Long id) {
        validateNull(id);
        Reservation reservation = findReservationBy(id);
        reservationDao.delete(reservation);
    }

    private void validateFromDateIsNotAfterToDate(ReservationDate from, ReservationDate to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("시작 날짜는 종료 날짜보다 이후일 수 없습니다.");
        }
    }

    private void validateDuplicate(Reservation reservation) {
        if (reservationDao.hasSame(reservation)) {
            throw new IllegalArgumentException("이미 예약이 있어 추가할 수 없습니다.");
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
                .orElseThrow(() -> new IllegalArgumentException("예약 아이디에 해당하는 예약이 존재하지 않습니다."));
    }

    private ReservationTime findReservationTimeBy(Long id) {
        return reservationTimeDao.readById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약 시간 아이디에 해당하는 예약 시간이 존재하지 않습니다."));
    }

    private Member findMemberById(Long id) {
        return memberDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원 아이디에 해당하는 회원이 존재하지 않습니다."));
    }

    private Theme findThemeBy(Long id) {
        return themeDao.readById(id)
                .orElseThrow(() -> new IllegalArgumentException("테마 아이디에 해당하는 테마가 존재하지 않습니다."));
    }
}
