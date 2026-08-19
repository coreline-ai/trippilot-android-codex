# Play Data safety 대응 초안 — TripPilot 0.1.0

상태: **초안**. 실제 제출은 배포 owner가 서명 artifact 기준으로 확정한다.

## 요약 선언 (제안)

**수집하는 데이터: 없음.** TripPilot은 모든 여행 데이터를 기기 내 Room DB에만 저장하고 어떤 서버로도 전송하지 않는다. Analytics SDK, 충돌 리포터, 광고 ID, 원격 설정이 포함되어 있지 않다.

예외 — **사용자가 명시적으로 시작하는 Codex 연결(secureDebug/release 빌드)**:
- 사용자가 직접 OAuth 로그인을 시작한 경우에만, 사용자가 입력한 여행 요청 정보(목적지·기간·관심사 등)가 사용자 자신의 OpenAI/Codex 계정으로 전송된다
- 토큰은 Android Keystore로 보호되어 앱 전용 영역에만 저장되며 화면에 표시되지 않고 백업에서 제외된다
- debug 빌드는 INTERNET 권한 자체가 없어 이 경로가 존재하지 않는다

## Play 콘솔 항목별 매핑 (제안)

| 항목 | 선언 |
|---|---|
| Data collected | 없음 (수집 서버 부재) |
| Data shared | 없음 (third party 전송 코드 부재; 사용자가 시작한 Codex 요청은 사용자 자신 계정으로의 전송이며 "shared" 아님 — 콘솔 정책 확인 필요) |
| Data encrypted in transit | 해당 없음 (전송 없음). Codex 경로는 HTTPS/UDS |
| Users can request data deletion | 앱 내 여행 삭제·전체 데이터 삭제로 기기 내 즉시 삭제. 서버 데이터 없음 |

## 권한 사용 근거 (선언용)

| 권한 | 용도 | 사용 시점 |
|---|---|---|
| `READ_CALENDAR` / `WRITE_CALENDAR` (runtime) | 사용자가 선택한 일정만 내 캘린더에 추가·중복 방지 | 사용자가 "Calendar 반영" 승인 시만 |
| `POST_NOTIFICATIONS` (runtime) | D-7~D-1 미완료 준비 항목 하루 1회 알림 | 사용자가 알림 켜기 승인 시만 |
| `RECEIVE_BOOT_COMPLETED` | 재부팅 후 승인된 알림 일정만 재예약 | 부팅 시, 알림이 켜져 있을 때만 |
| `INTERNET` (secureDebug/release만) | 사용자가 시작한 Codex OAuth/초안 요청 | 사용자가 연결을 시작할 때만 |

## 백업 정책

- Android cloud backup / device-to-device: **차단** (`allowBackup=false` 규칙) — 여행 데이터가 의도치 않게 클라우드로 가지 않게 한다
- 사용자 주도 백업: 앱 내 JSON 내보내기(SAF) — 민감 필드(AI 원문·OAuth·승인 이력) 미포함, 검증 후 새 사본으로만 복원

## 개인정보 처리 원칙 (요약)

전체 경계는 [`privacy-and-egress.md`](privacy-and-egress.md) 참고. 핵심: 연락값·안전 메모·여행 텍스트는 로그·analytics·AI prompt·알림 본문에 포함되지 않는다.

## 제출 전 체크

- [ ] 최신 Play 정책에서 "사용자 계정으로의 직접 전송" 분류 재확인
- [ ] 데이터 삭제 응답(기기 내 삭제) 스크린샷/절차 준비
- [ ] 콘솔 선언 스크린샷을 release 검증 기록에 보관
