# WorkProof — Test Checklist

Two ways to verify the system:

* **Automated (already executed).** `mvn test` runs 80 unit tests; two scripted HTTP suites
  (`docs/tools/smoke.sh`, `docs/tools/flow.sh`) drove a deployed WAR over real HTTP against MySQL —
  **82 page/security checks + 94 workflow checks, 0 failures**.
* **Manual (this document).** The same ground, written as a checklist you can tick off in front of
  an examiner. Each row lists the steps, the expected result, and the technical reason behind it —
  which is exactly what gets asked in a viva.

> Legend: ✅ = passes in the automated run · **DB** = inspect with SQL in the "Audit" section.

---

## 1. Authentication and session security

| # | Steps | Expected result | Why it matters |
|---|---|---|---|
| 1.1 ✅ | Open `http://localhost:8080/WorkProof/` while signed out | Public landing page with a **Sign in** button; no application data | The landing page must not leak anything |
| 1.2 ✅ | Request `/dashboard` while signed out | Redirect to `/login` with "Please sign in to continue." | Protected pages are never rendered for anonymous visitors |
| 1.3 ✅ | Sign in as `admin@workproof.local` / `Admin@123` | Redirect to the admin dashboard, name in the top bar | Happy path |
| 1.4 ✅ | Sign in with the correct email and a wrong password | Same generic message: *"Incorrect email or password."* and no session | The message must not reveal whether the account exists |
| 1.5 | View the page source of the login form | `<input type="hidden" name="_csrf" value="…">` with a 43-character token | CSRF token is issued per session |
| 1.6 ✅ | Submit the login form after deleting the `_csrf` field (edit HTML and submit, or use `curl`) | HTTP **403** "Security check failed" | Every POST is CSRF-protected |
| 1.7 ✅ | Note the CSRF token on the login page, sign in, then replay that old token in a POST | HTTP **403** | The session — and therefore the token — is rotated after login (session-fixation defence) |
| 1.8 ✅ | Sign out, then press the browser Back button and reload a protected page | Redirect to `/login` | The session really is invalidated |
| 1.9 | Type `/logout` in the address bar (GET) | **Never** a logout; you are redirected to the login page | State change requires POST |
| 1.10 | Check the response headers with the browser developer tools | `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options`, `Referrer-Policy`; `JSESSIONID` marked `HttpOnly` | Defence in depth |

## 2. Authorisation (server-side RBAC)

| # | Steps | Expected result | Why it matters |
|---|---|---|---|
| 2.1 ✅ | Signed in as `aarav.member@workproof.local`, open `/admin/users` | HTTP **403** access-denied page (not the user list) | Hiding a menu item is not security; the server refuses |
| 2.2 ✅ | As the member, open `/reviews` and `/reviews/view?id=<any>` | HTTP **403** | Only reviewers/admins decide on evidence |
| 2.3 ✅ | As `priya.manager…` (a manager), open `/admin/activity` | HTTP **403** | Managers are not administrators |
| 2.4 ✅ | As a member, open `/projects/new` | HTTP **403** | Only managers/admins create projects |
| 2.5 | As a member, open a task in a project you do **not** belong to (copy a URL from another account) | HTTP **403** or **404** | Row-level scoping, not just role checks |
| 2.6 | As a reviewer who is not in the project, try `/submissions/download?id=<fileId>` | HTTP **403** | Authorisation is re-checked on every file download |
| 2.7 ✅ | As an administrator, open the activity log | Full system-wide log | The admin role is not blocked by the same rules |

## 3. Dashboards and reports

| # | Steps | Expected result |
|---|---|---|
| 3.1 ✅ | Sign in as each of the four roles and open the dashboard | Each shows different, role-appropriate data — all from the database, nothing hard-coded |
| 3.2 ✅ | Compare the dashboard counters with the SQL in the Audit section | They match exactly |
| 3.3 ✅ | Run `/reports` and pick each of the five reports | Project progress, task completion, team contribution, pending verification, rejected submissions |
| 3.4 ✅ | Apply a date range that excludes everything (e.g. `from=2000-01-01&to=2000-01-31`) | Empty state is shown, no error |
| 3.5 ✅ | Click **Export CSV** on any report and open the file in Excel/LibreOffice | File downloads with the right name, columns line up, accented characters correct |
| 3.6 | In a task title, type `=1+1` and export the report containing it | The cell shows `'=1+1` — **not** the result `2` | Formula-injection guard |
| 3.7 ✅ | As a member, open `/reports` | Only your own contribution; a manager sees only their projects |

## 4. Projects and membership

| # | Steps | Expected result |
|---|---|---|
| 4.1 ✅ | As the manager, create a project (name, code `VIVA-2026`, description ≥ 10 characters, dates, status) | Redirect to the project page, flash "Project created"; the creator is automatically its MANAGER |
| 4.2 ✅ | Create a second project with the **same code** | Form is redisplayed with *"…is already in use"*; only one row exists in `projects` |
| 4.3 ✅ | Set the end date **before** the start date | Refused with a clear message |
| 4.4 ✅ | Add `aarav` as MEMBER and `mentor` as REVIEWER | Both appear with their in-project roles |
| 4.5 | Add the same person twice | The unique key `(project_id, user_id)` prevents a duplicate; a friendly message appears |
| 4.6 | Archive the project, then restore it | Status becomes ARCHIVED and back; archived projects are visually marked |
| 4.7 | Remove a member, then open their "My tasks" list | They no longer see the project; their historical submissions stay in the database |

## 5. Tasks and the status workflow

| # | Steps | Expected result |
|---|---|---|
| 5.1 ✅ | As the manager, create a task in the project, priority HIGH, due date, assignee `aarav` | Task page opens; the assignee receives a "New task assigned" notification; status is **TODO** |
| 5.2 | Create a task with no assignee | Accepted (a manager may plan unassigned work) |
| 5.3 ✅ | As the member, move the task TODO → IN_PROGRESS | Status changes, activity log records it |
| 5.4 ✅ | As the member, try to jump IN_PROGRESS → COMPLETED | Refused, status unchanged, explanation shown |
| 5.5 | As the member, try to move a task assigned to someone else | Refused |
| 5.6 ✅ | As the manager, move the task APPROVED → COMPLETED (before it is approved) | Refused: only an approved task can be completed |
| 5.7 | Filter the task list by status/priority/project, sort by due date, page through | Filters combine, sorting is stable, pagination works |
| 5.8 | Search for `' OR 1=1 --` in the task search box | Empty result, no error, no data leak |

## 6. Proof-of-work submission (upload)

| # | Steps | Expected result |
|---|---|---|
| 6.1 ✅ | As `aarav`, open the assigned task → **Submit evidence**, add a title, a description ≥ 15 characters and 2 files (a `.txt` and a `.pdf`) | Submission page opens; task becomes **SUBMITTED** |
| 6.2 ✅ | Check the upload directory on the server | Two files inside `…/<yyyy>/<MM>/`, each renamed to a UUID; the original names appear only in the database |
| 6.3 ✅ | Confirm the upload directory is **outside** `webapps/` | Requesting a file directly by URL is impossible |
| 6.4 ✅ | Upload a file renamed `payload.exe` | Rejected with a message naming the allowed types; nothing is written to disk |
| 6.5 | Rename `payload.exe` to `payload.png` and upload it | Rejected — the magic-byte check catches the mismatch between name and content |
| 6.6 | Upload a file larger than 10 MB | Rejected with the size limit in the message |
| 6.7 | Submit with 6 files | Rejected: at most 5 files per submission |
| 6.8 ✅ | Submit with **no** file and a 20-character description | Rejected: "…at least 40 characters…" |
| 6.9 ✅ | Submit again while the first submission is still waiting for review | Refused with *"This task already has evidence waiting for review."* — the reviewer must not chase a moving target |
| 6.10 ✅ | Download your own file; then ask the reviewer to download it; then try the same URL signed out | 200, 200, and a redirect to the login page |
| 6.11 | Open a submission and try to change its URL id to another project's submission | HTTP 403/404 |

## 7. Verification / review

| # | Steps | Expected result |
|---|---|---|
| 7.1 ✅ | Sign in as `mentor.reviewer…`, open the review queue | The submission is listed with task, submitter, file count and waiting time |
| 7.2 ✅ | Open it, then click **Approve** with a comment | Submission APPROVED, task APPROVED, submitter notified, activity log entry written |
| 7.3 ✅ | Try to approve/reject the same submission again | Refused: only PENDING submissions can be decided; the decision cannot be rewritten |
| 7.4 ✅ | As the member who submitted, try to open `/reviews/view?id=<own submission>` | HTTP **403** — no self-approval |
| 7.5 ✅ | Create a second submission, then reject it with `bad` (3 characters) | Refused: feedback must be at least 10 characters |
| 7.6 ✅ | Reject it properly with real feedback | Submission REJECTED, task REJECTED, the submitter gets a notification containing the feedback |
| 7.7 ✅ | As the member, upload a corrected version (version 2) | New submission row with `version = 2`; task back to SUBMITTED; version 1 and its review are still stored |
| 7.8 ✅ | Open the task's evidence history | Both versions with their own status and the reviewer's comments are visible |

## 8. Notifications and activity log

| # | Steps | Expected result |
|---|---|---|
| 8.1 ✅ | Trigger an assignment, a submission and a review decision | The bell shows an unread count for the right users only |
| 8.2 ✅ | Click a notification with a link | It opens the related page and marks itself read |
| 8.3 ✅ | Click **Mark all as read** | Unread count returns to 0 |
| 8.4 | Delete a notification | It disappears for you and is **not** visible to anyone else |
| 8.5 ✅ | Open `/admin/activity` as the administrator | Every action above is listed with actor, action, entity, IP address and time |
| 8.6 | Filter the activity log by user | Only that user's actions are listed |
| 8.7 | Wait for (or trigger) the deadline reminder job, then look again | A `DEADLINE` notification appears once per task per day — running the job again does not duplicate it |

## 9. Error handling and robustness

| # | Steps | Expected result |
|---|---|---|
| 9.1 ✅ | Open `/projects/view?id=999999` | Friendly **404** page, no stack trace |
| 9.2 ✅ | Open `/projects/view?id=abc` and `/tasks/view?id=` | Friendly **400** "That request could not be understood" page |
| 9.3 ✅ | Open `/reports?report=does-not-exist` and `/reports?report=project-progress&from=not-a-date` | The page still renders; the bad filter is simply ignored |
| 9.4 ✅ | Open `/tasks?page=abc` and `/tasks?page=99999` | Page 1 and an empty result respectively — no exception |
| 9.5 ✅ | Open `/notifications` with a nonsense `action` in a POST | Friendly "Unknown notification action" message |
| 9.6 | Stop MySQL and reload a page | Friendly 500 page; the Tomcat log has the stack trace, the browser never sees it. (Remember to start MySQL again.) |
| 9.7 | Search `/nonexistent-page` | 404 page styled like the rest of the application |

## 10. User interface, accessibility and offline behaviour

| # | Steps | Expected result |
|---|---|---|
| 10.1 | Resize the browser to ~375 px wide | Sidebar collapses, tables scroll horizontally, no overlapping text |
| 10.2 | Disconnect the internet and reload every page | Everything still works: Bootstrap, icons, CSS and JS are served by the application itself |
| 10.3 | Tab through the login form | Visible focus, labels announced, the skip link works |
| 10.4 | Open a page with an empty list (a new project with no tasks) | A useful empty state with a call to action, not a blank table |
| 10.5 | Look at any status badge | Colour **and** text are both present (readable in greyscale) |
| 10.6 | Print a report page (Ctrl+P) | Print styles hide navigation and keep the table readable |

---

## 11. Audit SQL

Run these in MySQL to confirm what the UI claims. They are the same assertions the automated suite
makes by hand.

```sql
USE workproof;

-- 1. Nobody stores a plaintext password
SELECT id, email, LEFT(password_hash, 7) AS hash_prefix FROM users;

-- 2. Passwords are unique per user (BCrypt salts), even for identical passwords
SELECT COUNT(*) AS hashes, COUNT(DISTINCT password_hash) AS distinct_hashes
FROM users WHERE password_hash LIKE '$2a$%';

-- 3. Task counts per status (compare with the dashboard)
SELECT status, COUNT(*) FROM tasks GROUP BY status ORDER BY status;

-- 4. Every project has exactly one manager
SELECT p.id, p.code, COUNT(pm.id) AS managers
FROM projects p JOIN project_members pm ON pm.project_id = p.id AND pm.role_in_project = 'MANAGER'
GROUP BY p.id, p.code;

-- 5. No submission can be approved without a review
SELECT s.id FROM proof_submissions s
LEFT JOIN reviews r ON r.submission_id = s.id
WHERE s.status = 'APPROVED' AND r.id IS NULL;   -- must return no rows

-- 6. Nobody reviewed their own submission
SELECT r.id FROM reviews r
JOIN proof_submissions s ON s.id = r.submission_id
WHERE r.reviewer_id = s.submitted_by;           -- must return no rows

-- 7. Every rejection carries usable feedback
SELECT id FROM reviews WHERE decision = 'REJECTED' AND CHAR_LENGTH(comments) < 10;  -- no rows

-- 8. Submission versions are unique per task
SELECT task_id, version, COUNT(*) FROM proof_submissions
GROUP BY task_id, version HAVING COUNT(*) > 1;  -- no rows

-- 9. Only one submission per task may be waiting for review
SELECT task_id, COUNT(*) FROM proof_submissions WHERE status = 'PENDING'
GROUP BY task_id HAVING COUNT(*) > 1;           -- no rows

-- 10. Files exist in the database with a hash, and the bytes are outside the web root
SELECT s.id, COUNT(f.id) AS files, MIN(f.sha256) IS NOT NULL AS hashed
FROM proof_submissions s JOIN submission_files f ON f.submission_id = s.id
GROUP BY s.id;

-- 11. The activity log recorded the review decisions
SELECT action, entity_type, entity_id, ip_address, created_at
FROM activity_logs ORDER BY id DESC LIMIT 20;
```

## 12. How to re-run the automated suites

```bash
# Unit tests (no database needed)
mvn test                      # 80 tests, all green in ~12 s

# HTTP suites - build, deploy, then point the scripts at your instance
mvn clean package
cp target/WorkProof.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh

bash docs/tools/smoke.sh      # pages, roles, CSRF, headers, sessions
bash docs/tools/flow.sh       # the full write workflow, edge cases
```

`flow.sh` creates and then cleans up its own demo project (`VIVA-2026`) and test accounts, so it can
be re-run safely. `smoke.sh` is read-only apart from the accounts it logs into.
