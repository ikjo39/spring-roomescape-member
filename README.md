# 🚪 방탈출 사용자 예약

## 페어 프로그래밍 컨벤션
- 클래스를 정의한 뒤 다음 줄은 공백으로 한다.
- `@Test` ➡️ `@DisplayName` 순으로 작성한다.

## 1단계 요구사항
- [x] 정상 동작 확인
- [x] 시간 관리, 예약 관리 API가 적절한 응답을 하도록 변경
- [ ] 발생할 수 있는 예외 상황에 대한 처리를 하여, 사용자에게 적절한 응답
  - [x] 시간 생성 시 시작 시간에 유효하지 않은 값이 입력되었을 때
    - [x] null 또는 공백 문자열이 입력된 경우
    - [x] 시작 시간 형식이 HH:mm이 아닌 경우
  - [ ] 예약 생성 시 예약자명, 날짜, 시간에 유효하지 않은 값이 입력 되었을 때
    - [ ] 이름, 날짜가 null 또는 공백 문자열이 입력된 경우
    - [ ] 날짜 형식이 yyyy-MM-dd가 아닌 경우
    - [ ] 존재하지 않는 시간 아이디가 입력된 경우
  - [ ] 특정 시간에 대한 예약이 존재하는데, 그 시간을 삭제하려 할 때
- [ ] 지나간 날짜와 시간에 대한 예약 생성은 불가능
- [ ] 중복 예약은 불가능
