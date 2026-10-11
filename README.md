# WorkProof — Work Management & Proof-of-Work Verification System

WorkProof is a server-rendered Java web application that manages project work **and verifies the
evidence** produced for it. Teams create projects and tasks, members upload proof of work
(screenshots, documents, logs), and reviewers approve or reject that proof with mandatory written
feedback. Every important action is written to an activity log, and reviewers/submitters are
notified in the application.

Built for the 7-week PBL / mini-project cycle: Java 17 · JSP · Servlets · Tomcat 9 · Maven (WAR) ·
MySQL 8 · JDBC · JSTL · Bootstrap 5.

Expected URL after deployment: **http://localhost:8080/WorkProof/**

---

## 1. Roles and what each can do

| Role | Can do |
|---|---|
| **ADMIN** | Everything: manage users (create, edit, activate/deactivate, reset password), see all projects/tasks/submissions, read the full activity log, run every report. The last active admin cannot be deactivated or demoted. |
| **MANAGER** (Project Manager / Team Leader) | Create and edit projects, archive/restore them, add/remove members and set their in-project role, create/assign/edit/delete tasks, move any task through the workflow, complete approved tasks, run reports, export CSV. Cannot approve their own submission. |
| **MEMBER** | See the projects they belong to and the tasks assigned to them, move their own tasks through TODO → IN_PROGRESS → SUBMITTED, upload evidence files, resubmit after a rejection, read notifications, track their own progress. |
| **REVIEWER** (Mentor) | See the verification queue for projects they review, open a submission together with the submitter's description and files, then APPROVE or REJECT it. A rejection requires written feedback of at least 10 characters. |

Permissions are enforced **on the server** (`service/Permissions` + the service layer), not by
hiding buttons. A member who calls `/admin/users` directly receives 403.

## 2. The eight functional modules

1. **Authentication** — register (Managers/Members/Reviewers), login with BCrypt password check,
   session rotation on login (session-fixation defence), POST-only logout, CSRF token on every form.
2. **Role dashboards** — one dashboard built from real aggregate SQL for each role: task counts,
   status distribution chart, upcoming deadlines, recent activity, my open tasks.
3. **Project management** — CRUD, archive/restore, member management with in-project roles.
4. **Task management** — CRUD, multi-assignee assignment, priorities, due dates, and a status
   workflow whose legal transitions live in one place (`TaskStatus.allowedNext()`).
5. **Proof-of-work submission** — multipart upload, server-side type + size validation, safe
   generated filenames, files stored outside the web root, SHA-256 recorded, versioned resubmission.
6. **Verification / review** — pending queue, decision with optional/required feedback, no
   self-approval, only PENDING submissions are decidable, full version + review history preserved.
7. **Notifications & activity log** — in-app notifications (deduplicated) plus an append-only
   activity log of who did what, when and from which IP.
8. **Reports** — five reports with date/status/project filters, truncated at a documented row cap,
   and CSV export with a UTF-8 BOM and formula-injection protection.

## 3. Technology and versions

| Layer | Choice | Version |
|---|---|---|
| Language / JDK | Java (release 17) | 17 |
| Web | Servlet API (`javax.servlet`), JSP, JSTL | 4.0.1 / 2.3.3 / 1.2 |
| Server | Apache Tomcat | 9.0.x |
| Build | Maven WAR (`finalName=WorkProof`) | 3.9.x |
| Database | MySQL | 8.x (also runs on MariaDB 10.6+) |
| Connection pool | HikariCP | 5.1.0 |
| Driver | mysql-connector-j | 8.0.33 |
| Passwords | jBCrypt (cost 12) | 0.4 |
| Logging facade | slf4j | 2.0.13 |
| Front end | Bootstrap 5 + Bootstrap Icons, vendored locally (no CDN) | 5.3.3 / 1.11.3 |

> **javax, not jakarta.** Tomcat 9 implements `javax.servlet`. Do not upgrade these dependencies to
> the `jakarta.*` namespaces unless you also move to Tomcat 10+/Jakarta EE 9+.

## 4. Prerequisites

```bash
java -version        # 17.x
mvn -v               # 3.9.x
mysql --version      # 8.x
echo $CATALINA_HOME  # /opt/tomcat (a Tomcat 9 install)
```

If any are missing, see `docs/VSCODE-SETUP.md` (Ubuntu + VS Code walkthrough, per-milestone
commands, common errors).

## 5. Quick start

### 5.1 Create the database

```bash
mysql -u root -p < database/schema.sql     # database + 11 tables + view, idempotent
mysql -u root -p workproof < database/seed.sql   # demo data (destroys existing rows)
```

Then create the least-privilege application user (never use root from the web app):

```sql
CREATE USER 'workproof_app'@'localhost' IDENTIFIED BY 'a-strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON workproof.* TO 'workproof_app'@'localhost';
FLUSH PRIVILEGES;
```

### 5.2 Point the application at your database

Configuration is resolved in this order: **environment variable → JVM `-D` → bundled
`workproof.properties` → gitignored `workproof-local.properties`**.

Copy `scripts/tomcat-setenv.sh.example` to `$CATALINA_HOME/bin/setenv.sh`, edit it, and make it
executable. Tomcat sources it on start:

```bash
cp scripts/tomcat-setenv.sh.example $CATALINA_HOME/bin/setenv.sh
chmod +x $CATALINA_HOME/bin/setenv.sh
# now edit $CATALINA_HOME/bin/setenv.sh and put in your own database password
```

Alternative for local IDE work: `cp src/main/resources/workproof-local.properties.example
src/main/resources/workproof-local.properties` and fill it in. That file is gitignored, so no
password ever reaches GitHub.

### 5.3 Build and deploy

```bash
mvn clean package                 # produces target/WorkProof.war
cp target/WorkProof.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh     # Linux/macOS     (bin\startup.bat on Windows)
```

Watch the log — the application prints a start-up banner and a database check:

```
==========================================================
 WorkProof starting up
 Database URL  : jdbc:mysql://localhost:3306/workproof...
 Database user : workproof_app
 Password      : ******** (configured)
 Upload folder : /var/lib/workproof/uploads
 Session (min) : 30
 Self-review   : BLOCKED
----------------------------------------------------------
 Upload folder is ready.
 Database connection: OK
==========================================================
```

Then open **http://localhost:8080/WorkProof/** and sign in.

### 5.4 Demo accounts (from `database/seed.sql`)

| Email | Password | Role |
|---|---|---|
| `admin@workproof.local` | `Admin@123` | ADMIN |
| `priya.manager@workproof.local` | `Manager@123` | MANAGER |
| `aarav.member@workproof.local` | `Member@123` | MEMBER |
| `ishita.member@workproof.local` | `Member@123` | MEMBER |
| `rohan.member@workproof.local` | `Member@123` | MEMBER |
| `mentor.reviewer@workproof.local` | `Reviewer@123` | REVIEWER |
| `kavya.reviewer@workproof.local` | `Reviewer@123` | REVIEWER |

Passwords are stored as BCrypt hashes (cost 12) — never in plain text. Change them before any real
use.

## 6. Project layout

```
WorkProof/
├── pom.xml                          Maven WAR build
├── database/
│   ├── schema.sql                   DDL: 11 tables + v_project_progress view
│   └── seed.sql                     demo data with real BCrypt hashes
├── docs/                            setup guide, ERD, SRS, test checklist, 7-week plan
├── scripts/
│   └── tomcat-setenv.sh.example     environment template for Tomcat
└── src/main/
    ├── java/com/workproof/
    │   ├── controller/   27 servlets + BaseServlet (thin: read request → call service → render view)
    │   ├── service/      11 classes: business rules, transactions, Permissions (RBAC)
    │   ├── dao/          11 classes: all SQL, BaseDao helpers, prepared statements only
    │   ├── model/        14 models + 9 enums + PagedResult
    │   ├── filter/       4 filters: encoding, security headers, authentication, CSRF
    │   ├── listener/     start-up checks + notification scheduler
    │   ├── util/         AppConfig, DataSourceProvider, PasswordUtil, FileStorageUtil, …
    │   └── exception/    5 typed exceptions
    ├── resources/
    │   ├── workproof.properties               safe defaults (no secrets)
    │   └── workproof-local.properties.example copy for your own password
    └── webapp/
        ├── index.jsp          public landing page
        ├── assets/            css, js, img, vendored Bootstrap + icons (offline)
        └── WEB-INF/
            ├── web.xml        filters, session config, error pages
            ├── tlds/workproof.tld     wp: EL functions (dates, initials, percent, …)
            └── views/         23 JSPs + 6 shared fragments
```

## 7. Security features (implemented, not just claimed)

| Area | Implementation |
|---|---|
| Passwords | BCrypt cost 12, ≥ 8 chars with a letter and a digit; generic login error message |
| Session fixation | `request.changeSessionId()` on successful login; new CSRF token is issued |
| CSRF | Per-session 256-bit token in every POST form, compared in constant time; mismatch → 403 |
| SQL injection | Every statement is a `PreparedStatement`; sort columns come from a whitelist |
| XSS | All user-controlled output escaped with `<c:out>`; CSP `script-src 'self'` with no inline scripts or `on*=` handlers |
| File uploads | Extension **and** magic-byte check, 10 MB limit per file, ≤ 5 files, random UUID filenames, stored outside the web root, SHA-256 recorded |
| File downloads | Every download re-checks that the requester is the submitter, an in-project member or an admin, and writes an audit entry |
| Authorisation | Single `Permissions` class, enforced in the service layer; UI hiding is only cosmetic |
| Error handling | Users see friendly 400/403/404/500 pages; stack traces only in the Tomcat log; no credentials or SQL reach the browser |
| Security headers | Content-Security-Policy, X-Content-Type-Options, X-Frame-Options, Referrer-Policy on every response |
| Secrets | No password in source control: environment variables or a gitignored local properties file |

## 8. Verified behaviour

The application was built with `mvn clean package` and deployed to a real Tomcat 9.0.107 instance
against a live MySQL-compatible server, then exercised over HTTP.

* **`mvn test` — 80 unit tests, 0 failures** covering the rules that are easiest to break: the task
  state machine, BCrypt hashing, CSRF token generation/comparison, upload extension and path safety,
  CSV formula-injection escaping, validation policy and the paging maths.
* **176 HTTP checks across two shipped scripts, 0 failures** (`docs/tools/smoke.sh` and
  `docs/tools/flow.sh`, both re-runnable against your own deployment):

  * **82 page and security checks** — anonymous redirects, all four role logins, every page for
    every role (200), 403 for members on admin and reviewer pages, CSRF rejection with a missing or
    wrong token, stale-token replay after login, security headers, HttpOnly session cookie,
    404 handling.
  * **94 workflow checks** — project → members → task → status workflow (including a refused illegal
    transition) → multi-file upload → reviewer approval → task completion → authorised download;
    rejection with mandatory feedback and resubmission as version 2 with the history preserved;
    superseding a submission that was replaced while waiting for review; duplicate-code and weak
    password rejection; admin user creation with a BCrypt hash that can log in; filters, reports,
    CSV export; and a set of malformed-input edge cases that must never return 500.

Coverage of the same list by hand — exactly as it will be demonstrated in the viva — is in
`docs/TEST-CHECKLIST.md`.

## 9. Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `Access denied for user 'workproof_app'@'localhost'` at start-up | Wrong password in `setenv.sh`, or the grant was not run. Re-check section 5.1. |
| `Public Key Retrieval is not allowed` | MySQL 8 uses `caching_sha2_password`; keep `allowPublicKeyRetrieval=true` in the JDBC URL (or switch the user to `mysql_native_password`). |
| `Unknown database 'workproof'` | `schema.sql` was not imported. Import it, then `seed.sql`. |
| Login works but every page shows "Please sign in" | Cookies are blocked, or the clock set `Secure` on a plain-HTTP cookie. Keep `secure=false` for local HTTP. |
| The 500 page appears after deploying only `target/WorkProof/` | Deploy the WAR, or copy the exploded directory *including* `WEB-INF/lib`. |
| Uploads fail with "storage directory is not writable" | The `WORKPROOF_UPLOAD_DIR` directory does not exist or Tomcat's user cannot write to it. |
| Tomcat log: `ClassNotFoundException: com.mysql.cj.jdbc.Driver` | `mysql-connector-j` is missing from `WEB-INF/lib` — rebuild with `mvn clean package`. |
| Port 8080 already in use | `sudo lsof -i :8080` and stop the other process, or change the port in `conf/server.xml`. |

## 10. Documentation map

The `docs/` folder is filled by the documentation step of this project (see the list below); the
README, `database/` and `scripts/` folders are complete as shipped.

| File | Contents |
|---|---|
| `docs/VSCODE-SETUP.md` | Ubuntu + VS Code setup, per-milestone build/test commands |
| `docs/ERD.md` | Entity-relationship diagram (Mermaid) for all 11 tables |
| `docs/SRS.md` | Software requirements specification: actors, use cases, functional + non-functional requirements |
| `docs/TEST-CHECKLIST.md` | Manual test checklist matching the automated suites |
| `docs/PROJECT-PLAN.md` | 7-week milestone plan with deliverables and demo script |

## 11. License / academic use

Written as a college project (PBL) deliverable. Reuse and adapt freely for coursework; keep the
attribution if you submit it as your own.

---

*(The bundled `database/seed.sql` demo scenario is fictional test data: two projects, nine tasks,
three approved and two open submissions, and seven user accounts. It contains no real personal
data.)*
