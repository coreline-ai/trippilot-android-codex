# Phase 2 local data contract

## Storage ownership

- Room database: app-private `trippilot.db`; no shared database, sync service, account, or network permission.
- DataStore: only the `readiness_reminder_enabled` UI preference. It does not hold travel records, AI content, account state, OAuth data, or secrets.
- Backup contract: `trippilot.trip-backup` schema v1–v3, UTF-8, maximum 2 MiB, maximum 100 trips and 500 records per nested collection. A trip carries itinerary, Preparation (with optional post-trip window since v3), Packing, Reservation, Source, and SafetyMemo (since v3) fields. URLs, dates, timezones, enum values, item counts, source-owner indexes, contact-value (200자) and note (2000자) limits are validated before restore. Restore creates fresh IDs for every TripPilot record and may never overwrite an existing record.
- The compatibility reader accepts one explicitly documented legacy *public JSON schema literal* only. It maps only title, destination, start/end date, timezone, scope, and notes. It does not read an external app's database, chat history, files, account, or OAuth data.

## v3 additions (2026-08-17 문제 대응·귀국 후)

- `preparation_items.postTripWindow` — nullable. null은 기존 출발 전/일반 항목. 귀국 후 팩은 사용자 opt-in으로만 추가되고 template ID 기준 idempotent하다
- `safety_memos` — trip cascade 소유. category(7개 정적 enum), title, note, contactLabel/contactValue(사용자 소유). 정적 `ProblemResponseCatalog` 문구와 절대 혼합하지 않는다
- `SourceOwnerType.SAFETY_MEMO` — 안전 메모 출처. 메모 삭제 시 연결 출처가 같은 transaction에서 정리된다. backup v3는 safety-memo 출처를 담지 않고(사용자가 복원 후 재연결) 메모 본문만 담는다
- SafetyMemo 데이터는 debug log, analytics, AI prompt, 알림 본문에 포함되지 않는다

## Migration policy

Room schemas `1.json`–`3.json` are checked into `app/schemas/`. The app does **not** enable destructive fallback. Any version change must add an explicit `Migration`, an exported schema, a migration test from every supported version, and a user-facing backup/recovery note before versionCode release. `MIGRATION_2_3` adds only a nullable column and a new table; existing rows are untouched.

## External-action boundary

Phase 2 registers only `ACTION_SEND` with MIME type `text/plain`. The shared text is shown first; the user must select a TripPilot trip before it is stored for 24 hours. The app does not parse it into a reservation, open it, browse it, or execute any external action. Cancel/consume deletes it; expired intake is deleted before the next store operation.

Calendar actions are schema/ledger placeholders only. This phase never requests calendar permission, sends an intent to maps/browser, schedules a notification, or invokes Codex.

Safety-memo contact values copy to the clipboard only; dialing, browsing, or mapping requires the existing ApprovalSheet handoff.
