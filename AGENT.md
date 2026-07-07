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
4. Write clean, well-commented Kotlin code. Every public function and class gets a KDoc comment.
5. Follow secure coding practices: never expose secrets (API keys, Firebase config, keystore passwords) in source control or logs. Always validate and sanitize SMS content before parsing, since it is untrusted input arriving from outside the app. When Firebase sync (M8) is implemented, enforce Firestore security rules scoped strictly to the authenticated user's own data.
6. Write scalable code for new work: keep business logic out of Composables and the UI layer (use the Repository and UseCase layers per the existing architecture), and never hardcode values that belong in `local.properties`, `BuildConfig`, or resource files.
7. Do not over-comment. Only comment where the code is not self-explanatory.
8. Log meaningful state changes for traceability, without violating Hard Constraint 1. Parser match events (institution name, timestamp, matched or not) may be logged; the raw SMS body itself must never appear in any log output, in debug or release builds.
9. No emojis and no em dashes anywhere: code comments, markdown files, commit messages, and all UI copy. Use plain punctuation only.

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
| Nagad | `Nagad`, `16167` | "Tk" prefix for amounts |
| Rocket (DBBL Mobile) | `Rocket`, `16216` | "BDT" suffix |
| Dutch-Bangla Bank (DBBL) | `DBBL`, `DUTCHB` | Debit/credit alerts; priority bank, Sajib holds an account here |
| Sonali Bank | `SonaliBank`, `SBL` *(verify exact sender on device)* | State-owned bank; SMS format not yet confirmed, may differ from private banks; priority bank, Sajib holds an account here |
| BRAC Bank | `BRACB` | Verify sender on device |
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