# Roadmap — SMS Expense Tracker (Bangladesh)

> Living document. Update the status column as work progresses. Decisions recorded here are canonical, reference this before starting any milestone.

---

## North Star

A Bangladeshi user installs the app, grants SMS permission, and within 30 seconds sees their complete transaction history. No accounts to create, no data to enter manually, no internet required.

---


## Milestones

### M0 — Skeleton & Tooling
**Goal:** Empty project builds, CI passes, team conventions in place.
**Status:** In progress

| Task | Owner | Done? |
|---|---|---|
| Init Android project (Kotlin DSL, minSdk 26) | | |
| Configure Hilt, Room, Coroutines, Compose BOM | | |
| Set up GitHub Actions: build + unit test on every PR | | |
| Write `SmsParser` interface and `ParseResult` model | | |
| Write `ParserEngine` with zero parsers registered (returns null always) | | |
| Confirm `./gradlew test` passes on green-field project | | |

**Exit criteria:** `./gradlew build` and `./gradlew test` both pass locally in Android Studio with no warnings suppressed. (Note: verification of Gradle builds happens on Sajib's machine; the assistant's sandbox has no Android SDK or Google Maven access.)

---

### M1 — First Parser: bKash
**Goal:** Parse real bKash SMS into `Transaction` records. This milestone proves the parser architecture end-to-end.
**Status:** Not started
**Note:** Real bKash/Nagad/Rocket SMS history is already available in Sajib's default Messages app; no separate collection step needed, samples will be copied out when this milestone starts.

| Task | Done? |
|---|---|
| Collect >= 10 real bKash SMS samples (send money, receive money, payment, cashout, add money) from Messages app | |
| Implement `BkashParser` with regex per transaction type | |
| Unit tests: all 5 sample types + edge cases (amount with comma, Tk vs BDT prefix) | |
| Room schema: `transactions` table + DAO with insert and query by date range | |
| `SmsReceiver` BroadcastReceiver wired to `ParserEngine` -> Room insert | |
| Manual import job: read SMS inbox on first launch, parse all bKash messages | |
| Minimal Compose UI: scrollable list of parsed transactions | |
| Confirm no raw SMS body appears in Logcat (production build flag) | |

**Exit criteria:** Install on a real device (BlueStacks cannot receive real SMS, no SIM/telephony) with bKash SMS history. All bKash transactions appear in the list within 5 seconds of granting permission.

---

### M2 — Core Wallet Parsers
**Goal:** Cover the three dominant mobile wallets: bKash (already done), Nagad, Rocket.
**Status:** Not started
**Depends on:** M1

| Parser | Sample types needed | Done? |
|---|---|---|
| `NagadParser` | Send, receive, payment, cashout, add money | |
| `RocketParser` | Send, receive, payment, cashout, DBBL-to-Rocket | |

**Exit criteria:** On a device with mixed bKash/Nagad/Rocket SMS history, all three wallets appear as separate accounts in the transaction list with correct amounts.

---

### M3 — Dashboard UI
**Goal:** Replace the plain list with a real home screen showing financial summary.
**Status:** Not started
**Depends on:** M2

Decisions locked:
- Per-institution breakdown (e.g. "bKash: received X, sent Y") shown alongside an aggregate total balance card, not one blended number.
- UI ships in both English and Bangla from this milestone. English strings are primary since bKash/bank SMS content itself is in English; Bangla is a full parallel string set, not a stub.

Features:
- Total balance card (sum across all institutions)
- Per-institution breakdown cards (bKash, Nagad, Rocket, DBBL, Sonali Bank, etc.)
- Income vs. expense bar for current month
- Last 10 transactions with institution logo, amount (green/red), and counterparty
- Quick filter chips: Today / This Week / This Month / All

**Exit criteria:** UI reviewed on Pixel 6 (API 33) emulator and a low-end device (2GB RAM, API 26). No jank on scroll of 500+ transactions. Both English and Bangla strings render correctly with no missing translations.

---

### M4 — Bank SMS Parsers
**Goal:** Support major BD scheduled banks that send transaction alert SMS.
**Status:** Not started
**Depends on:** M1 (parser architecture)
**Priority order confirmed:** DBBL and Sonali Bank first, both are accounts Sajib personally holds and can generate real test SMS from.

| Parser | Supported alert types | Priority | Done? |
|---|---|---|---|
| `DutchBanglaParser` | Debit alert, credit alert, OD alert | 1st | |
| `SonaliBankParser` | Debit alert, credit alert | 1st | |
| `BracBankParser` | Debit, credit | 2nd | |
| `IslamiBankParser` | Debit, credit | 2nd | |
| `UcbParser` | Debit, credit | 2nd | |
| `CityBankParser` | Debit, credit | 2nd | |

> Sonali Bank is state-owned; SMS sender address and format are not yet confirmed on a real device, this needs to be verified before implementation starts (see AGENT.md SMS Sender Reference).
> Add rows as new banks are confirmed. Each parser requires real SMS samples before implementation begins.

---

### M5 — Telecom / Airtime Parsers
**Goal:** Track mobile recharge and data pack purchases alongside financial transactions.
**Status:** Not started
**Depends on:** M1

| Parser | Alert types | Done? |
|---|---|---|
| `GrameenphoneParser` | Balance recharge, data pack activation, balance inquiry | |
| `RobiParser` | Balance recharge, data pack, balance | |
| `BanglalinkParser` | Balance recharge, data pack | |
| `TeletalkParser` | Balance recharge | |

Telecom SMS produces `TransactionType.RECHARGE` or `TransactionType.AIRTIME` records, displayed in a separate "Telecom" section of the dashboard.

---

### M6 — Reports & Export
**Goal:** Weekly and monthly summaries, CSV export.
**Status:** Not started
**Depends on:** M3

Features:
- Monthly expense by category (auto-categorized by institution + transaction type)
- Week-over-week spending trend line chart (MPAndroidChart or Compose Canvas)
- Export to CSV (transactions for a selected date range)
- Share sheet for exported file

**Exit criteria:** A 12-month transaction history (5,000+ rows) renders the monthly chart in under 1 second. CSV opens correctly in Google Sheets.

---

### M7 — Manual Entry & Categorization
**Goal:** Let users add cash transactions and override auto-categories.
**Status:** Not started
**Depends on:** M3

Features:
- Add transaction bottom sheet: amount, type, date, note, category
- Category tag on each transaction (auto-assigned, user-editable)
- Custom categories (user-defined label + color)
- Bulk recategorize

---

### M8 — Cloud Sync (Optional)
**Goal:** Firebase Firestore sync for users who want multi-device or backup.
**Status:** Not started
**Depends on:** M6

Design constraints:
- Sync is OFF by default. User must enable in Settings and sign in with Google.
- Only structured `Transaction` records are synced, raw SMS bodies are never uploaded.
- Conflict resolution: last-write-wins on `updatedAt` timestamp.
- Offline-first: local DB is always authoritative; sync is background-only.

Tasks:
- [ ] Firebase project setup (Firestore, Auth)
- [ ] `FirestoreSyncAdapter` implementing `SyncAdapter` interface
- [ ] Sync toggle in Settings with clear privacy explanation
- [ ] Background WorkManager job for periodic sync
- [ ] Conflict resolution tests

**Exit criteria:** Transactions added on Device A appear on Device B within 60 seconds when both are online. Disabling sync removes all cloud data.

---

### M9 — Extensibility UI (Parser Management)
**Goal:** Let power users see which parsers are active and toggle them.
**Status:** Not started
**Depends on:** M8

Features:
- Settings > Parsers: list of all registered parsers with toggle on/off
- Per-parser: name, institution, last matched SMS date, match count
- "Test a message" debug tool: paste an SMS body, see what ParseResult it produces

---

## Parsers Backlog

Institutions confirmed to send transaction SMS but not yet assigned to a milestone:

| Institution | Type | Notes |
|---|---|---|
| Mutual Trust Bank (MTB) | Bank | |
| Southeast Bank | Bank | |
| Prime Bank | Bank | |
| Eastern Bank (EBL) | Bank | |
| Standard Chartered BD | Bank | Different SMS format from local banks |
| AB Bank | Bank | |
| Airtel (now Robi) | Telecom | May be covered by RobiParser |
| Skitto (GP subsidiary) | Telecom | Verify if sender differs from GP |

---

## Non-Goals (Explicit Out of Scope)

These will not be built unless the product direction changes:

- iOS / macOS / Windows app (also not technically possible for SMS reading, Apple blocks third-party SMS access)
- Web dashboard
- Automatic bill payment or money transfer
- Investment / portfolio tracking
- OCR of paper receipts
- Screen scraping of banking apps
- Shared wallets / household budgeting (multi-user)
- Third-party integrations (Tally, QuickBooks, etc.)
- Becoming the device's default SMS handler (would be required for unrestricted Play Store SMS permission access, but conflicts with the app's actual purpose; see Open Questions)

---

## Open Questions

| Question | Status |
|---|---|
| Play Store distribution: Google requires apps requesting READ_SMS/RECEIVE_SMS to be the user's default SMS, Phone, or Assistant handler, or to qualify for a narrow, case-by-case exception. Sideloaded APK distribution avoids this entirely. | Decision needed before M8-M9; does not block M0-M7 |
| Which BD banks use shared short codes that could cause parser collisions? | Research needed before M4 |
| Sonali Bank exact SMS sender address and message format | Needs verification on a real device before M4 implementation starts |
| Are there GDPR/PDPA implications for Bangladeshi users? | Legal review before M8 (cloud sync) |

---


## Tech Debt Log

> Add items here when you knowingly cut a corner. Each item needs a milestone target for resolution.

| Item | Added in | Target fix |
|---|---|---|
| (none yet) | | |