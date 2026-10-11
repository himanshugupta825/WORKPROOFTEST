# WorkProof — 7-Week Project Plan

A 7-week build plan for a two-to-three person PBL team. Each week ends with something **demonstrable**
— never "in progress". The columns say what to build, what to show, and how to prove it works.

> The repository ships in its **final state** (all seven weeks complete). Use this plan either as the
> report's project schedule, or as the order in which to re-build and re-explain the system during
> the viva.

---

## Week 1 — Requirements, architecture, database

**Build**

| Task | Output |
|---|---|
| Freeze requirements and actors | `docs/SRS.md` (actors, use cases, functional + non-functional requirements) |
| Choose the stack and pin versions | `pom.xml` with Java 17 / Servlet 4.0.1 / JSTL 1.2 / mysql-connector-j 8.0.33 / HikariCP / jBCrypt |
| Lay out the packages | `controller → service → dao → model` skeleton, `util`, `filter`, `listener`, `exception` |
| Design the database (3NF) | `database/schema.sql`: 11 tables + `v_project_progress`, unique keys, FK delete rules, indexes |
| Write the seed scenario | `database/seed.sql` with real BCrypt hashes |

**Milestone / demo:** `mysql < database/schema.sql && mysql workproof < database/seed.sql` runs, and
the ERD (`docs/ERD.md`) can be explained table by table.

**Evidence for the report:** ERD, requirement list, schema file, seed file.

---

## Week 2 — Foundation: configuration, models, DAO layer

**Build**

| Task | Output |
|---|---|
| Configuration utility (env → `-D` → properties → local file) | `util/AppConfig` |
| HikariCP connection pool + start-up DB check | `util/DataSourceProvider`, `listener/AppContextListener` |
| Password hashing and validation rules | `util/PasswordUtil`, `util/ValidationUtil` |
| Domain models and enums (workflow rules included) | 14 models, 9 enums, `PagedResult` |
| Generic JDBC helpers (no boilerplate, prepared statements only) | `dao/BaseDao` with `list/one/update/insert/count/tx` and `RowMapper` |
| DAOs for every table | 11 DAO classes, all SQL centralised here |

**Milestone / demo:** the app deploys, prints its start-up banner, reports **Database connection: OK**,
and a DAO smoke call returns seeded rows.

**Evidence:** generated start-up log, `mvn compile` output, DAO class list.

---

## Week 3 — Security, session handling and the application shell

**Build**

| Task | Output |
|---|---|
| Filters in the right order | `EncodingFilter` → `SecurityHeadersFilter` → `AuthenticationFilter` → `CsrfFilter` (declared in `web.xml`) |
| CSRF protection | `util/CsrfUtil` + one `<input type="hidden" name="_csrf">` in every form |
| Session-fixation defence and POST-only logout | `LoginServlet` / `LogoutServlet` |
| Error pages without stack traces | `errors/400,403,404,500` + `ErrorServlet` / `AccessDeniedServlet` |
| UI shell, vendored Bootstrap, custom CSS/JS | `webapp/index.jsp`, `assets/**`, `WEB-INF/views/fragments/**` |

**Milestone / demo:** the landing page and login page render; a POST without a CSRF token returns 403;
accessing `/dashboard` signed out redirects to `/login`.

**Evidence:** screenshots of the 403 page, session rotation proof (token replay rejected).

---

## Week 4 — Module 1 & 2: authentication and role dashboards

**Build**

| Task | Output |
|---|---|
| Register / login / logout, BCrypt verification, generic error message | `AuthService`, `LoginServlet`, `RegisterServlet` |
| RBAC in one place | `service/Permissions` (global role + in-project role) |
| Four role dashboards from live SQL | `DashboardService/DAO`, `views/dashboard/index.jsp` |
| Notification bell and unread badge | `NotificationService/DAO`, `sessionScope.unreadNotifications` |

**Milestone / demo:** log in as each of the four seeded roles and show four different dashboards whose
counters match the database.

**Evidence:** `mvn test` green (`PasswordUtilTest`, `ValidationUtilTest`), dashboard screenshots.

---

## Week 5 — Module 3 & 4: projects, membership, tasks, workflow

**Build**

| Task | Output |
|---|---|
| Project CRUD, archive/restore, member management | `ProjectService/DAO`, `Project*Servlet`, `projects/*.jsp` |
| Task CRUD, multi-assignee, filters, sorting, paging | `TaskService/DAO`, `Task*Servlet`, `tasks/*.jsp` |
| Status workflow enforced server-side | `TaskStatus.allowedNext()` + `TaskService.changeStatus` |
| Activity logging for every change | `ActivityService/DAO` |

**Milestone / demo:** create a project, add members, create and assign a task, move it through
TODO → IN_PROGRESS → SUBMITTED, and show that an illegal jump is refused.

**Evidence:** `TaskStatusTest` (16 tests), activity-log screenshot, `flow.sh` section
*MEMBER MOVES TASK STATUS*.

---

## Week 6 — Module 5 & 6: evidence upload and verification (the core idea)

**Build**

| Task | Output |
|---|---|
| Multipart upload with type/size validation, generated names, storage outside the web root | `FileStorageUtil`, `SubmissionService/DAO`, `SubmissionFormServlet` |
| Authorised download with audit entry | `FileDownloadServlet` |
| Review queue, decision, mandatory rejection feedback, no self-approval | `ReviewService/DAO`, `Review*Servlet` |
| Versioned resubmission with preserved history | `(task_id, version)` + `SUPERSEDED` handling |

**Milestone / demo:** member uploads two files → reviewer approves → task completes; then reject with
feedback → member resubmits as version 2 → both versions and both reviews are still visible.

**Evidence:** `FileStorageUtilTest`, uploaded files on disk showing UUID names, `flow.sh` sections
*MEMBER SUBMITS PROOF*, *REVIEWER APPROVES*, *REJECTION + RESUBMISSION*.

---

## Week 7 — Module 7 & 8, polish, documentation, release

**Build**

| Task | Output |
|---|---|
| Notifications (with dedupe) and the deadline reminder job | `NotificationService`, scheduler in `AppContextListener` |
| Admin console (users + activity log) | `AdminUserServlet`, `AdminActivityServlet` |
| Five reports with filters and CSV export | `ReportService/DAO`, `ReportServlet`, `ReportExportServlet` |
| Profile page, error pages, empty states, responsive pass | `profile/index.jsp`, UI polish |
| Documentation and packaging | `README.md`, `docs/**`, ZIP |

**Milestone / demo:** the full 10-minute demo script (below) with the automated suites green.

**Evidence:** `mvn test` 80/80, `smoke.sh` 82/82, `flow.sh` 94/94, CSV export file, final ZIP.

---

## Roles in a 3-person team

| Role | Owns |
|---|---|
| **Backend / DB** | Schema, `BaseDao`, DAOs, transactions, `AppConfig`, connection pool |
| **Backend / security + modules** | Filters, `Permissions`, auth, submissions/reviews, notifications, reports |
| **Frontend / docs / test** | JSPs, CSS/JS, accessibility, responsiveness, SRS, ERD, test checklist, demo script |

Everyone reviews the other two's pull requests. Merge only when `mvn test` is green.

## Weekly discipline

1. **Monday:** pick the week's tasks from this plan and open one branch per task.
2. **Daily:** commit with a message that names the requirement (`FR-5.3: refuse illegal task transitions`).
3. **Thursday:** run `mvn test` plus the two HTTP suites; fix everything before the demo.
4. **Friday:** 15-minute demo to the guide using the checklist — on the *deployed* application, never
   on slides.
5. **Sunday:** update the report chapter for that week with the evidence produced.

## 10-minute viva demo script

| Min | Action | What the examiner sees |
|---|---|---|
| 0–1 | Open the landing page, sign in as **admin** | Public page → role-aware dashboard with live counters |
| 1–3 | Admin: create a user, deactivate it, try to deactivate the last admin | Guard rails, flash messages, activity log filling up |
| 3–5 | Switch to **manager**: create a project, add a member and a reviewer, create a task with a due date | Full project lifecycle, notifications appearing for other users |
| 5–7 | Switch to **member**: start the task, upload two evidence files, attempt a `.exe` (refused), then upload again while pending (refused) | The security controls, with the reason shown on screen |
| 7–9 | Switch to **reviewer**: reject with feedback, then approve the resubmitted version 2 | Mandatory feedback, version history both preserved |
| 9–10 | Manager: complete the task. Admin: open the report, apply a date filter, export CSV | Reports from real data, export opens in Excel |
| +2 min | Terminal: `mvn test`, `bash docs/tools/smoke.sh`, `bash docs/tools/flow.sh` | 80 + 176 checks green; then `mysql` audit queries confirming the same facts |

**Questions to be ready for.**
*Why not store files in the database or in `webapps/`?* · *How do you stop a member from approving
their own work?* (server-side `Permissions` + a unit-tested rule) · *What happens if two reviewers
click at once?* (only PENDING can be decided, and the decision runs in a transaction) ·
*Why `VARCHAR` instead of `ENUM` for status?* (workflow lives in one Java enum, unit-tested) ·
*How do you know a task is overdue?* (computed from `due_date`, never stored) ·
*Where is the SQL?* (only in `dao/`, never in a JSP).
