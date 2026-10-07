# Roadmap: SMS Expense Tracker (Bangladesh)

> Living document. Update the status column as work progresses. Decisions recorded here are canonical, reference this before starting any milestone.

---

## North Star

A Bangladeshi user installs the app, grants SMS permission, and within 30 seconds sees their complete transaction history. No accounts to create, no data to enter manually, no internet required.

---

## Milestones

### M0: Skeleton & Tooling
**Goal:** Empty project builds, CI passes, team conventions in place.
**Status:** Done

| Task | Owner | Done? |
|---|---|---|
| Init Android project (Kotlin DSL, minSdk 26) | | |
| Configure Hilt, Room, Coroutines, Compose BOM | | |
| Set up GitHub Actions: build + unit test on every PR | | |
| Write `SmsParser` interface and `ParseResult` model | | |
| Write `ParserEngine` with zero parsers registered (returns null always) | | |
| Confirm `./gradlew test` passes on green-field project | | |

**Exit criteria:** `./gradlew build` and `./gradlew test` both pass locally in Android Studio with no warnings suppressed for real code issues. Advisory version-check lint rules (`NewerVersionAvailable`, `GradleDependency`, `AndroidGradlePluginVersion`) are explicitly disabled via lint config, since they are time-relative and chasing them causes unnecessary breaking migrations, not quality improvements. (Note: verification of Gradle builds happens on Sajib's machine; the assistant's sandbox has no Android SDK or Google Maven access.)

---

### M1: First Parser (bKash)
**Goal:** Parse real bKash SMS into `Transaction` records. This milestone proves the parser architecture end-to-end.
**Status:** Done

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

### M2: Core Wallet Parsers
**Goal:** Cover the three dominant mobile wallets: bKash (already done), Nagad, Rocket.
**Status:** Nagad done, not device-verified (built from anonymized samples of a third-party real export, not Sajib's own phone). Rocket still blocked, no real data; do not guess its format to unblock this.
**Depends on:** M1

| Parser | Sample types needed | Done? |
|---|---|---|
| `NagadParser` | Cash In, Money Received, Payment (seen so far; send and cashout not yet seen) | Yes (2026-10-07, unit tests green, not device-verified) |
| `RocketParser` | Send, receive, payment, cashout, DBBL-to-Rocket | |

**Exit criteria:** On a device with mixed bKash/Nagad/Rocket SMS history, all three wallets appear as separate accounts in the transaction list with correct amounts.

---

### M3: Dashboard UI
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

### M4: Bank SMS Parsers
**Goal:** Support major BD scheduled banks that send transaction alert SMS.
**Status:** Ready to start for DBBL, real data confirmed. Blocked on Sonali Bank pending real transaction samples (only OTP/notification noise found so far).
**Depends on:** M1 (parser architecture)
**Priority order confirmed:** DBBL first (real data now in hand), then Sonali Bank once real transaction SMS appears, then FSIBL (new real bank, real credit samples found, not previously known).

| Parser | Supported alert types | Priority | Done? |
|---|---|---|---|
| `DutchBanglaParser` | Balance inquiry, ATM-to-A/C transfer credit, NexusPay cash-out debit | 1st, real data confirmed | Yes (2026-09-02, unit tests green) |
| `SonaliBankParser` | Debit alert, credit alert | 1st, blocked on real samples | |
| `FsiblParser` | Deposit/credit alert | 2nd, real data confirmed | |
| `BracBankParser` | Withdrawal, deposit, debit, credit, branch counter deposit and withdrawal, incoming transfer from another bank, cash deposit, RTGS debit (amount includes charges), loan-linked account deposit, cheque clearing credit. Sender `BRAC BANK`; `BRACBANK` is notification only and rejected | 3rd, real export samples | Yes (2026-10-07, unit tests green, not device-verified) |
| `UttaraBankParser` | Debit, credit (balance may be negative or absent), SMS service charge debit, balance statement (BALANCE_INQUIRY) | 3rd, real export samples | Yes (2026-10-07, unit tests green, not device-verified) |
| `PubaliBankParser` | Branch debit and credit, EFT credit, fund transfer credit, PI Banking credit ("Dr" balance stored negative, "Cr" positive, no marker keeps its sign) | 3rd, real export samples | Yes (2026-10-07, unit tests green, not device-verified) |
| `IslamiBankParser` | Debit, credit | 3rd | |
| `UcbParser` | Debit, credit | 3rd | |
| `CityBankParser` | Deposit/credit alert | 3rd, one real sample only, needs more before building | |

> Real finding: sender `16216` is shared between DBBL bank account alerts (balance, ATM transfer, NexusPay cash-out) and, per AGENT.md's original reference table, was assumed to be Rocket's sender code. For this account, every `16216` message observed is a DBBL bank alert, not a Rocket wallet transaction. Sender-code-only matching is not reliable for `16216`; if a Rocket parser is ever built, it cannot safely claim this sender code without also checking message content (e.g. presence of "NexusPay" or "Rocket" wording) to avoid misclassifying real bank transactions as wallet transactions.
> First Security Islami Bank (FSIBL) is a newly discovered real institution, not previously known. Real samples use "Muhtaram" greeting, deposit-only so far (no debit sample yet).
> City Bank has one real deposit sample, multiline format (uses literal newlines in the SMS body), no TrxID visible in the sample seen. Needs more samples, including a debit, before building.
> Sonali Bank is state-owned; real SMS seen so far are entirely OTP/notification noise (PIN changes, maintenance notices), no real transaction SMS yet.
> Add rows as new banks are confirmed. Each parser requires real SMS samples before implementation begins.

#### Known unsupported messages (parsers return null on purpose)

| Sender | Message | Reason |
|---|---|---|
| PUBALI BANK | bKash QR payment received ("BDT X RECEIVED FROM ... ,bKash USING QR PAYMENT ON ... .TrxID:...") | No balance, and the Merchant-QR settlement message describes the same money; parsing both would double count |
| PUBALI BANK | Merchant-QR settlement credit ("Dear Merchant! TK X has been credited for Merchant-QR transaction On Settlement Date ...") | No balance, and it repeats the QR payment above; parsing both would double count |
| PUBALI BANK | PI Banking app promotion, biometric registration request, account number change after branch merging, thank-you message, cheque book collection, maintenance notice | Not transactions |
| UTTARA BANK | RTGS sent notice ("your fund transfer through RTGS of TK. X to ... has been sent.") | The bank sends a separate debit alert with the balance for the same transfer; parsing both would double count |
| UTTARA BANK | SMS banking account created (contains a PIN), loan instalment due, cheque book ready, contact center, dormancy, app update, maintenance, OTP, Bangla security warnings and promotions | Not transactions; the account-created message must never be stored or logged |
| BRAC BANK | SME loan instalment reminders, paid confirmations, overdue notices, term loan disbursement | Loan account events, not movements on the tracked deposit account |
| BRAC BANK | Dormancy prevention, cheque book notices, statement and certificate links, OTP messages, sender number notice, Astha registration, bKash promotion | Not transactions |
| BRACBANK | Every message | Notification-only sender, never routed to a parser |
| NAGAD | Recharge cashback ("Congrats! You've received Cashback X Tk for Mobile Recharge of Y Tk.") | No balance and not a wallet transaction |
| NAGAD | OTP, device registration, Bangla fraud warning | Not transactions |

---

### M5: Telecom / Airtime Parsers
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

### M6: Reports & Export
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

### M7: Manual Entry & Categorization
**Goal:** Let users add cash transactions and override auto-categories.
**Status:** Not started
**Depends on:** M3

Features:
- Add transaction bottom sheet: amount, type, date, note, category
- Category tag on each transaction (auto-assigned, user-editable)
- Custom categories (user-defined label + color)
- Bulk recategorize

---

### M8: Cloud Sync (Optional)
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

### M9: Extensibility UI (Parser Management)
**Goal:** Let power users see which parsers are active and toggle them.
**Status:** Not started
**Depends on:** M8

Features:
- Settings > Parsers: list of all registered parsers with toggle on/off
- Per-parser: name, institution, last matched SMS date, match count
- "Test a message" debug tool: paste an SMS body, see what ParseResult it produces

---

### M10: Play Store Build
**Goal:** Ship a Google Play compliant build alongside the full (sideloaded) build, both feeding one ingestion entry point.
**Status:** B1 done, B2 active

| Phase | Scope | Status |
|---|---|---|
| B1 | Build flavors `play` and `full` (dimension `distribution`); SMS permissions and receiver only in `full`; `LiveCaptureSetup` interface with per-flavor Hilt bindings; single ingestion entry point `IngestSmsUseCase` returning Saved, Duplicate or Unrecognized | Done |
| B2 | Import from an SMS Backup & Restore XML file in both flavors, with no new permission | Active |
| B3 | Share and paste intake, plus onboarding | Not started |
| B4 | Release preparation: signing, AAB, privacy policy, store listing | Not started |

#### B2: what it delivers

- The user picks an XML export of the SMS Backup & Restore app with the system file picker (Storage Access Framework, `ACTION_OPEN_DOCUMENT`). The file is read once through ContentResolver; it is not copied and no persistable URI permission is taken. No READ_SMS, INTERNET or storage permission is needed, so the play flavor gets the feature unchanged. All code is in `src/main`.
- `data/backup/SmsBackupReader` streams the file: UTF-8 decoding with malformed bytes replaced, a constant-memory `SanitizingXmlReader` in front of the platform XmlPullParser, and a lazy sequence of entries. Emoji written as two surrogate references become one valid reference; lone surrogate and control character references become U+FFFD. A DOCTYPE, a wrong root, a damaged or truncated file each give a typed `BackupImportException` (NOT_SMS_BACKUP, MALFORMED, READ_FAILED) whose message holds no message content.
- `ImportSmsBackupUseCase` sends every received (`type="1"`) message through `IngestSmsUseCase` and returns an `ImportSummary` of counts only: totalRead, saved, duplicates, unrecognized, skippedNotInbox, invalid. It runs on the IO dispatcher, is cancellable and reports progress. Rows saved before a failure or a cancellation stay saved; re-importing is idempotent through the duplicate check. Unrecognized bodies are never stored or logged.
- `TransactionListScreen` has an "Import from backup" app bar action and an empty state button. A dialog shows progress (with a Stop button), the summary, or the failure.
- Verified locally on a real third-party export of 999 received messages: 826 saved and 173 unrecognized on the first run, 0 saved and 826 duplicates on the second.

#### Importing from SMS Backup & Restore

1. In the SMS Backup & Restore app, make a backup of messages and choose a local backup location, for example the phone's storage or Downloads. The result is an XML file named like `sms-YYYYMMDDHHMMSS.xml`.
2. In this app, tap "Import from backup" in the top bar, or "Import from backup file" while the list is empty.
3. Pick the XML file. The picker lists all file types, not only XML, because some file managers label this file with an unusual type.
4. Wait for the summary. Only received messages are read; sent messages are skipped. Messages no parser recognizes are ignored and not stored. Importing the same file again adds nothing new.

> Open conflict, not resolved: the play flavor must declare no INTERNET permission (AGENT.md rule 11), so the planned Firebase sync (M8) cannot ship in the play flavor unless INTERNET is added together with a privacy disclosure.

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
| Which BD banks use shared short codes that could cause parser collisions? | Research needed before M4 |
| Sonali Bank exact SMS sender address and message format | Needs verification on a real device before M4 implementation starts |
| Are there GDPR/PDPA implications for Bangladeshi users? | Legal review before M8 (cloud sync), low priority given personal/direct-share distribution |

## Decisions Log

| Decision | Rationale |
|---|---|
| Distribution: sideload APK only, no Play Store | Personal use, no budget for the $25 Play Console fee, and it avoids Google's default-SMS-handler requirement for READ_SMS entirely. Sharing with others happens by handing them the APK directly. |
| Play Protect / Vivo security warnings on install are expected, not a bug | An app requesting READ_SMS/RECEIVE_SMS from an unsigned/unknown source will very likely trigger a warning on Pixel (Play Protect) and Vivo (iManager) on every fresh sideload. This is normal for this permission combination and is dismissed with a manual "install anyway", not something to engineer around. |
| Locked stable stack: whatever AGP/Gradle/Kotlin/compileSdk versions are green after the lint fix (M0 baseline) | Original stack (AGP 8.7.3, Kotlin 2.0.21, compileSdk 35) built and tested green on first try. Chasing "newer version available" lint warnings pulled in AGP 9.2.1, Gradle 9.6.1, compileSdk 37 and broke the build repeatedly for zero functional gain, since those specific lint checks are time-relative and never stay satisfied. Do not revert now that it is green; lock the current working versions in the version catalog and do not bump again without a deliberate, specific reason (a real dependency requiring it), not an advisory warning. |
| bKash Cash Out likely sends no SMS at all (confirmed, not just missing sample) | A real Cash Out was performed and verified in the bKash app's own transaction history (TrxID AAAAAAAAAA, invented placeholder for the real id). Searched both the default Messages app and Truecaller (which also indexes SMS) on the device, found nothing. A second person (roommate) reports the same experience. This is treated as a structural gap, not a "need a sample" backlog item: no SmsParser can ever catch a transaction type the network never sends as SMS. Cash Out stays unbuilt via the parser path. If the user wants Cash Out tracked at all, the only route is manual entry (M7), not a parser. Do not revisit this as an M2/M4 task unless new evidence (an actual Cash Out SMS) surfaces. |

---

## Tech Debt Log

> Add items here when you knowingly cut a corner. Each item needs a milestone target for resolution.

| Item | Added in | Target fix |
|---|---|---|
| (none yet) | | |