# Phase 2 local data contract

## Storage ownership

- Room database: app-private `trippilot.db`; no shared database, sync service, account, or network permission.
- DataStore: only the `readiness_reminder_enabled` UI preference. It does not hold travel records, AI content, account state, OAuth data, or secrets.
- Backup contract: `trippilot.trip-backup` schema v1, UTF-8, maximum 2 MiB, maximum 100 trips and 500 records per nested collection. A trip carries itinerary, Preparation, Packing, Reservation, and Source fields. URLs, dates, timezones, enum values, item counts, and source-owner indexes are validated before restore. Restore creates fresh IDs for every TripPilot record and may never overwrite an existing record.
- The compatibility reader accepts one explicitly documented legacy *public JSON schema literal* only. It maps only title, destination, start/end date, timezone, scope, and notes. It does not read an external app's database, chat history, files, account, or OAuth data.

## Migration policy

Room schema `1.json` is checked into `app/schemas/`. The app does **not** enable destructive fallback. Any v2 change must add an explicit `Migration`, an exported schema, a migration test from every supported version, and a user-facing backup/recovery note before versionCode release.

## External-action boundary

Phase 2 registers only `ACTION_SEND` with MIME type `text/plain`. The shared text is shown first; the user must select a TripPilot trip before it is stored for 24 hours. The app does not parse it into a reservation, open it, browse it, or execute any external action. Cancel/consume deletes it; expired intake is deleted before the next store operation.

Calendar actions are schema/ledger placeholders only. This phase never requests calendar permission, sends an intent to maps/browser, schedules a notification, or invokes Codex.
