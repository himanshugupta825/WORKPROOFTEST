-- ============================================================================
--  WorkProof - Work Management and Proof-of-Work Verification System
--  File   : database/schema.sql
--  Target : MySQL 8.x   (also tested on MariaDB 11.x)
--  Engine : InnoDB, charset utf8mb4
--
--  HOW TO RUN
--    mysql -u root -p < database/schema.sql
--  or inside the MySQL shell:
--    source database/schema.sql;
--
--  The script is idempotent: it drops and recreates the `workproof` database,
--  so re-running it always gives a clean, known state for your demo/viva.
-- ============================================================================

DROP DATABASE IF EXISTS workproof;
CREATE DATABASE workproof
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;
USE workproof;

-- ============================================================================
-- 1. roles  - master list of application roles (RBAC)
-- ============================================================================
CREATE TABLE roles (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(30)  NOT NULL,
    description   VARCHAR(255) NOT NULL,
    CONSTRAINT uq_roles_name UNIQUE (name)
) ENGINE = InnoDB;

-- ============================================================================
-- 2. users  - accounts. Passwords are stored as BCrypt hashes only.
-- ============================================================================
CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,           -- BCrypt hash (60 chars)
    role_id       INT          NOT NULL,
    status        VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE',
    department    VARCHAR(100) NULL,
    designation   VARCHAR(100) NULL,
    phone         VARCHAR(20)  NULL,
    last_login_at DATETIME     NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_users_role (role_id),
    INDEX idx_users_status (status)
) ENGINE = InnoDB;

-- ============================================================================
-- 3. projects
-- ============================================================================
CREATE TABLE projects (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    code          VARCHAR(20)  NOT NULL,           -- short human code, e.g. PBL-2026
    description   TEXT         NOT NULL,
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    status        VARCHAR(15)  NOT NULL DEFAULT 'PLANNED',
    created_by    INT          NOT NULL,           -- project owner
    archived_at   DATETIME     NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_projects_code UNIQUE (code),
    CONSTRAINT ck_projects_status
        CHECK (status IN ('PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'ARCHIVED')),
    CONSTRAINT ck_projects_dates CHECK (end_date >= start_date),
    CONSTRAINT fk_projects_creator FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_projects_status (status),
    INDEX idx_projects_dates (start_date, end_date)
) ENGINE = InnoDB;

-- ============================================================================
-- 4. project_members  - who works on a project and in which project role
--    role_in_project : MANAGER | MEMBER | REVIEWER
-- ============================================================================
CREATE TABLE project_members (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    project_id      INT         NOT NULL,
    user_id         INT         NOT NULL,
    role_in_project VARCHAR(15) NOT NULL DEFAULT 'MEMBER',
    joined_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_member UNIQUE (project_id, user_id),
    CONSTRAINT ck_project_member_role
        CHECK (role_in_project IN ('MANAGER', 'MEMBER', 'REVIEWER')),
    CONSTRAINT fk_pm_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_pm_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_pm_user (user_id)
) ENGINE = InnoDB;

-- ============================================================================
-- 5. tasks
--    status flow: TODO -> IN_PROGRESS -> SUBMITTED -> IN_REVIEW
--                 -> APPROVED / REJECTED -> COMPLETED
-- ============================================================================
CREATE TABLE tasks (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    project_id   INT          NOT NULL,
    title        VARCHAR(180) NOT NULL,
    description  TEXT         NOT NULL,
    priority     VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',
    status       VARCHAR(15)  NOT NULL DEFAULT 'TODO',
    start_date   DATE         NULL,
    due_date     DATE         NOT NULL,
    created_by   INT          NOT NULL,
    completed_at DATETIME     NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_tasks_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_tasks_status CHECK (status IN
        ('TODO', 'IN_PROGRESS', 'SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'COMPLETED')),
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_tasks_creator FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_tasks_project_status (project_id, status),
    INDEX idx_tasks_due (due_date),
    INDEX idx_tasks_priority (priority)
) ENGINE = InnoDB;

-- ============================================================================
-- 6. task_assignments  - many-to-many between tasks and users
-- ============================================================================
CREATE TABLE task_assignments (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    task_id     INT      NOT NULL,
    user_id     INT      NOT NULL,
    assigned_by INT      NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_task_assignee UNIQUE (task_id, user_id),
    CONSTRAINT fk_ta_task FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ta_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ta_assigner FOREIGN KEY (assigned_by) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_ta_user (user_id)
) ENGINE = InnoDB;

-- ============================================================================
-- 7. proof_submissions  - one row per submission attempt (history kept)
--    status: PENDING | APPROVED | REJECTED | SUPERSEDED
-- ============================================================================
CREATE TABLE proof_submissions (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    task_id      INT          NOT NULL,
    submitted_by INT          NOT NULL,
    title        VARCHAR(180) NOT NULL,
    description  TEXT         NOT NULL,
    status       VARCHAR(12)  NOT NULL DEFAULT 'PENDING',
    version      INT          NOT NULL DEFAULT 1,
    submitted_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_sub_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'SUPERSEDED')),
    CONSTRAINT uq_submission_version UNIQUE (task_id, version),
    CONSTRAINT fk_sub_task FOREIGN KEY (task_id) REFERENCES tasks (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_sub_user FOREIGN KEY (submitted_by) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_sub_status (status),
    INDEX idx_sub_user (submitted_by),
    INDEX idx_sub_submitted_at (submitted_at)
) ENGINE = InnoDB;

-- ============================================================================
-- 8. submission_files  - metadata of uploaded evidence (bytes live on disk)
-- ============================================================================
CREATE TABLE submission_files (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    submission_id INT          NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(255) NOT NULL,   -- generated safe name on disk
    content_type  VARCHAR(120) NOT NULL,
    file_size     BIGINT       NOT NULL,
    sha256        CHAR(64)     NOT NULL,   -- integrity checksum
    uploaded_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sf_submission FOREIGN KEY (submission_id) REFERENCES proof_submissions (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_sf_submission (submission_id)
) ENGINE = InnoDB;

-- ============================================================================
-- 9. reviews  - reviewer decision on a submission (audit trail)
-- ============================================================================
CREATE TABLE reviews (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    submission_id INT          NOT NULL,
    reviewer_id   INT          NOT NULL,
    decision      VARCHAR(10)  NOT NULL,
    comments      TEXT         NOT NULL,
    reviewed_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_review_submission UNIQUE (submission_id),   -- one decision per submission
    CONSTRAINT ck_review_decision CHECK (decision IN ('APPROVED', 'REJECTED')),
    CONSTRAINT fk_rev_submission FOREIGN KEY (submission_id) REFERENCES proof_submissions (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_rev_reviewer FOREIGN KEY (reviewer_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_rev_reviewer (reviewer_id)
) ENGINE = InnoDB;

-- ============================================================================
-- 10. notifications  - in-app notifications.
--     dedupe_key keeps the scheduled deadline job from creating duplicates.
-- ============================================================================
CREATE TABLE notifications (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NOT NULL,
    title      VARCHAR(150) NOT NULL,
    message    VARCHAR(500) NOT NULL,
    type       VARCHAR(25)  NOT NULL,
    link       VARCHAR(255) NULL,
    dedupe_key VARCHAR(120) NULL,
    is_read    TINYINT      NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at    DATETIME     NULL,
    CONSTRAINT uq_notification_dedupe UNIQUE (user_id, dedupe_key),
    CONSTRAINT ck_notification_type CHECK (type IN
        ('TASK_ASSIGNED', 'TASK_STATUS', 'SUBMISSION', 'REVIEW', 'DEADLINE', 'PROJECT', 'SYSTEM')),
    CONSTRAINT fk_not_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_not_user_unread (user_id, is_read),
    INDEX idx_not_created (created_at)
) ENGINE = InnoDB;

-- ============================================================================
-- 11. activity_logs  - immutable audit of important actions
-- ============================================================================
CREATE TABLE activity_logs (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT          NULL,          -- NULL for system-generated events
    action      VARCHAR(60)  NOT NULL,
    entity_type VARCHAR(30)  NULL,
    entity_id   INT          NULL,
    details     VARCHAR(500) NULL,
    ip_address  VARCHAR(45)  NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    INDEX idx_log_created (created_at),
    INDEX idx_log_user (user_id),
    INDEX idx_log_entity (entity_type, entity_id)
) ENGINE = InnoDB;

-- ============================================================================
-- Reporting view: task progress per project (used by the reports module)
-- ============================================================================
CREATE OR REPLACE VIEW v_project_progress AS
SELECT
    p.id                                             AS project_id,
    p.name                                           AS project_name,
    p.code                                           AS project_code,
    p.status                                         AS project_status,
    p.start_date,
    p.end_date,
    COUNT(t.id)                                      AS total_tasks,
    SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed_tasks,
    SUM(CASE WHEN t.status = 'APPROVED'  THEN 1 ELSE 0 END) AS approved_tasks,
    SUM(CASE WHEN t.status IN ('SUBMITTED', 'IN_REVIEW') THEN 1 ELSE 0 END) AS awaiting_verification_tasks,
    SUM(CASE WHEN t.status = 'REJECTED'  THEN 1 ELSE 0 END) AS rejected_tasks,
    SUM(CASE WHEN t.status NOT IN ('COMPLETED') AND t.due_date < CURDATE() THEN 1 ELSE 0 END) AS overdue_tasks,
    ROUND(
        100 * SUM(CASE WHEN t.status IN ('APPROVED', 'COMPLETED') THEN 1 ELSE 0 END)
        / NULLIF(COUNT(t.id), 0), 1
    )                                                AS progress_percent
FROM projects p
LEFT JOIN tasks t ON t.project_id = p.id
GROUP BY p.id, p.name, p.code, p.status, p.start_date, p.end_date;

-- ============================================================================
-- Optional: dedicated application user (recommended instead of root).
-- Run this manually as root if you want a least-privilege DB account:
--
--   CREATE USER 'workproof_app'@'localhost' IDENTIFIED BY 'ChangeThisStrongPassword!';
--   GRANT SELECT, INSERT, UPDATE, DELETE ON workproof.* TO 'workproof_app'@'localhost';
--   FLUSH PRIVILEGES;
-- ============================================================================
