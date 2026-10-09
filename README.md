# VelisBank

A complete simulated banking application for a Java NC III final project. Built with Java 25, Spring Boot 4.1.1, Spring Security, Spring Data JPA, PostgreSQL, and a responsive HTML/CSS/JavaScript interface.

No real money is held or transferred. The scope is intentionally limited to customer accounts, deposits, withdrawals, transfers, transaction records, profiles, and administrative account management.

## Start on this computer

Prerequisites: Java 25+, Maven 3.9+, and PostgreSQL 18. These are already installed on the development computer.

From `C:\VelisBank` in PowerShell:

```powershell
    mvn package
    .\scripts\start.ps1
```

Open [VelisBank](http://localhost:8080).

The start script creates an isolated PostgreSQL cluster in `data/postgres` on **127.0.0.1:55432**, creates `velisbank` and `velisbank_test`, and runs the application. It does not modify the existing PostgreSQL service on port 5432. The generated database password is stored in the ignored `data/db-password.txt` file. Keep the `data` directory private and preserve it to retain your local records.

Stop the application with Ctrl+C. To stop only the project's database:

```powershell
.\scripts\stop-local-db.ps1
```

If port 8080 is occupied:

```powershell
.\scripts\start.ps1 -Port 8081
```

## Demo accounts

The local start script and the `demo` profile seed these accounts once:

- Customer: `joshua` / `Customer!2026` — starts with ₱12,500.00.
- Customer: `juan` / `Customer!2026` — starts with ₱6,500.00.
- Administrator: `admin` / `AdminDemo!2026`.

Account numbers are generated at setup. To transfer to Juan, log in as Juan and open **My account → Show details** and copy the full account number, then log in as Joshua to transfer. Administrators can also view the full number on customer details.

These credentials are for local demonstrations only. Default configuration does **not** seed customers or a default administrator password.

## Run without PostgreSQL

An optional file-backed H2 profile lets you demonstrate the same application without installing PostgreSQL:

```powershell
mvn package
java -jar target/velisbank-1.0.0.jar --spring.profiles.active=demo
```

Or use `.\scripts\start.ps1 -Mode demo`. H2 stores demo records in `data/velisbank.mv.db`. PostgreSQL and H2 use separate data stores.

## Use an existing PostgreSQL database

Create an empty database and a dedicated owner in your PostgreSQL installation. Then set your own values:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/velisbank'
$env:DB_USERNAME = 'velisbank'
$env:DB_PASSWORD = '<your database password>'
$env:ADMIN_USERNAME = 'admin'
$env:ADMIN_PASSWORD = '<your administrator password>'
java -jar target/velisbank-1.0.0.jar
```

On Linux/macOS, export the same variables before running Java. `PORT` optionally changes the HTTP port.

The administrator is created on the first startup where `ADMIN_PASSWORD` is provided and the username is unused. Passwords must contain at least 10 characters and fit within 72 UTF-8 bytes. Changing the environment variable later does not overwrite an existing administrator's password; use **Profile → Change password**.

Hibernate creates/updates the three application tables automatically. This keeps assessment setup simple; an externally hosted deployment should use reviewed schema migrations, TLS, protected secrets, and appropriate operational controls. This project is designed for learning and local simulated banking.

## Included screens and behavior

- Landing page with registration and login navigation.
- Registration with personal/account sections, field validation, and account-created confirmation.
- Session login, password visibility toggle, inline errors, and logout.
- Customer dashboard: balance, a flip card that reveals account details on request, quick actions, and the latest five transactions.
- My account with full account details.
- Deposit and withdrawal with a review step and digital receipt.
- Transfer with recipient lookup, balance validation, recipient name review, and final confirmation.
- Complete transaction history, incoming/outgoing summaries, type filters, search, and detail receipts.
- Read-only profile with edit dialog and password change. Password changes end the current session.
- Frozen-account notice; banking actions are disabled while account/history/profile remain readable.
- Admin dashboard with customer, active/frozen account, and completed operation counts.
- Searchable customer/account lists, status filter, detailed customer view, and freeze/unfreeze confirmation.
- Admin transaction search and type filters. A transfer produces two ledger entries sharing one reference; dashboard operation counts count it once.
- Mobile navigation, stacked cards, keyboard focus handling, processing states, warnings, and empty states.

## How the project is organized

- `src/main/java/com/velisbank/VelisBankApplication.java`: application entry point.
- `Customer`, `Account`, `BankTransaction`: database entities.
- The three repositories: database access and account locking.
- `BankService`: registration, account operations, profile updates, validation, and transaction boundaries.
- `BankController`: JSON API routes.
- `Requests`: validated input objects; `Views`: output objects that exclude password hashes.
- `SecurityConfig`: authentication, roles, CSRF protection, session logout.
- `ApiErrors`: readable API validation responses.
- `DataSeeder`: optional local sample data and administrator bootstrap.
- `src/main/resources/static`: the interface, styles, and local icons/fonts.
- `src/test/java/com/velisbank`: integration and concurrency tests.
- `scripts`: local database/start helpers and browser checks.

The browser calls the same-origin JSON API. Spring Security identifies the signed-in customer; the browser cannot choose which source account to debit. The service uses `BigDecimal` for exact arithmetic and commits the balances and ledger entries in one database transaction. Account rows are locked in numeric ID order to avoid opposing transfer deadlocks, then refreshed while locked so concurrent operations use current balances. Each operation carries a unique request key; repeating the same request returns its previous receipt instead of moving money again. Changing a completed request's payload is rejected.

A frozen account cannot send **or receive** money. The backend revalidates status and balance at confirmation, even if they changed after the review screen.

## Tests

Run all 16 backend tests against in-memory H2:

```powershell
mvn test
```

Run the same suite against the project's dedicated PostgreSQL test database:

```powershell
.\scripts\setup-local-db.ps1
$env:TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:55432/velisbank_test'
$env:TEST_DB_USERNAME = 'velisbank'
$env:TEST_DB_PASSWORD = (Get-Content data/db-password.txt -Raw).Trim()
mvn test
```

The test profile recreates its database tables. **Only point `TEST_DB_URL` at a disposable test database**, never your application database.

Tests cover exact balances, matching transfer ledger entries, insufficient funds, invalid amounts, self/missing-recipient transfers, frozen accounts, duplicate requests, concurrent withdrawals/transfers, password hashing, profile updates, account ownership, login, logout, role boundaries, and CSRF.

Optional browser checks, with the local seeded app running on port 8080:

```powershell
npm install
npx playwright install chromium
npm run test:ui
```

`BASE_URL` can select another local port. `CHROMIUM_EXECUTABLE` can point to an installed Chrome executable. Browser checks create a `qa...` customer and transfer ₱100 of simulated funds to Juan. Use a fresh demo database when you want pristine sample balances. Results and screenshots are written to `target/ui-checks`.

## Assessment walkthrough

1. Open the landing page and register a new account. Explain validation and the initial zero balance.
2. Log in; show the masked account number and empty activity state.
3. Deposit funds, review, confirm, and inspect the receipt.
4. Attempt a withdrawal larger than the balance; show the field error.
5. Complete a withdrawal and transfer to a second customer.
6. Filter transaction history and open the transfer reference.
7. Edit the profile and demonstrate password change.
8. Log in as admin, search for the customer, and freeze the account.
9. Return as the customer to show the frozen notice and disabled actions.
10. Unfreeze as admin and demonstrate restored banking access.

## Interface design

The interface uses an indigo-to-teal gradient theme, layered surfaces, a custom SVG VelisBank mark, and locally bundled fonts. Account cards, feature cards, activity headings, and administrator statistics link to their relevant screens. Desktop uses the gradient sidebar. Mobile uses a compact greeting, a prominent balance card, three colored money actions, and four fixed bottom destinations (dashboard, account, transactions, profile). Transfer stays accessible through its dashboard action. Admin mobile navigation retains its five existing destinations. Navigation remains in place during page changes; content, dialogs, buttons, and receipts use short transitions. The interface respects the operating system's reduced-motion preference.

The original SVG logo is `src/main/resources/static/vendor/velis-logo.svg`; the same mark is used for the application favicon.

Additional interface checks, with the app running:

```powershell
node scripts/ui-polish-check.cjs
node scripts/ui-responsive-check.cjs
node scripts/mobile-reference-check.cjs
```

These check card destinations, the logo, modal focus, reduced motion, and layouts from 320px to 1440px. They use the same Playwright installation and optional `CHROMIUM_EXECUTABLE` as the main browser checks.
## Dependencies and attribution

[Spring Boot documentation](https://docs.spring.io/spring-boot/) covers the framework used here. Lucide 0.468.0 icons are bundled locally under the ISC license. DM Sans and Manrope fonts are bundled locally under the SIL Open Font License. Their license files are in `src/main/resources/static/vendor`. The application interface needs no external asset requests at runtime.

If VelisBank was started in the background by the assistant, run `.\scripts\stop-app.ps1` to stop that recorded application process, then `.\scripts\stop-local-db.ps1` to stop its database.


The customer balance card starts with masked details. **Show details** flips it to account information; **Hide details** flips it back. The reverse face is excluded from keyboard and screen-reader navigation while hidden, and reduced-motion preferences disable the transition. Run `node scripts/flip-check.cjs` for the flip interaction checks.

Login protection: three unsuccessful password attempts for the same normalized username trigger a five-minute cooldown, enforced by the server across browser sessions. Failed attempts show the remaining count; blocked responses use HTTP 429 and Retry-After. Successful login resets the counter. Cooldowns are held in memory for this single-server demo and reset when the application restarts; a multi-instance deployment requires a shared persistent limiter. The browser remembers the countdown across reloads but is not the security enforcement point.

Transaction PIN: customers set or reset a six-digit PIN in Profile → Security using their current password. Transfers and withdrawals require the PIN at confirmation; deposits and login do not. PINs are BCrypt-hashed. Three incorrect PIN/password-verification attempts lock PIN operations for five minutes, with counters and expiry stored in PostgreSQL. There is no default PIN. Existing accounts must set one before their next transfer or withdrawal. Fresh demo accounts receive opening deposits only; no transaction bypass or shared demo PIN is used.

PIN flow update: Profile shows Set up PIN when no PIN exists and Reset PIN after setup. Setup asks for the current password, then uses a six-digit keypad for entry and confirmation. Every customer transaction, including deposits, now follows Enter details → Verify PIN → Review & confirm. Verification alone does not move money; the final endpoint checks the PIN again before committing funds.

PIN reset now verifies the account password first, then the current transaction PIN, before allowing a new PIN and its confirmation. The final server request also validates both current credentials. Wrong current credentials share the PIN cooldown; new-PIN reuse and confirmation mistakes do not increase that counter. First-time setup requires the account password only, because no current PIN exists.

## Mobile login update

Customers now use mobile number → SMS OTP → PIN for every login. First verification requires PIN creation; forgot/reset PIN also requires SMS verification. Customer registration no longer asks for a username or password. Administrator login stays available separately. See [mobile login and SMS setup](docs/MOBILE-LOGIN.md) for local testing and real SMS configuration. Earlier username/password customer examples and UI scripts document the previous flow; the new MobileAuthTest covers the replacement API flow.

### Isolated responsive layout checks

Run `npm run test:responsive` to check the current local interface without starting the backend or using customer credentials. These Playwright checks serve the workspace assets with synthetic API responses and never change account data. They cover 195 customer/admin route and viewport combinations from 320px through 1440px, including tablet breakpoint boundaries, filter selection, search, page overflow, and clipped action/filter/summary labels. Screenshots and results are saved to `target/tablet-checks`. This verifies presentation and browser interactions; it does not replace authentication or backend integration tests.

### Rebuild an already-running app on Windows

Use `.\scripts\rebuild-start.ps1` to stop the project JVMs, build with tests, and start only after a successful build. Windows locks a running JAR, so running `mvn package` while the packaged app is active can cause a rename failure and leave a non-executable JAR. The stop script handles both the Java launcher and its child JVM, without stopping PostgreSQL. For local SMS testing, set `$env:SMS_MODE = 'local'` before running the script.

Startup now copies the successful package to `data/runtime/velisbank-<port>.jar` and runs that copy, keeping Maven's `target` JAR unlocked. It also checks the port and executable manifest before launching. Use `scripts/rebuild-start.ps1` to stop existing project instances, build, and launch the new version; a plain build does not update an already-running app.
