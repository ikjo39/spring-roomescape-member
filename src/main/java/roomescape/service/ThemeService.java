package roomescape.service;

import java.util.List;
import org.springframework.stereotype.Service;
import roomescape.dao.ReservationDao;
import roomescape.dao.ThemeDao;
import roomescape.domain.theme.Theme;
import roomescape.dto.theme.ThemeCreateRequest;
import roomescape.dto.theme.ThemeResponse;

@Service
public class ThemeService {

    private final ThemeDao themeDao;
    private final ReservationDao reservationDao;

    public ThemeService(ThemeDao themeDao, ReservationDao reservationDao) {
        this.themeDao = themeDao;
        this.reservationDao = reservationDao;
    }

    public List<ThemeResponse> findAll() {
        return themeDao.readAll().stream()
                .map(ThemeResponse::from)
                .toList();
    }

    public ThemeResponse add(ThemeCreateRequest request) {
        Theme theme = request.toDomain();
        Theme result = themeDao.create(theme);
        return ThemeResponse.from(result);
    }

    public void delete(Long id) {
        validateNull(id);
        validateNotExistTheme(id);
        validateExistReservationByThemeId(id);
        themeDao.delete(id);
    }

    private void validateNull(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("테마 아이디는 비어있을 수 없습니다.");
        }
    }

    private void validateNotExistTheme(Long id) {
        if (!themeDao.exist(id)) {
            throw new IllegalArgumentException("테마 아이디에 해당하는 테마가 존재하지 않습니다.");
        }
    }

    private void validateExistReservationByThemeId(Long id) {
        if (reservationDao.existByThemeId(id)) {
            throw new IllegalArgumentException("해당 테마를 사용하는 예약이 존재합니다.");
        }
    }
}
