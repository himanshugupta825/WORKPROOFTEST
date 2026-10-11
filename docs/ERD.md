# WorkProof — Database Design (ERD)

Database name: **`workproof`** · Engine: **InnoDB** · Charset: **utf8mb4 / utf8mb4_general_ci**
DDL: [`database/schema.sql`](../database/schema.sql) · demo data: [`database/seed.sql`](../database/seed.sql)

---

## 1. Entity-relationship diagram

```mermaid
erDiagram
    ROLES ||--o{ USERS : "assigns a global role"
    USERS ||--o{ PROJECTS : "creates"

    PROJECTS ||--o{ PROJECT_MEMBERS : "has members"
    USERS    ||--o{ PROJECT_MEMBERS : "belongs to projects"

    PROJECTS ||--o{ TASKS : "contains"
    USERS    ||--o{ TASKS : "creates"

    TASKS ||--o{ TASK_ASSIGNMENTS : "is assigned to"
    USERS ||--o{ TASK_ASSIGNMENTS : "works on"
    USERS ||--o{ TASK_ASSIGNMENTS : "assigns"

    TASKS ||--o{ PROOF_SUBMISSIONS : "receives evidence"
    USERS ||--o{ PROOF_SUBMISSIONS : "submits"

    PROOF_SUBMISSIONS ||--o{ SUBMISSION_FILES : "carries files"
    PROOF_SUBMISSIONS ||--o| REVIEWS : "is decided by at most one review"
    USERS             ||--o{ REVIEWS : "reviews"

    USERS ||--o{ NOTIFICATIONS : "receives"
    USERS ||--o{ ACTIVITY_LOGS : "acts (nullable for system events)"

    ROLES {
        int     id PK
        varchar name UK "ADMIN | MANAGER | MEMBER | REVIEWER"
        varchar description
    }

    USERS {
        int      id PK
        varchar  full_name "120"
        varchar  email UK "150, stored lower-case"
        varchar  password_hash "100, BCrypt $2a$12$... never plaintext"
        int      role_id FK
        varchar  status "ACTIVE | INACTIVE"
        varchar  department "100, nullable"
        varchar  designation "100, nullable"
        varchar  phone "20, nullable"
        datetime last_login_at "nullable"
        datetime created_at
        datetime updated_at
    }

    PROJECTS {
        int      id PK
        varchar  name "150"
        varchar  code UK "20, upper-case, e.g. CMP-2026"
        text     description
        date     start_date
        date     end_date
        varchar  status "PLANNED|ACTIVE|ON_HOLD|COMPLETED|ARCHIVED"
        int      created_by FK "users.id"
        datetime archived_at "nullable"
        datetime created_at
        datetime updated_at
    }

    PROJECT_MEMBERS {
        int      id PK
        int      project_id FK
        int      user_id FK
        varchar  role_in_project "MANAGER | MEMBER | REVIEWER"
        datetime joined_at
    }

    TASKS {
        int      id PK
        int      project_id FK
        varchar  title "180"
        text     description
        varchar  priority "LOW | MEDIUM | HIGH | CRITICAL"
        varchar  status "TODO|IN_PROGRESS|SUBMITTED|IN_REVIEW|APPROVED|REJECTED|COMPLETED"
        date     start_date "nullable"
        date     due_date "nullable"
        int      created_by FK "users.id"
        datetime completed_at "nullable, set only on COMPLETED"
        datetime created_at
        datetime updated_at
    }

    TASK_ASSIGNMENTS {
        int      id PK
        int      task_id FK
        int      user_id FK
        int      assigned_by FK "users.id"
        datetime assigned_at
    }

    PROOF_SUBMISSIONS {
        int      id PK
        int      task_id FK
        int      submitted_by FK "users.id"
        varchar  title "180"
        text     description "the written proof of work"
        varchar  status "PENDING | APPROVED | REJECTED | SUPERSEDED"
        int      version "unique per (task_id, version)"
        datetime submitted_at
        datetime updated_at
    }

    SUBMISSION_FILES {
        int      id PK
        int      submission_id FK
        varchar  original_name "255, as the browser sent it (sanitised)"
        varchar  stored_name "255, generated uuid path, e.g. 2026/10/<uuid>.pdf"
        varchar  content_type "120"
        bigint   file_size
        char     sha256 "64, integrity hash of the stored bytes"
        datetime uploaded_at
    }

    REVIEWS {
        int      id PK
        int      submission_id FK "one live decision per submission"
        int      reviewer_id FK "users.id, must differ from the submitter"
        varchar  decision "APPROVED | REJECTED"
        text     comments "mandatory (>= 10 characters) when REJECTED"
        datetime reviewed_at
    }

    NOTIFICATIONS {
        int      id PK
        int      user_id FK
        varchar  title "150"
        varchar  message "500"
        varchar  type "TASK_ASSIGNED|TASK_STATUS|SUBMISSION|REVIEW|DEADLINE|PROJECT|SYSTEM"
        varchar  link "255, in-app target such as /submissions/view?id=12"
        varchar  dedupe_key "120, unique per (user_id, dedupe_key) - stops duplicate reminders"
        tinyint  is_read
        datetime created_at
        datetime read_at "nullable"
    }

    ACTIVITY_LOGS {
        int      id PK
        int      user_id FK "nullable: system events have no actor"
        varchar  action "60, e.g. PROJECT_CREATED, SUBMISSION_APPROVED"
        varchar  entity_type "30, PROJECT | TASK | SUBMISSION | USER | REVIEW"
        int      entity_id "nullable"
        varchar  details "500, truncated, never contains secrets"
        varchar  ip_address "45, fits IPv6"
        datetime created_at
    }
```

## 2. Views

| View | Purpose |
|---|---|
| `v_project_progress` | Per-project roll-up used by the dashboard and the project list: total tasks, completed, approved, awaiting verification, rejected, overdue, and `progress_percent`. Keeping the aggregate here means the JSPs never contain arithmetic or SQL. |

## 3. Keys, constraints and delete rules

| Table | Unique keys | Foreign keys (`ON DELETE` / `ON UPDATE`) |
|---|---|---|
| `roles` | `name` | — |
| `users` | `email` | `role_id → roles.id` **RESTRICT / CASCADE** |
| `projects` | `code` | `created_by → users.id` **RESTRICT / CASCADE** |
| `project_members` | `(project_id, user_id)` | `project_id → projects.id` **CASCADE**; `user_id → users.id` **CASCADE** |
| `tasks` | — (indexed on `(project_id, status)`, `due_date`) | `project_id → projects.id` **CASCADE**; `created_by → users.id` **RESTRICT** |
| `task_assignments` | `(task_id, user_id)` | `task_id → tasks.id` **CASCADE**; `user_id → users.id` **CASCADE**; `assigned_by → users.id` **RESTRICT** |
| `proof_submissions` | `(task_id, version)` | `task_id → tasks.id` **CASCADE**; `submitted_by → users.id` **RESTRICT** |
| `submission_files` | — | `submission_id → proof_submissions.id` **CASCADE** |
| `reviews` | `submission_id` | `submission_id → proof_submissions.id` **CASCADE**; `reviewer_id → users.id` **RESTRICT** |
| `notifications` | `(user_id, dedupe_key)` | `user_id → users.id` **CASCADE** |
| `activity_logs` | — | `user_id → users.id` **SET NULL** (the audit trail survives a deleted account) |

**Why these rules**

* Deleting a **project** takes its membership rows, tasks, submissions and files with it — which is
  why the application offers *archiving* instead of deletion for real work.
* Deleting a **user** is not offered in the UI at all (accounts are deactivated); `RESTRICT` on
  the "author" columns means the database refuses to orphan authored content even if somebody
  writes a raw `DELETE`.
* The **unique key on `(user_id, dedupe_key)`** is what makes the deadline reminder job idempotent:
  the same reminder cannot be inserted twice, and `INSERT IGNORE` turns a duplicate into a no-op.

## 4. Design decisions worth defending in the viva

1. **Normalised to 3NF.** Every table has a single purpose; no repeating groups, no comma-joined
   `member_ids` columns (that is why `project_members` and `task_assignments` exist as separate
   tables with their own keys).
2. **Status columns are `VARCHAR`, not `ENUM`.** The allowed values live in Java enums
   (`TaskStatus`, `ProjectStatus`, …) and in the validation layer. Changing a workflow step becomes
   a one-line Java change instead of an `ALTER TABLE` that rewrites the whole table.
   `TaskStatus.allowedNext()` is unit-tested, so the rule lives in exactly one place.
3. **`role_id` is a foreign key, not a role name string**, so a role can be renamed without touching
   `users`.
4. **Two levels of authorisation in the data model**: the global `users.role_id`
   (ADMIN/MANAGER/MEMBER/REVIEWER) and the per-project `project_members.role_in_project`
   (MANAGER/MEMBER/REVIEWER). A user can manage one project and merely belong to another; the
   queries scope rows using the second table, never the first.
5. **Evidence is versioned, never overwritten.** `(task_id, version)` is unique and older versions
   stay in the table, so "show me the history of this task" is a plain `ORDER BY version` and the
   reviewer's decision on v1 is never silently rewritten.
6. **Files are described, not stored, in the database.** `submission_files` holds metadata only; the
   bytes live in the configured upload directory outside the web root. `stored_name` is generated
   by the server (never the browser's filename) and `sha256` proves the bytes did not change.
7. **Indexes match the real queries**: `tasks(project_id, status)` for the project task list,
   `tasks(due_date)` for the deadline job and upcoming-deadline panels,
   `proof_submissions(status)` and `proof_submissions(submitted_at)` for the review queue and the
   turnaround report, `notifications(user_id, is_read)` for the bell badge,
   `activity_logs(entity_type, entity_id)` for a record's history and `activity_logs(created_at)`
   for the log page — which default-sorts newest first.

## 5. Reproducing the database from scratch

```bash
mysql -u root -p < database/schema.sql              # idempotent: drops and recreates the tables
mysql -u root -p workproof < database/seed.sql      # demo scenario (destroys existing rows)
```

`seed.sql` inserts the same scenario used in the viva: 7 accounts (BCrypt cost 12), 2 projects,
9 tasks covering every status, 5 submissions in every state, 4 reviews, 10 notifications and 20
activity-log rows. Its header comment lists the logins and passwords.
