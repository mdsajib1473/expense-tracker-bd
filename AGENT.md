# AGENT.md — SMS Expense Tracker (Bangladesh)

> Instructions for AI agents working in this repository. Read this before touching any code.

---

## 1. Project Overview

An Android application that reads SMS messages from Bangladeshi banks, mobile wallets, and telecom operators, parses them into structured transaction records, and presents the user with a unified expense/income dashboard. All data lives on-device by default; Firebase Firestore sync is an optional, user-toggled feature.

**Platform:** Android (minSdk 26 / Android 8.0+)
**Language:** Kotlin (100%)
**Build system:** Gradle (Kotlin DSL)

---

## 2. Hard Constraints

These are non-negotiable. Never violate them.

1. No SMS content leaves the device unless the user explicitly enables cloud sync. Raw SMS text must never be logged to Logcat in production builds, sent to analytics, or stored in plain text in shared preferences.
2. READ_SMS and RECEIVE_SMS permissions must be requested with a clear rationale dialog before the OS prompt. Explain why in plain Bangla and English.
3. The parser engine must be side-effect-free. A parser receives a string, returns a structured result or null. No database writes, no network calls, no UI updates inside a parser.
4. parsers/ is a plugin boundary. Adding support for a new institution means adding a file under `parsers/`, not touching existing parsers or the core engine. Existing parser tests must stay green.
5. All database migrations must be written explicitly. Never use `fallbackToDestructiveMigration()` outside of debug builds.
6. No forced cloud dependency. The app must be fully functional with no internet connection and no Google account. Firebase is opt-in.

---

## 3. Rules

1. Never build beyond the current milestone. Complete and verify one phase against its Roadmap.md exit criteria before starting the next.
2. Always respond in the same language typed during development conversations (Bangla in, Bangla out).
3. Do not commit or push code. Sajib commits and pushes manually after testing. Ask first if a push seems necessary.
4. Write clean, idiomatic Kotlin. Every public function and class gets a KDoc comment. Add no other comments unless the code is not self-explanatory (see rule 7).
5. Follow secure coding practices: never expose secrets (API keys, Firebase config, keystore passwords) in source control or logs. Always validate and sanitize SMS content before parsing, since it is untrusted input arriving from outside the app. When Firebase sync (M8) is implemented, enforce Firestore security rules scoped strictly to the authenticated user's own data.
6. Write scalable code for new work: keep business logic out of Composables and the UI layer (use the Repository and UseCase layers per the existing architecture), and never hardcode values that belong in `local.properties`, `BuildConfig`, or resource files.
7. Do not over-comment. Only comment where the code is not self-explanatory.
8. Log meaningful state changes for traceability, without violating Hard Constraint 1. Parser match events (institution name, timestamp, matched or not) may be logged; the raw SMS body itself must never appear in any log output, in debug or release builds.
9. No emojis and no em dashes anywhere: code comments, markdown files, commit messages, and all UI copy. Use plain punctuation only.
10. Real data never enters the repo. files/ is gitignored and must stay that way. Never commit real SMS bodies, phone numbers, account numbers, PINs or OTPs. All test fixtures and KDoc sample bodies use invented values only. Run git status before reporting any work as ready and confirm files/ is not listed.
11. Play Store compliance. READ_SMS, RECEIVE_SMS and the SMS receiver exist only in the full flavor source set. The play flavor must declare no SMS permission and no INTERNET permission. After any manifest or dependency change, inspect the merged manifests of both flavors and report the permission lists.
12. Never persist or log the body of a message that no parser recognizes. Unrecognized messages may contain OTPs or PINs. This applies to live receive, file import and share or paste intake.
13. Money is BigDecimal only. Never use Double or Float for amounts or balances.
14. Parsers are pure functions with no Android imports and no IO. Sender patterns, regexes and date formats are named constants at the top of the parser file, never inline literals.
15. Every parser or feature ships with tests: one positive test per message template and one null test per known non-transaction message. Run the full suite and show the real output before calling a milestone done.
16. Do not invent. If a needed TransactionType, sender ID or format is missing or ambiguous, stop and ask. List every assumption made in the final report.

---

## 4. Architecture

```
app/
├── data/
│   ├── db/                   # Room database, DAOs, entities
│   ├── repository/           # TransactionRepository (single source of truth)
│   └── sync/                 # Firebase sync adapter (opt-in)
├── domain/
│   ├── model/                # Pure Kotlin data classes (Transaction, Summary, etc.)
│   └── usecase/              # One class per use case
├── parser/
│   ├── core/                 # SmsParser interface, ParseResult, ParserEngine
│   └── institutions/         # One file per institution (bkash.kt, nagad.kt, dbbl.kt, ...)
├── receiver/
│   └── SmsReceiver.kt        # BroadcastReceiver, delegates to ParserEngine only
├── ui/
│   ├── dashboard/             # Home screen: balance summary, recent transactions
│   ├── transactions/          # Full list with filter/search
│   ├── reports/                # Weekly / monthly charts
│   ├── settings/                # Cloud sync toggle, parser management
│   └── common/                   # Shared composables, theme
└── di/                             # Hilt modules
```

**Pattern:** MVVM + Repository + Use Cases (Clean Architecture lite)
**UI:** Jetpack Compose
**Async:** Kotlin Coroutines + StateFlow/SharedFlow
**DI:** Hilt
**DB:** Room (with explicit migrations)
**Sync:** Firebase Firestore (opt-in, behind a feature flag in settings)
**Testing:** JUnit5 + MockK for unit tests; Robolectric for Android unit tests; Espresso for UI smoke tests

---

## 5. Parser Contract

```kotlin
// parser/core/SmsParser.kt
interface SmsParser {
    val institutionName: String
    val senderPatterns: List<String>
    fun parse(sender: String, body: String, receivedAt: Long): ParseResult?
}
```

```kotlin
// parser/core/ParseResult.kt
data class ParseResult(
    val type: TransactionType,
    val amount: BigDecimal,
    val currency: String = "BDT",
    val balance: BigDecimal?,
    val counterparty: String?,
    val reference: String?,
    val rawSms: String,
)
```

`ParserEngine` iterates registered parsers, matches by sender, runs `parse()`, and returns the first non-null result. `SmsReceiver` and the historical-import job both go through `ParserEngine`; they never call institution parsers directly.

---

## 6. SMS Sender Reference

Use these patterns to route incoming SMS to the right parser. Expand as new institutions are verified on real devices.

| Institution | Sender address(es) | Notes |
|---|---|---|
| bKash | `bKash`, `01678600000` | Amount in BDT, TrxID present |
| Nagad | `NAGAD` | Verified in a third-party real SMS export, not on Sajib's own device. Cash In, Money Received and Payment supported; fields separated by newlines; promo text can precede the message. `16167` appears only as a helpline number in message bodies and is not used as a sender pattern |
| Rocket (DBBL Mobile) | unconfirmed, do not assume `16216` | "BDT" suffix per general knowledge only; `16216` was assumed to be Rocket's code but real data shows it is DBBL's own bank-alert sender for this account (see DBBL row). No real Rocket wallet sample exists, Sajib barely/never uses this wallet. If a real Rocket sample ever appears, confirm its actual sender before trusting any assumed code |
| Dutch-Bangla Bank (DBBL) | `16216` (confirmed via real data: balance inquiry, ATM-to-A/C transfer credit, NexusPay cash-out debit), `DUTCHB` | Priority bank, Sajib holds an account here, real samples confirmed |
| Sonali Bank | `SonaliBank`, `SBL` *(verify exact sender on device)* | State-owned bank; real samples seen so far are OTP/notification only, no real transaction SMS yet; priority bank, Sajib holds an account here |
| First Security Islami Bank (FSIBL) | `FSIBL` | Newly discovered via real data, not previously known; "Muhtaram" greeting, deposit-only real samples so far, no debit sample yet |
| City Bank | `CITY BANK` | One real deposit sample only, multiline body (uses literal newlines), no TrxID visible; needs more samples including a debit before building |
| BRAC Bank | `BRAC BANK` (with the space) | Verified in a real SMS export, not on device. Never use `BRAC` or `BRACB` as a pattern: the engine matches by substring and would catch `BRACBANK` |
| BRAC Bank notifications | `BRACBANK` (no space) | Notification only, reject. No parser may claim this sender |
| Uttara Bank | `UTTARA BANK` | Verified in a real SMS export, not on device. Balance can be negative |
| Pubali Bank | `PUBALI BANK` | Verified in a real SMS export, not on device. "Dr" before the balance is stored as a negative balance |
| Islami Bank | `IBBL`, `16259` | |
| UCB | `UCB`, `16419` | |
| Grameenphone | `GP`, `8008` | Recharge + data packs |
| Robi | `Robi`, `8444` | |
| Banglalink | `BL`, `9999` | |
| Teletalk | `TT`, `111` | |

> When adding a new parser, document the sender address and at least two sample SMS bodies in a comment at the top of the parser file.

---

## 7. What Agents Should NOT Do

- Do not add Kotlin Multiplatform (KMP) or iOS targets, out of scope.
- Do not add third-party ad SDKs or analytics SDKs.
- Do not store the user's phone number or device identifier in the database or cloud.
- Do not use `Thread.sleep()` or blocking calls on the main thread.
- Do not `catch (e: Exception)` silently, either handle meaningfully or let it propagate.
- Do not add a new library without checking if an existing dependency already covers the need.
- Do not modify a parser file that is not in your current task scope, parsers are independent units.

---

## 8. Running Tests

```bash
./gradlew test
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

All parser logic must have unit test coverage. A PR that adds a parser without tests will be rejected.

---

## 9. Adding a New Institution Parser, Checklist

- [ ] Create `parser/institutions/<name>.kt` implementing `SmsParser`
- [ ] Add at least 5 sample SMS strings as constants at the top of the file
- [ ] Write unit tests covering normal debit, normal credit, insufficient balance, balance inquiry, and at least one edge case
- [ ] Register the parser in `di/ParserModule.kt`
- [ ] Add the sender address to the table in this file
- [ ] Add an entry to `Roadmap.md` under the Parsers Backlog section