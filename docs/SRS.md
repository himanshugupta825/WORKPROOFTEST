# WorkProof — Software Requirements Specification (SRS)

| | |
|---|---|
| **Project** | WorkProof — Work Management and Proof-of-Work Verification System |
| **Type** | Academic PBL / mini-project, 7-week cycle |
| **Version** | 1.0.0 |
| **Stack** | Java 17 · JSP · Servlets (`javax.servlet`) · Apache Tomcat 9 · Maven (WAR) · MySQL 8 · JDBC · JSTL 1.2 · Bootstrap 5 |
| **Context path** | `/WorkProof` → `http://localhost:8080/WorkProof/` |

---

## 1. Purpose

Teams routinely claim that work is done without producing verifiable evidence. WorkProof closes that
gap: a member cannot mark work as finished on their own word — they must **submit proof**, and a
**different** authorised person must **accept or reject it with a written reason**. Every project,
task, submission, review and login is recorded so progress can be audited after the fact.

## 2. Scope

**In scope.** User accounts and roles; projects and membership; tasks and their workflow;
proof-of-work submission with file uploads; review/verification; notifications; activity logging;
reports with CSV export; role dashboards.

**Out of scope (deliberate, and stated in the viva).** Email/SMS delivery (notifications are in-app),
chat, real-time collaboration, mobile apps, OAuth/SSO, payment, multi-tenancy. Version control
integration is replaced by file upload + textual proof of work.

## 3. Actors

| Actor | Description | Primary goals |
|---|---|---|
| **Administrator** | Department/system owner. | Manage accounts and roles, monitor activity, read any report. |
| **Project Manager** (Team Leader) | Owns a project. | Plan projects and tasks, assign work, track progress, verify evidence. |
| **Team Member** | Does the work. | See assigned work, update status, upload evidence, react to feedback. |
| **Reviewer / Mentor** | Independent verifier. | Audit evidence, approve or reject with feedback, monitor quality. |
| *(Secondary)* **Scheduled job** | System actor, no login. | Raise deadline reminders, never twice for the same day. |

## 4. Functional requirements

Priority: **M** = must have, **S** = should have.

### FR-1 Authentication and accounts

| ID | Requirement | Pri |
|---|---|---|
| FR-1.1 | A visitor may register as Manager, Member or Reviewer. Administrator accounts are created only by an administrator. | M |
| FR-1.2 | Passwords are stored only as BCrypt hashes (cost 12). Plaintext passwords are never stored or logged. | M |
| FR-1.3 | Password policy: at least 8 characters, at most 72 bytes (BCrypt limit), containing at least one letter and one digit. | M |
| FR-1.4 | Login failures return one generic message that does not reveal whether the email exists. | M |
| FR-1.5 | The session identifier is rotated on successful login (session-fixation defence) and a fresh CSRF token is issued. | M |
| FR-1.6 | Logout is a POST request that invalidates the session; a GET to `/logout` never logs anybody out. | M |
| FR-1.7 | An administrator may create users, edit their role/details, activate or deactivate them, and reset a password. | M |
| FR-1.8 | The last remaining active administrator cannot be deactivated or demoted. | M |
| FR-1.9 | A deactivated account is refused at login. | M |

### FR-2 Role-based access control

| ID | Requirement | Pri |
|---|---|---|
| FR-2.1 | Four global roles exist: ADMIN, MANAGER, MEMBER, REVIEWER. | M |
| FR-2.2 | Authorisation is enforced **server-side** for every request; hidden UI is a convenience, never the control. | M |
| FR-2.3 | A member who requests an administrator page directly receives HTTP 403. | M |
| FR-2.4 | In-project roles (MANAGER / MEMBER / REVIEWER) scope access to individual projects: a user may manage one project and merely participate in another. | M |
| FR-2.5 | Tasks, submissions and reviews are visible only to their project's members, its reviewers and administrators. | M |

### FR-3 Dashboards

| ID | Requirement | Pri |
|---|---|---|
| FR-3.1 | Every role sees a dashboard built entirely from live queries. No metric is hard-coded. | M |
| FR-3.2 | A member sees their own tasks by status, deadlines and recent activity; a manager sees project health; a reviewer sees the verification queue; an admin sees system-wide totals. | M |
| FR-3.3 | The status distribution is shown as a chart rendered from server-provided data (no external chart CDN; the page works offline). | S |

### FR-4 Project management

| ID | Requirement | Pri |
|---|---|---|
| FR-4.1 | A manager or administrator can create a project with a unique code, description and date window. | M |
| FR-4.2 | Project codes are unique, upper-cased, 2–20 characters of letters, digits and hyphens. | M |
| FR-4.3 | End date cannot precede start date. | M |
| FR-4.4 | Managers/admins can edit a project, archive it, restore it, and manage its members and their in-project roles. | M |
| FR-4.5 | A member or reviewer cannot create or edit projects (server-side refusal). | M |
| FR-4.6 | The project page shows members, tasks, progress, upcoming deadlines and recent activity. | S |

### FR-5 Task management and workflow

| ID | Requirement | Pri |
|---|---|---|
| FR-5.1 | Tasks can be created, edited and deleted by a project manager/administrator, with title, description, priority, dates and one or more assignees. | M |
| FR-5.2 | Task status follows exactly this lifecycle: TODO → IN_PROGRESS → SUBMITTED → IN_REVIEW → APPROVED → COMPLETED, with REJECTED → (IN_PROGRESS \| SUBMITTED) and reopening from COMPLETED. | M |
| FR-5.3 | Illegal transitions are refused **server-side** (for example TODO → COMPLETED), and the UI only offers the legal next states. | M |
| FR-5.4 | A member may move only tasks assigned to them, and only within TODO / IN_PROGRESS / SUBMITTED / IN_REVIEW. | M |
| FR-5.5 | A manager may complete a task only after it is APPROVED. | M |
| FR-5.6 | Task lists support search, status/priority/project filters, sorting and pagination. | M |
| FR-5.7 | Overdue tasks are flagged by comparing `due_date` with the current date — never by a stored flag. | S |

### FR-6 Proof-of-work submission

| ID | Requirement | Pri |
|---|---|---|
| FR-6.1 | An assignee can submit a title, a written description and up to 5 files for a task. | M |
| FR-6.2 | File types are validated server-side by extension **and** magic bytes. Executables and server pages (`.exe`, `.jsp`, `.sh`, …) are refused. | M |
| FR-6.3 | Size limits: 10 MB per file, enforced by the servlet's multipart configuration as well as the service layer. | M |
| FR-6.4 | Stored names are generated by the server (`<uploadRoot>/<yyyy>/<MM>/<uuid>.<ext>`); the browser-supplied name is never used as a path. | M |
| FR-6.5 | Files are stored in a directory **outside** the deployed web application, and each file's SHA-256 is recorded. | M |
| FR-6.6 | A submission without files is accepted only with a description of at least 40 characters. | M |
| FR-6.7 | Resubmission after rejection creates a new version (`version + 1`); earlier versions and their reviews are kept forever. | M |
| FR-6.8 | Only one submission per task may be awaiting review: uploading again while a review is pending is refused. An older *waiting* version that is replaced (after the task is reopened) becomes SUPERSEDED instead of silently lingering in the queue. | M |
| FR-6.9 | Files may be downloaded only by their submitter, a member of that project, its reviewer, or an administrator; each download is written to the activity log. | M |

### FR-7 Verification and review

| ID | Requirement | Pri |
|---|---|---|
| FR-7.1 | Reviewers see a queue of submissions awaiting verification, scoped to the projects they review. | M |
| FR-7.2 | A reviewer records APPROVED or REJECTED with comments. | M |
| FR-7.3 | A rejection **requires** feedback of at least 10 characters; approval feedback is optional. | M |
| FR-7.4 | Nobody may review their own submission (self-approval is refused even for an administrator). | M |
| FR-7.5 | Only a PENDING submission can be decided; a decided submission cannot be re-decided, so history cannot be rewritten. | M |
| FR-7.6 | A decision atomically updates the review, the submission status and the task status in one transaction. | M |
| FR-7.7 | The decision is delivered to the submitter through a notification and the activity log. | M |

### FR-8 Notifications and activity log

| ID | Requirement | Pri |
|---|---|---|
| FR-8.1 | In-app notifications are raised for assignment, submission, review decision, project membership and approaching deadlines. | M |
| FR-8.2 | Duplicate notifications are prevented by a unique `(user_id, dedupe_key)` constraint (a deadline reminder is sent at most once per task per day per threshold). | M |
| FR-8.3 | A user sees only their own notifications, can mark one or all as read, and can delete their own. | M |
| FR-8.4 | A failure in notification delivery must never fail the underlying business action. | M |
| FR-8.5 | Every important action is appended to `activity_logs` with actor, action, entity, details, IP address and timestamp. | M |
| FR-8.6 | The activity log is readable by administrators (and per-record history by the people allowed to see that record). | M |
| FR-8.7 | The log is append-only from the application: no user-facing feature edits or deletes entries. | M |

### FR-9 Reports and export

| ID | Requirement | Pri |
|---|---|---|
| FR-9.1 | Five reports are available: project progress, task completion, team contribution, pending verification, rejected submissions. | M |
| FR-9.2 | Reports can be filtered by project, member, status and a start/end date range. | M |
| FR-9.3 | Report results are capped at a documented row count (2,000 on screen, 5,000 for export) and the screen says so when the cap is hit. | S |
| FR-9.4 | Any report can be exported as CSV with a UTF-8 BOM so Excel renders accents correctly. | M |
| FR-9.5 | CSV cells beginning with `=`, `+`, `-` or `@` are prefixed with `'` to prevent formula injection. | M |
| FR-9.6 | Members see their own contribution; managers see their projects; administrators see everything. | M |

### FR-10 User interface

| ID | Requirement | Pri |
|---|---|---|
| FR-10.1 | Consistent application shell: sidebar, top bar with notification bell and profile menu, breadcrumbs, page titles. | M |
| FR-10.2 | Bootstrap 5 is served from the application itself (vendored), so the demo works without internet. | M |
| FR-10.3 | Status is conveyed with colour **and** text/icon, never colour alone. | S |
| FR-10.4 | Empty states explain what to do next; forms show validation errors next to the fields. | S |
| FR-10.5 | Layout is responsive down to a phone-sized viewport; the sidebar collapses on small screens. | S |
| FR-10.6 | Accessibility: labelled inputs, `aria-label` on icon-only controls, visible focus, semantic tables, skip-to-content link. | S |
| FR-10.7 | No inline JavaScript and no inline event handlers: behaviour is attached from `workproof.js` via `data-*` hooks, so the Content-Security-Policy stays strict. | M |

## 5. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Security | Passwords hashed with BCrypt; all SQL through `PreparedStatement`; CSRF token on every state-changing request; output escaped with `<c:out>`; CSP, `X-Content-Type-Options`, `X-Frame-Options` and `Referrer-Policy` on every response; session cookie `HttpOnly`. |
| NFR-2 | Security | No stack trace, SQL fragment or credential is ever shown to a user; detailed errors are logged server-side only. Errors are rendered from dedicated views (400/403/404/500). |
| NFR-3 | Security | Secrets live in environment variables or a gitignored properties file; `workproof.properties` in the repository contains safe defaults only. |
| NFR-4 | Architecture | Strict layering: `controller → service → dao → model`. JSPs render only; no SQL and no business rules in any JSP. |
| NFR-5 | Architecture | Cross-cutting concerns live in filters (encoding, security headers, authentication, CSRF) declared in `web.xml`; the views sit under `WEB-INF` so they can never be requested directly. |
| NFR-6 | Integrity | Multi-table changes (project + owner membership, submission + files + task status, review + submission + task) run in a single transaction and roll back as a unit. |
| NFR-7 | Performance | Connection pooling (HikariCP, 10 connections), paginated list queries, N+1-free enrichment of assignees/files/reviewers, statement caching. Typical page response stays well under 300 ms locally. |
| NFR-8 | Reliability | Server-side validation on every field; malformed input (bad id, bad date, unknown enum, out-of-range page) produces a friendly 400/404 page, never a 500. |
| NFR-9 | Usability | A new user should complete "register → see dashboard → open a task → submit evidence" without training; every action confirms its result with a flash message. |
| NFR-10 | Maintainability | The workflow rule exists once (`TaskStatus.allowedNext()`), authorisation once (`Permissions`), configuration once (`AppConfig`). Javadoc on every non-trivial class; 80 unit tests covering the rules that are easy to break. |
| NFR-11 | Portability | Runs on JDK 17 with Tomcat 9 and MySQL 8 (x86-64 Linux, Windows and macOS); `javax.servlet` only. |
| NFR-12 | Traceability | Every requirement in this document maps to a page, an endpoint and a test in `TEST-CHECKLIST.md`. |

## 6. Assumptions and constraints

* MySQL is installed and reachable on `localhost:3306`; the schema is created by `schema.sql`.
* The upload directory is a real path on the server with write permission for the Tomcat user and is
  **not** inside `webapps/`.
* Single instance, single database: no clustering, no distributed cache.
* Time is the server's local time zone; dates are stored as `DATE`, timestamps as `DATETIME`.
* The evaluator/demo scenario is driven by `database/seed.sql`.

## 7. Traceability summary

| Requirement group | Implemented in | Verified by |
|---|---|---|
| FR-1 | `AuthService`, `LoginServlet`, `RegisterServlet`, `LogoutServlet`, `AdminUserServlet` | `PasswordUtilTest`, `ValidationUtilTest`, smoke checks `AUTH`, `ADMIN MODULE` |
| FR-2 | `Permissions`, `AuthenticationFilter`, service-layer guards | smoke `CSRF / RBAC (negative)` |
| FR-3/10 | `DashboardService/DAO`, JSPs, `workproof.css/js` | smoke page checks for all four roles |
| FR-4 | `ProjectService/DAO`, `Project*Servlet` | flow `CREATE PROJECT`, `PROJECT MEMBERS`, `DUPLICATE / VALIDATION GUARDS` |
| FR-5 | `TaskService/DAO`, `TaskStatus` | `TaskStatusTest`, flow `MEMBER MOVES TASK STATUS` |
| FR-6 | `SubmissionService/DAO`, `FileStorageUtil`, `FileDownloadServlet` | `FileStorageUtilTest`, flow `MEMBER SUBMITS PROOF`, `AUTHORISED DOWNLOAD`, `ONE LIVE SUBMISSION PER TASK` |
| FR-7 | `ReviewService/DAO`, `ReviewDecisionServlet` | flow `REVIEWER APPROVES`, `REJECTION + RESUBMISSION` |
| FR-8 | `NotificationService/DAO`, `ActivityService/DAO`, `AppContextListener` | flow notification assertions, `admin/activity` page check |
| FR-9 | `ReportService/DAO`, `ReportServlet`, `ReportExportServlet` | `WebUtilCsvTest`, flow `FILTERS / SEARCH / EXPORT` |
| NFR-8 | `BaseServlet` error mapping, error views | flow `EDGE CASES` |

## 8. Future enhancements

1. Email digest of pending verifications (queue + scheduled job already exist).
2. Rubric/weighted scoring per task instead of a binary decision.
3. Git commit hook that attaches a diff as evidence automatically.
4. Bulk reviewer actions and reviewer assignment rules (round-robin per project).
5. Two-factor authentication for administrators, and an OWASP ZAP pass in CI.
6. i18n resource bundles (all user-facing strings are already in the JSPs, not in Java code).
